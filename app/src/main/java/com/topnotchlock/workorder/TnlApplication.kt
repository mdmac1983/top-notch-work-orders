package com.topnotchlock.workorder

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class TnlApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Required once before any PdfBox-Android PDDocument use.
        PDFBoxResourceLoader.init(applicationContext)
    }
}
