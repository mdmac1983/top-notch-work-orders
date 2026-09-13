package com.topnotchlock.workorder.data

import android.content.Context
import org.json.JSONArray

/**
 * The businesses this app can generate work orders for. Most shops only ever
 * have one, but the list supports more than one d/b/a - useful if you run
 * several companies and want each work order headed with the right name and
 * phone number. Stored as a small JSON list in SharedPreferences (not tied to
 * the vendor JSON files), seeded on first launch with Top Notch Lock. Managed
 * from the Settings screen; picked per work order from a dropdown on Review.
 */
class CompanyRepository(context: Context) {

    private val prefs = context.getSharedPreferences("companies", Context.MODE_PRIVATE)

    fun listCompanies(): List<Company> {
        ensureSeeded()
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        val array = JSONArray(raw)
        return (0 until array.length()).map { Company.fromJson(array.getJSONObject(it)) }
    }

    fun defaultCompanyId(): String {
        ensureSeeded()
        return prefs.getString(KEY_DEFAULT, null) ?: listCompanies().firstOrNull()?.id.orEmpty()
    }

    fun setDefaultCompanyId(id: String) {
        prefs.edit().putString(KEY_DEFAULT, id).apply()
    }

    /** The company to pre-select for a brand-new work order. */
    fun defaultCompany(): Company? {
        val companies = listCompanies()
        return companies.firstOrNull { it.id == defaultCompanyId() } ?: companies.firstOrNull()
    }

    fun getCompany(id: String): Company? = listCompanies().firstOrNull { it.id == id }

    /** Adds a new company, or replaces an existing one with the same id. */
    fun addOrUpdate(company: Company) {
        val current = listCompanies().filter { it.id != company.id }.toMutableList()
        current.add(company)
        saveList(current)
    }

    fun delete(id: String) {
        val remaining = listCompanies().filter { it.id != id }
        saveList(remaining)
        if (defaultCompanyId() == id) {
            setDefaultCompanyId(remaining.firstOrNull()?.id.orEmpty())
        }
    }

    /** Turns a display name into a safe, unique-enough id. */
    fun newId(name: String): String {
        val base = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "company" }
        val existing = listCompanies().map { it.id }.toSet()
        var candidate = base
        var suffix = 1
        while (candidate in existing) {
            suffix += 1
            candidate = "${base}_$suffix"
        }
        return candidate
    }

    private fun ensureSeeded() {
        if (prefs.contains(KEY_LIST)) return
        val seed = Company(id = "top_notch_lock", name = "TOP NOTCH LOCK", phone = "(800)-381-7033")
        saveList(listOf(seed))
        setDefaultCompanyId(seed.id)
    }

    private fun saveList(list: List<Company>) {
        val array = JSONArray(list.map { it.toJson() })
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }

    companion object {
        private const val KEY_LIST = "list"
        private const val KEY_DEFAULT = "default_id"
    }
}
