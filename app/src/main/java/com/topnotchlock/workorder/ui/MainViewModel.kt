package com.topnotchlock.workorder.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.topnotchlock.workorder.data.Company
import com.topnotchlock.workorder.data.CompanyRepository
import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.data.VendorRepository
import com.topnotchlock.workorder.data.VendorTemplate
import com.topnotchlock.workorder.data.WoNumberGenerator
import com.topnotchlock.workorder.data.WorkOrder
import com.topnotchlock.workorder.data.db.AppDatabase
import com.topnotchlock.workorder.data.db.WorkOrderEntity
import com.topnotchlock.workorder.ocr.TextExtractor
import com.topnotchlock.workorder.parsing.VendorParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "TnlViewModel"

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val vendorRepo = VendorRepository(application)
    private val companyRepo = CompanyRepository(application)
    private val db = AppDatabase.getInstance(application)
    private val woGen = WoNumberGenerator(application)

    var vendors by mutableStateOf(vendorRepo.listVendors())
        private set

    var companies by mutableStateOf(companyRepo.listCompanies())
        private set

    var defaultCompanyId by mutableStateOf(companyRepo.defaultCompanyId())
        private set

    var rawExtractedText by mutableStateOf("")
        private set

    var draft by mutableStateOf(WorkOrder())
        private set

    var isBusy by mutableStateOf(false)
        private set

    var matchedFields by mutableStateOf<Set<FieldKey>>(emptySet())
        private set

    var lastError by mutableStateOf<String?>(null)
        private set

    var lastGeneratedPdf by mutableStateOf<File?>(null)
        private set

    val history: StateFlow<List<WorkOrderEntity>> =
        db.workOrderDao().observeAll().stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val woNumberGenerator: WoNumberGenerator get() = woGen

    fun refreshVendors() {
        vendors = vendorRepo.listVendors()
    }

    fun saveVendor(template: VendorTemplate) {
        vendorRepo.saveVendor(template)
        refreshVendors()
    }

    fun deleteVendor(id: String) {
        vendorRepo.deleteVendor(id)
        refreshVendors()
    }

    fun newVendorId(name: String): String = vendorRepo.slugify(name)

    // --- Companies (multi-business support: header name/phone per work order) ---

    fun refreshCompanies() {
        companies = companyRepo.listCompanies()
        defaultCompanyId = companyRepo.defaultCompanyId()
    }

    fun saveCompany(company: Company) {
        companyRepo.addOrUpdate(company)
        refreshCompanies()
    }

    fun deleteCompany(id: String) {
        companyRepo.delete(id)
        refreshCompanies()
    }

    fun setDefaultCompany(id: String) {
        companyRepo.setDefaultCompanyId(id)
        refreshCompanies()
    }

    fun newCompanyId(name: String): String = companyRepo.newId(name)

    /** Changes which company's name/phone prints on the header of the current draft. */
    fun updateDraftCompany(company: Company) {
        draft = draft.copy(companyName = company.name, companyPhone = company.phone)
    }

    /** Starts a fresh work order for [vendor], reserving the next WO# immediately. */
    fun startNewDraft(vendor: VendorTemplate) {
        val today = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date())
        val company = companyRepo.defaultCompany()
        draft = WorkOrder(
            woNumber = woGen.consumeNext(),
            date = today,
            companyName = company?.name.orEmpty(),
            companyPhone = company?.phone.orEmpty(),
            vendorId = vendor.id,
            vendorName = vendor.name
        )
        rawExtractedText = ""
        matchedFields = emptySet()
        lastError = null
        lastGeneratedPdf = null
    }

    fun updateDraftField(field: FieldKey, value: String) {
        draft = draft.with(field, value)
    }

    fun updateWoNumber(value: String) {
        draft = draft.copy(woNumber = value)
    }

    fun updateDate(value: String) {
        draft = draft.copy(date = value)
    }

    fun extractFromPastedText(vendor: VendorTemplate, text: String) {
        applyExtraction(vendor, TextExtractor.fromPastedText(text))
    }

    fun extractFromImageUri(vendor: VendorTemplate, uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            isBusy = true
            lastError = null
            try {
                val text = withContext(Dispatchers.IO) { TextExtractor.fromImageUri(context, uri) }
                applyExtraction(vendor, text)
            } catch (e: Exception) {
                lastError = "Couldn't read text from that image: ${e.message}"
            } finally {
                isBusy = false
            }
        }
    }

    fun extractFromPdfUri(vendor: VendorTemplate, uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            isBusy = true
            lastError = null
            try {
                val text = withContext(Dispatchers.IO) { TextExtractor.fromPdfUri(context, uri) }
                applyExtraction(vendor, text)
            } catch (e: Exception) {
                lastError = "Couldn't read text from that PDF: ${e.message}"
            } finally {
                isBusy = false
            }
        }
    }

    private fun applyExtraction(vendor: VendorTemplate, text: String) {
        rawExtractedText = text
        val results = VendorParser.parse(text, vendor)
        var updated = draft
        results.forEach { result -> updated = updated.with(result.field, result.value) }
        draft = updated
        matchedFields = results.filter { it.matched }.map { it.field }.toSet()
    }

    fun generatePdf(onDone: (File) -> Unit) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            isBusy = true
            lastError = null
            try {
                val file = withContext(Dispatchers.IO) {
                    // filesDir (not cacheDir): survives as long as the app is installed,
                    // so History always has a real PDF to open, instead of the OS
                    // silently trimming it under storage pressure.
                    val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
                    val safeNumber = draft.woNumber.ifBlank { "WO" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val outFile = File(dir, "WorkOrder_${safeNumber}_${System.currentTimeMillis()}.pdf")
                    com.topnotchlock.workorder.pdfgen.WorkOrderPdfGenerator.generate(context, draft, outFile)
                    db.workOrderDao().insert(
                        WorkOrderEntity.fromWorkOrder(draft, System.currentTimeMillis(), outFile.absolutePath)
                    )
                    outFile
                }
                lastGeneratedPdf = file
                onDone(file)
            } catch (e: Exception) {
                Log.e(TAG, "PDF generation failed", e)
                lastError = "Couldn't generate the PDF (${e.javaClass.simpleName}): ${e.message ?: "unknown error"}"
            } finally {
                isBusy = false
            }
        }
    }

    fun shareUriFor(file: File): Uri {
        val context = getApplication<Application>()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /**
     * Copies [file] into the device's public Downloads folder so it shows up in the
     * Files app / Downloads app outside of this app entirely, for people who just
     * want the PDF sitting somewhere they already know how to find.
     * On Android 10+ this goes through MediaStore and needs no permission at all.
     * On Android 8-9 it needs WRITE_EXTERNAL_STORAGE, requested by the caller first.
     */
    fun saveToDownloads(file: File, displayName: String, onResult: (success: Boolean, message: String) -> Unit) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val safeName = if (displayName.endsWith(".pdf")) displayName else "$displayName.pdf"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = context.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        }
                        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: error("MediaStore refused to create the file")
                        resolver.openOutputStream(uri)?.use { out ->
                            FileInputStream(file).use { input -> input.copyTo(out) }
                        } ?: error("Couldn't open the destination file")
                    } else {
                        @Suppress("DEPRECATION")
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        downloadsDir.mkdirs()
                        val outFile = File(downloadsDir, safeName)
                        FileOutputStream(outFile).use { out ->
                            FileInputStream(file).use { input -> input.copyTo(out) }
                        }
                        @Suppress("DEPRECATION")
                        android.media.MediaScannerConnection.scanFile(
                            context, arrayOf(outFile.absolutePath), arrayOf("application/pdf"), null
                        )
                    }
                }
            }
            result.onSuccess {
                onResult(true, "Saved to Downloads.")
            }.onFailure { e ->
                Log.e(TAG, "Save to Downloads failed", e)
                onResult(false, "Couldn't save to Downloads: ${e.message ?: "unknown error"}")
            }
        }
    }

    fun clearError() {
        lastError = null
    }

    // --- Handles content shared into the app from Gmail/Outlook/screenshot apps ---
    var pendingShareText by mutableStateOf<String?>(null)
        private set
    var pendingShareImageUri by mutableStateOf<Uri?>(null)
        private set
    var pendingSharePdfUri by mutableStateOf<Uri?>(null)
        private set

    fun receivePendingShareText(text: String) { pendingShareText = text }
    fun receivePendingShareImage(uri: Uri) { pendingShareImageUri = uri }
    fun receivePendingSharePdf(uri: Uri) { pendingSharePdfUri = uri }

    fun hasPendingShare(): Boolean =
        pendingShareText != null || pendingShareImageUri != null || pendingSharePdfUri != null

    fun clearPendingShare() {
        pendingShareText = null
        pendingShareImageUri = null
        pendingSharePdfUri = null
    }
}
