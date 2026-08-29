package com.topnotchlock.workorder.parsing

import com.topnotchlock.workorder.data.ExtractionStrategy
import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.data.FieldRule
import com.topnotchlock.workorder.data.VendorTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorParserTest {

    /** Mirrors app/src/main/assets/vendors/example_vendor.json exactly. */
    private val exampleVendor = VendorTemplate(
        id = "example_vendor",
        name = "Example Vendor",
        rules = listOf(
            FieldRule(FieldKey.SITE_NAME, ExtractionStrategy.AFTER_LABEL, label = "Site Name"),
            FieldRule(FieldKey.SITE_ID, ExtractionStrategy.AFTER_LABEL, label = "Site ID"),
            FieldRule(FieldKey.ADDRESS, ExtractionStrategy.AFTER_LABEL, label = "Address"),
            FieldRule(FieldKey.CONTACT, ExtractionStrategy.AFTER_LABEL, label = "On-site Contact"),
            FieldRule(FieldKey.PHONE, ExtractionStrategy.AFTER_LABEL, label = "Contact Phone"),
            FieldRule(FieldKey.ARRIVE_BY, ExtractionStrategy.AFTER_LABEL, label = "Requested Arrival"),
            FieldRule(FieldKey.COMPLETE_BY, ExtractionStrategy.AFTER_LABEL, label = "Must Complete By"),
            FieldRule(
                FieldKey.PROBLEM, ExtractionStrategy.BETWEEN_LABELS,
                startLabel = "Description of Issue", endLabel = "Please dispatch"
            )
        )
    )

    private val sampleRequest = """
        Site Name: ABC Plaza
        Site ID: 4521
        Address: 123 Main St, Springfield, IL 62704
        On-site Contact: Jane Doe
        Contact Phone: 555-123-4567
        Requested Arrival: 8/29/2026 9:00 AM
        Must Complete By: 8/29/2026 5:00 PM

        Description of Issue:
        Front door lock is sticking and the key is hard to turn. Tenant reports it has gotten worse over the last week.

        Please dispatch a technician ASAP.
    """.trimIndent()

    @Test
    fun `after-label rules pull the right value from each labeled line`() {
        val results = VendorParser.parseToMap(sampleRequest, exampleVendor)
        assertEquals("ABC Plaza", results[FieldKey.SITE_NAME])
        assertEquals("4521", results[FieldKey.SITE_ID])
        assertEquals("123 Main St, Springfield, IL 62704", results[FieldKey.ADDRESS])
        assertEquals("Jane Doe", results[FieldKey.CONTACT])
        assertEquals("555-123-4567", results[FieldKey.PHONE])
        assertEquals("8/29/2026 9:00 AM", results[FieldKey.ARRIVE_BY])
        assertEquals("8/29/2026 5:00 PM", results[FieldKey.COMPLETE_BY])
    }

    @Test
    fun `between-labels rule captures the full multi-line problem description`() {
        val results = VendorParser.parseToMap(sampleRequest, exampleVendor)
        val problem = results[FieldKey.PROBLEM].orEmpty()
        assertTrue(problem.contains("Front door lock is sticking"))
        assertTrue(problem.contains("gotten worse over the last week"))
        assertTrue(!problem.contains("Please dispatch"))
    }

    @Test
    fun `missing fields come back unmatched rather than throwing`() {
        val incompleteVendor = VendorTemplate(
            id = "v", name = "V",
            rules = listOf(FieldRule(FieldKey.SITE_NAME, ExtractionStrategy.AFTER_LABEL, label = "Nonexistent Label"))
        )
        val results = VendorParser.parse("Some unrelated text with no labels at all.", incompleteVendor)
        val siteNameResult = results.first { it.field == FieldKey.SITE_NAME }
        assertTrue(!siteNameResult.matched)
        assertEquals("", siteNameResult.value)
    }

    @Test
    fun `regex strategy extracts using the first capture group`() {
        val regexVendor = VendorTemplate(
            id = "v", name = "V",
            rules = listOf(
                FieldRule(
                    FieldKey.PHONE, ExtractionStrategy.REGEX,
                    pattern = "Call us at \\(?(\\d{3}[-)]\\s?\\d{3}-\\d{4})"
                )
            )
        )
        val result = VendorParser.parseToMap("Please call us at (555) 234-5678 for questions.", regexVendor)
        assertEquals("555) 234-5678", result[FieldKey.PHONE])
    }

    @Test
    fun `static strategy always returns the fixed value`() {
        val staticVendor = VendorTemplate(
            id = "v", name = "V",
            rules = listOf(FieldRule(FieldKey.SITE_NAME, ExtractionStrategy.STATIC, staticValue = "Main Branch"))
        )
        val result = VendorParser.parseToMap("anything at all", staticVendor)
        assertEquals("Main Branch", result[FieldKey.SITE_NAME])
    }

    @Test
    fun `vendor template round-trips through JSON without losing rules`() {
        val json = exampleVendor.toJsonString()
        val roundTripped = VendorTemplate.fromJsonString(json)
        assertEquals(exampleVendor.rules.size, roundTripped.rules.size)
        val results = VendorParser.parseToMap(sampleRequest, roundTripped)
        assertEquals("ABC Plaza", results[FieldKey.SITE_NAME])
    }
}
