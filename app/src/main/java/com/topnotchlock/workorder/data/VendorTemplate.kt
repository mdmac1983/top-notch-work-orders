package com.topnotchlock.workorder.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * How to pull one field's value out of a vendor's raw request text.
 *
 * AFTER_LABEL       - find a line/spot containing [label], take the text that follows it
 *                      on the same line (after an optional separator like ":" or "-"),
 *                      stopping at [endLabel] if one is given.
 * BETWEEN_LABELS    - capture everything between [startLabel] and [endLabel] (or end of
 *                      text if [endLabel] is blank). Useful for multi-line problem
 *                      descriptions.
 * REGEX             - apply [pattern] to the whole text and take capture group 1.
 * STATIC            - always use [staticValue] (e.g. a vendor that never includes an
 *                      address you already know).
 */
enum class ExtractionStrategy {
    AFTER_LABEL,
    BETWEEN_LABELS,
    REGEX,
    STATIC
}

data class FieldRule(
    val field: FieldKey,
    val strategy: ExtractionStrategy,
    val label: String = "",
    val startLabel: String = "",
    val endLabel: String = "",
    val pattern: String = "",
    val staticValue: String = "",
    // If the label appears more than once, which occurrence to use (0 = first).
    val occurrence: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("field", field.name)
        put("strategy", strategy.name)
        put("label", label)
        put("startLabel", startLabel)
        put("endLabel", endLabel)
        put("pattern", pattern)
        put("staticValue", staticValue)
        put("occurrence", occurrence)
    }

    companion object {
        fun fromJson(json: JSONObject): FieldRule? {
            val field = FieldKey.fromNameOrNull(json.optString("field")) ?: return null
            val strategy = runCatching { ExtractionStrategy.valueOf(json.optString("strategy")) }
                .getOrDefault(ExtractionStrategy.AFTER_LABEL)
            return FieldRule(
                field = field,
                strategy = strategy,
                label = json.optString("label"),
                startLabel = json.optString("startLabel"),
                endLabel = json.optString("endLabel"),
                pattern = json.optString("pattern"),
                staticValue = json.optString("staticValue"),
                occurrence = json.optInt("occurrence", 0)
            )
        }
    }
}

data class VendorTemplate(
    val id: String,
    val name: String,
    val notes: String = "",
    val rules: List<FieldRule> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("notes", notes)
        put("rules", JSONArray(rules.map { it.toJson() }))
    }

    fun toJsonString(indent: Int = 2): String = toJson().toString(indent)

    companion object {
        fun fromJson(json: JSONObject): VendorTemplate {
            val rulesArray = json.optJSONArray("rules") ?: JSONArray()
            val rules = (0 until rulesArray.length()).mapNotNull { i ->
                FieldRule.fromJson(rulesArray.getJSONObject(i))
            }
            return VendorTemplate(
                id = json.optString("id"),
                name = json.optString("name"),
                notes = json.optString("notes"),
                rules = rules
            )
        }

        fun fromJsonString(text: String): VendorTemplate = fromJson(JSONObject(text))

        /** A starting point for onboarding a brand-new vendor. */
        fun blank(id: String, name: String): VendorTemplate = VendorTemplate(
            id = id,
            name = name,
            notes = "Fill in a rule for each field below, then paste a sample request " +
                "under Vendors > Test Extraction to check it.",
            rules = FieldKey.entries.map { key ->
                FieldRule(field = key, strategy = ExtractionStrategy.AFTER_LABEL, label = key.label)
            }
        )
    }
}
