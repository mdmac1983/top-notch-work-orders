package com.topnotchlock.workorder.data

/**
 * Every field that can appear on the Top Notch Lock work order template.
 * Vendor templates map their raw request text onto these keys.
 */
enum class FieldKey(val label: String) {
    SITE_NAME("Site Name"),
    SITE_ID("Site ID"),
    ADDRESS("Address"),
    CONTACT("Contact"),
    PHONE("Phone"),
    ARRIVE_BY("Arrive By"),
    COMPLETE_BY("Complete By"),
    PROBLEM("Problem Reported");

    companion object {
        fun fromNameOrNull(name: String): FieldKey? =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
