package com.topnotchlock.workorder.data

import android.content.Context

/**
 * Auto-increments the Work Order # (e.g. TNL-1001, TNL-1002, ...).
 * Prefix and the next number are both editable from the Settings screen -
 * no rebuild needed to match your existing numbering scheme.
 */
class WoNumberGenerator(context: Context) {

    private val prefs = context.getSharedPreferences("wo_counter", Context.MODE_PRIVATE)

    var prefix: String
        get() = prefs.getString(KEY_PREFIX, DEFAULT_PREFIX) ?: DEFAULT_PREFIX
        set(value) = prefs.edit().putString(KEY_PREFIX, value).apply()

    var nextNumber: Int
        get() = prefs.getInt(KEY_NEXT_NUMBER, DEFAULT_START)
        set(value) = prefs.edit().putInt(KEY_NEXT_NUMBER, value).apply()

    /** Formats the current counter without consuming it - used for a live preview. */
    fun previewNext(): String = format(nextNumber)

    /** Formats the current counter and advances it. Call once per generated work order. */
    fun consumeNext(): String {
        val value = format(nextNumber)
        nextNumber += 1
        return value
    }

    private fun format(number: Int): String =
        if (prefix.isBlank()) number.toString() else "$prefix-$number"

    companion object {
        private const val KEY_PREFIX = "prefix"
        private const val KEY_NEXT_NUMBER = "next_number"
        const val DEFAULT_PREFIX = "TNL"
        const val DEFAULT_START = 1001
    }
}
