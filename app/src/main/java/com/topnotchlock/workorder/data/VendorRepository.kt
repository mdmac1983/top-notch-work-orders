package com.topnotchlock.workorder.data

import android.content.Context
import java.io.File

/**
 * Vendor templates live as small JSON files under the app's private storage
 * (one JSON file per vendor, in a "vendors" subfolder of filesDir), seeded on
 * first launch from the bundled examples in the assets/vendors folder.
 * Because they're just files on disk (not baked into the compiled app),
 * adding or tweaking a vendor's parsing rules never requires a new APK build
 * - it's all done from the Vendors screen in the app.
 */
class VendorRepository(private val context: Context) {

    private val vendorsDir: File by lazy {
        File(context.filesDir, "vendors").apply { mkdirs() }
    }

    fun ensureSeeded() {
        if (vendorsDir.listFiles()?.isNotEmpty() == true) return
        val assetVendorFiles = runCatching {
            context.assets.list("vendors")?.toList().orEmpty()
        }.getOrDefault(emptyList())

        assetVendorFiles.filter { it.endsWith(".json") }.forEach { fileName ->
            val text = context.assets.open("vendors/$fileName").bufferedReader().use { it.readText() }
            File(vendorsDir, fileName).writeText(text)
        }
    }

    fun listVendors(): List<VendorTemplate> {
        ensureSeeded()
        return vendorsDir.listFiles { f -> f.extension == "json" }
            ?.sortedBy { it.name }
            ?.mapNotNull { file ->
                runCatching { VendorTemplate.fromJsonString(file.readText()) }.getOrNull()
            }
            ?: emptyList()
    }

    fun getVendor(id: String): VendorTemplate? =
        listVendors().firstOrNull { it.id == id }

    fun saveVendor(template: VendorTemplate) {
        File(vendorsDir, "${template.id}.json").writeText(template.toJsonString())
    }

    fun deleteVendor(id: String) {
        File(vendorsDir, "$id.json").delete()
    }

    /** Turns a display name into a safe, unique-enough file id. */
    fun slugify(name: String): String {
        val base = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        var candidate = base.ifBlank { "vendor" }
        var suffix = 1
        while (File(vendorsDir, "$candidate.json").exists()) {
            suffix += 1
            candidate = "${base}_$suffix"
        }
        return candidate
    }
}
