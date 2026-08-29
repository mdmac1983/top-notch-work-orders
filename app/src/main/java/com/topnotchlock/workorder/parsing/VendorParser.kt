package com.topnotchlock.workorder.parsing

import com.topnotchlock.workorder.data.ExtractionStrategy
import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.data.FieldRule
import com.topnotchlock.workorder.data.VendorTemplate

data class FieldExtractionResult(
    val field: FieldKey,
    val value: String,
    val matched: Boolean
)

/**
 * Applies a vendor's [FieldRule]s to raw OCR/paste/PDF text and produces a value
 * for every work-order field it knows how to find. Anything it can't find is
 * returned as an empty string with matched = false, so the Review screen can
 * flag it for the office to fill in by hand.
 */
object VendorParser {

    fun parse(rawText: String, template: VendorTemplate): List<FieldExtractionResult> {
        val cleaned = normalize(rawText)
        val lines = cleaned.lines()
        return FieldKey.entries.map { field ->
            val rule = template.rules.firstOrNull { it.field == field }
            if (rule == null) {
                FieldExtractionResult(field, "", false)
            } else {
                val value = extract(cleaned, lines, rule)
                FieldExtractionResult(field, value, value.isNotBlank())
            }
        }
    }

    fun parseToMap(rawText: String, template: VendorTemplate): Map<FieldKey, String> =
        parse(rawText, template).associate { it.field to it.value }

    private fun normalize(text: String): String =
        text.replace("\r\n", "\n").replace('\r', '\n')

    private fun extract(fullText: String, lines: List<String>, rule: FieldRule): String {
        return when (rule.strategy) {
            ExtractionStrategy.STATIC -> rule.staticValue
            ExtractionStrategy.REGEX -> extractRegex(fullText, rule)
            ExtractionStrategy.AFTER_LABEL -> extractAfterLabel(lines, rule)
            ExtractionStrategy.BETWEEN_LABELS -> extractBetweenLabels(fullText, rule)
        }.trim()
    }

    private fun extractRegex(fullText: String, rule: FieldRule): String {
        if (rule.pattern.isBlank()) return ""
        return runCatching {
            val regex = Regex(rule.pattern, setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            val matches = regex.findAll(fullText).toList()
            val match = matches.getOrNull(rule.occurrence) ?: return ""
            if (match.groupValues.size > 1) match.groupValues[1] else match.value
        }.getOrDefault("")
    }

    private fun extractAfterLabel(lines: List<String>, rule: FieldRule): String {
        if (rule.label.isBlank()) return ""
        val matchingIndices = lines.indices.filter { i ->
            lines[i].contains(rule.label, ignoreCase = true)
        }
        val lineIndex = matchingIndices.getOrNull(rule.occurrence) ?: return ""
        val line = lines[lineIndex]
        val labelStart = line.indexOf(rule.label, ignoreCase = true)
        var remainder = line.substring(labelStart + rule.label.length)
        remainder = stripLeadingSeparator(remainder)

        // If nothing useful is left on this line, the value is probably on the next line.
        if (remainder.isBlank() && lineIndex + 1 < lines.size) {
            remainder = lines[lineIndex + 1]
        }

        if (rule.endLabel.isNotBlank()) {
            val endIdx = remainder.indexOf(rule.endLabel, ignoreCase = true)
            if (endIdx >= 0) remainder = remainder.substring(0, endIdx)
        }
        return remainder.trim()
    }

    private fun extractBetweenLabels(fullText: String, rule: FieldRule): String {
        if (rule.startLabel.isBlank()) return ""
        val startIdx = fullText.indexOf(rule.startLabel, ignoreCase = true)
        if (startIdx < 0) return ""
        val afterStart = startIdx + rule.startLabel.length
        var contentStart = afterStart
        // Skip a trailing separator/newline right after the label itself.
        contentStart += (fullText.substring(afterStart).takeWhile { it == ':' || it == '-' || it == ' ' }).length

        val endIdx = if (rule.endLabel.isNotBlank()) {
            val idx = fullText.indexOf(rule.endLabel, ignoreCase = true, startIndex = contentStart)
            if (idx >= 0) idx else fullText.length
        } else {
            fullText.length
        }
        if (contentStart >= endIdx) return ""
        return fullText.substring(contentStart, endIdx).trim('\n', '\r', ' ', '\t')
    }

    private fun stripLeadingSeparator(text: String): String {
        var result = text
        while (result.isNotEmpty() && (result.first() == ':' || result.first() == '-' ||
                result.first() == '–' || result.first() == '—' || result.first() == ' ')) {
            result = result.substring(1)
        }
        return result.trim()
    }
}
