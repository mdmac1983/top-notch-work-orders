package com.topnotchlock.workorder.data

/** One released version's notes, newest first in [CHANGELOG] below. */
data class ChangelogEntry(
    val version: String,
    val date: String,
    val notes: List<String>
)

/**
 * Bumped by 0.1 (and versionCode by 1 in app/build.gradle.kts) with every build from
 * here on, so it's always obvious from Settings whether a newly installed APK actually
 * contains the latest changes.
 */
val CHANGELOG = listOf(
    ChangelogEntry(
        version = "1.1",
        date = "2026-09-13",
        notes = listOf(
            "Fixed a build pipeline bug where uploading a new zip could produce a stale " +
                "APK with none of the latest changes (the separate Build APK workflow was " +
                "racing the zip-unpack step and grabbing the old source) - it now only runs " +
                "on real code pushes, not zip uploads.",
            "Added the app version and this changelog to Settings.",
            "Switched from a debug build to a properly signed release build, so it installs " +
                "and updates like a normal app instead of showing a debug watermark."
        )
    ),
    ChangelogEntry(
        version = "1.0",
        date = "2026-09-12",
        notes = listOf(
            "Initial release: OCR-based work order generation for all 6 vendors, one-page " +
                "shrink-to-fit PDF output, editable vendor rules, work order history.",
            "Added the Top Notch Lock logo as the app icon, splash screen, PDF header logo, " +
                "and a faded PDF watermark.",
            "Added multi-company support (Settings > Companies) with a per-work-order " +
                "dropdown for which company's name/phone prints on the header.",
            "Added a built-in PDF viewer (pinch-zoom) plus Save to Downloads, alongside Share.",
            "Moved generated PDFs to persistent storage so History always has something to " +
                "open, and made History rows tappable."
        )
    )
)
