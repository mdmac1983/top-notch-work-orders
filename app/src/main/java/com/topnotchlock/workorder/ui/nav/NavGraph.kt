package com.topnotchlock.workorder.ui.nav

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val NEW_WORK_ORDER = "new_work_order"
    const val REVIEW = "review"
    const val VENDORS = "vendors"
    const val VENDOR_EDIT = "vendor_edit/{vendorId}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val PDF_VIEW = "pdf_view/{path}"

    fun vendorEdit(vendorId: String) = "vendor_edit/$vendorId"

    /** [path] is the absolute file path on disk; URL-encoded since it contains "/". */
    fun pdfView(path: String) = "pdf_view/${Uri.encode(path)}"
}
