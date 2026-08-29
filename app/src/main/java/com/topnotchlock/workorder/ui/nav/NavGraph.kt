package com.topnotchlock.workorder.ui.nav

object Routes {
    const val HOME = "home"
    const val NEW_WORK_ORDER = "new_work_order"
    const val REVIEW = "review"
    const val VENDORS = "vendors"
    const val VENDOR_EDIT = "vendor_edit/{vendorId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun vendorEdit(vendorId: String) = "vendor_edit/$vendorId"
}
