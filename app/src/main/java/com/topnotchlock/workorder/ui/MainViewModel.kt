package com.topnotchlock.workorder.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val vendorRepo = VendorRepository(application)
    private val db = AppDatabase.getInstance(application)
    private val woGen = WoNumberGenerator(application)

    var vendors by mutableStateOf(vendorRepo.listVendors())
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

    /** Starts a fresh work order for [vendor], reserving the next WO# immediately. */
    fun startNewDraft(vendor: VendorTemplate) {
        val today = SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date())
        draft = WorkOrder(
            woNumber = woGen.consumeNext(),
            date = today,
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
                    val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
                    val safeNumber = draft.woNumber.ifBlank { "WO" }.replace(Regex("[^A-Za-z0-9_-]"), "_")
                    val outFile = File(dir, "WorkOrder_$safeNumber.pdf")
                    com.topnotchlock.workorder.pdfgen.WorkOrderPdfGenerator.generate(context, draft, outFile)
                    db.workOrderDao().insert(
                        WorkOrderEntity.fromWorkOrder(draft, System.currentTimeMillis(), outFile.absolutePath)
                    )
                    outFile
                }
                lastGeneratedPdf = file
                onDone(file)
            } catch (e: Exception) {
                lastError = "Couldn't generate the PDF: ${e.message}"
            } finally {
                isBusy = false
            }
        }
    }

    fun shareUriFor(file: File): Uri {
        val context = getApplication<Application>()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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
