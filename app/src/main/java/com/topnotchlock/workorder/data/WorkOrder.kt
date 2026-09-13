package com.topnotchlock.workorder.data

/**
 * Mirrors every fillable field on the Top Notch Lock paper work order template.
 * Signature lines, Date, Time In/Out, and Manager on Duty are intentionally left
 * off this model (and off the generated PDF) as blank underscores to be signed
 * by hand after the work order is printed, per the paper-form workflow.
 */
data class WorkOrder(
    val woNumber: String = "",
    val date: String = "",
    val companyName: String = "",
    val companyPhone: String = "",
    val siteName: String = "",
    val siteId: String = "",
    val address: String = "",
    val contact: String = "",
    val phone: String = "",
    val arriveBy: String = "",
    val completeBy: String = "",
    val problem: String = "",
    val vendorId: String = "",
    val vendorName: String = ""
) {
    fun get(field: FieldKey): String = when (field) {
        FieldKey.SITE_NAME -> siteName
        FieldKey.SITE_ID -> siteId
        FieldKey.ADDRESS -> address
        FieldKey.CONTACT -> contact
        FieldKey.PHONE -> phone
        FieldKey.ARRIVE_BY -> arriveBy
        FieldKey.COMPLETE_BY -> completeBy
        FieldKey.PROBLEM -> problem
    }

    fun with(field: FieldKey, value: String): WorkOrder = when (field) {
        FieldKey.SITE_NAME -> copy(siteName = value)
        FieldKey.SITE_ID -> copy(siteId = value)
        FieldKey.ADDRESS -> copy(address = value)
        FieldKey.CONTACT -> copy(contact = value)
        FieldKey.PHONE -> copy(phone = value)
        FieldKey.ARRIVE_BY -> copy(arriveBy = value)
        FieldKey.COMPLETE_BY -> copy(completeBy = value)
        FieldKey.PROBLEM -> copy(problem = value)
    }

    companion object {
        fun fromExtractedFields(
            fields: Map<FieldKey, String>,
            vendorId: String,
            vendorName: String,
            woNumber: String,
            date: String
        ): WorkOrder {
            var wo = WorkOrder(woNumber = woNumber, date = date, vendorId = vendorId, vendorName = vendorName)
            fields.forEach { (key, value) -> wo = wo.with(key, value) }
            return wo
        }
    }
}

/** Fixed checklist printed on every work order - matches the paper template exactly. */
val ACTION_REQUIRED_ITEMS = listOf(
    "Check in to office upon arrival",
    "Do not perform work without approval from office",
    "Before & after photos must be taken",
    "Do not leave site without cleaning up",
    "Manager name and signature required",
    "Always check out with office",
    "Never use additional parts without approval"
)
