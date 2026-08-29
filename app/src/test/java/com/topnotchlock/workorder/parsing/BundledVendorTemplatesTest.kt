package com.topnotchlock.workorder.parsing

import com.topnotchlock.workorder.data.FieldKey
import com.topnotchlock.workorder.data.VendorTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Loads the real vendor JSON files bundled in assets/vendors and runs them
 * against a realistic sample of that vendor's actual dispatch format, to
 * prove the shipped rules work end-to-end through the real parsing engine
 * (not just a hand-written unit test template).
 */
class BundledVendorTemplatesTest {

    private fun loadVendor(fileName: String): VendorTemplate {
        val file = File("src/main/assets/vendors/$fileName")
        return VendorTemplate.fromJsonString(file.readText())
    }

    @Test
    fun `dynamic vendor extracts site, address, dates and problem`() {
        val template = loadVendor("dynamic.json")
        val sample = """
            Dynamics Order # Date Issued
            PO17784 08/22/2026
            GENERAL INFORMATION
            Remit to Contact Information
            AP@dynamic-es.com
            866-287-1362
            Service Manager : Kevin Thimm
            Email :
            KThimm@dynamic-es.com
            Phone :
            JOB INSTRUCTIONS & INFORMATION
            Job Site Information SLA (Service Level Agreement) Client IVR
            Customer :
            Top Notch Lock LLC
            Response by Date :
            08/22/2026
            Technician(s) must check in and out of IVR via the Service
            Channel App: Download the free "Service Channel" app.
            Customer IVR# :361146064
            Address :
            Trader Joe's - 096 - Oro Valley
            AZ
            7912 N. Oracle
            Oro Valley AZ 85704
            United States
            Complete by Date :
            Scope of Work
            ***EMERGENCY***
            STOCK ROOM / DOORS / OVERHEAD/ROLLUP / STUCK / DOOR IS STUCK AND WON'T ROLL DOWN/UP
            Site Requirements :
            LINE ITEMS
        """.trimIndent()

        val results = VendorParser.parseToMap(sample, template)
        assertEquals("Trader Joe's - 096 - Oro Valley\nAZ", results[FieldKey.SITE_NAME])
        assertEquals("7912 N. Oracle\nOro Valley AZ 85704\nUnited States", results[FieldKey.ADDRESS])
        assertEquals("08/22/2026", results[FieldKey.ARRIVE_BY])
        assertEquals("", results[FieldKey.COMPLETE_BY])
        assertEquals(
            "***EMERGENCY***\nSTOCK ROOM / DOORS / OVERHEAD/ROLLUP / STUCK / DOOR IS STUCK AND WON'T ROLL DOWN/UP",
            results[FieldKey.PROBLEM]
        )
    }

    @Test
    fun `23rd group vendor extracts site, address, phone, arrival and problem`() {
        val template = loadVendor("23rd_group.json")
        val sample = """
            DavidG@23rdgroup.com
            David Garcia
            361579921
            72 hrs
            General Locksmith / Exit Device
            Dispatch NTE ${'$'}155.00 | Total: ${'$'}155.00
            Order Type
            Priority
            Input Date 8/26/26 05:22 pm
            704-909-4423 ext 5654
            Client PO #
            Service Date 8/31/26 11:38 AM
            2021011-01
            VENDOR PO #
            23rd Group
            4944 Parkway Plaza Blvd, Ste 400
            Charlotte, NC 28217
            Phone # 704-909-4423
            SERVICE LOCATION
            Lakeside Dental Care - Loc # SN0101977
            4920 S Alma School Rd Ste 1
            Chandler, AZ 85248-5547
            Phone # 480-739-6404
            Top Notch Lock LLC
            4435 E CHandler Blvd
            Suite 200
            Phoenix, AZ 85048
            Phone # 800-381-7033 Fax #
            VENDOR # 342017
            SERVICE DESCRIPTION
            EXTERIOR / Locks / Locks / Rekeying Required / 2 doors need to be rekeyed
            BILLING INSTRUCTIONS
            REQUIREMENTS - PLEASE READ.
        """.trimIndent()

        val results = VendorParser.parseToMap(sample, template)
        assertEquals("Lakeside Dental Care - Loc # SN0101977", results[FieldKey.SITE_NAME])
        assertEquals("SN0101977", results[FieldKey.SITE_ID])
        assertEquals("4920 S Alma School Rd Ste 1\nChandler, AZ 85248-5547", results[FieldKey.ADDRESS])
        assertEquals("480-739-6404", results[FieldKey.PHONE])
        assertEquals("8/31/26 11:38 AM", results[FieldKey.ARRIVE_BY])
        assertEquals("EXTERIOR / Locks / Locks / Rekeying Required / 2 doors need to be rekeyed", results[FieldKey.PROBLEM])
        // Make sure the NTE amount is never captured anywhere.
        results.values.forEach { assertEquals(false, it.contains("155.00")) }
    }

    @Test
    fun `nest vendor extracts site, id, address, phone, arrival and problem`() {
        val template = loadVendor("nest.json")
        val sample = """
            WORK ORDER
            7177549-01
            Client Details
            Mattress Firm
            Mattress Firm
            Location#: 753009
            Free Standing
            44765 West Hathaway Avenue
            Maricopa, AZ 85138
            Phone # 520-200-0090
            Work Order Details
            Top Notch Lock LLC
            Priority: Emergency
            Category: Locksmith
            Service: Unable to Unlock - Exterior Door
            Schedule Date: 08/28/2026 01:30 PM
            Check-In Instructions
            Checking In/Out is REQUIRED.
            Service Description
            Store locked their key in the store and has no other way to get in. Please send tech to open the store.
            Signatures Use ISP Connect to obtain Esignature
        """.trimIndent()

        val results = VendorParser.parseToMap(sample, template)
        assertEquals("Mattress Firm", results[FieldKey.SITE_NAME])
        assertEquals("753009", results[FieldKey.SITE_ID])
        assertEquals("44765 West Hathaway Avenue\nMaricopa, AZ 85138", results[FieldKey.ADDRESS])
        assertEquals("520-200-0090", results[FieldKey.PHONE])
        assertEquals("08/28/2026 01:30 PM", results[FieldKey.ARRIVE_BY])
        assertEquals(
            "Store locked their key in the store and has no other way to get in. Please send tech to open the store.",
            results[FieldKey.PROBLEM]
        )
    }

    @Test
    fun `truesource vendor extracts site, id, address, contact, phone, arrival and problem`() {
        val template = loadVendor("truesource.json")
        val sample = """
            Dear TOP NOTCH LOCK LLC,
            Please see details below regarding new Work Order WO-03538420.
            Customer Name: JOURNEYS - COTTONWOOD MALL #A-214; 10000 COORS BLVD BYP NW
            Site Address: COTTONWOOD MALL #A-214; 10000 COORS BLVD BYP NW, ALBUQUERQUE, NM 87114-4040
            Accepted and Scheduled Tech ETA: 07/12/2026 12:15 PM (GMT-06:00) Mountain Daylight Time
            Not to Exceed Amount: ${'$'} 249.99
            Work Order Priority: 4 Hour Response
            On-site contact: Manager, On Duty, (505) 898-9101
            Description of Work: Security Grille Gate cannot open or close. Please repair.
            Customer IVR is required to validate billable hours, call: (516) 500-7776
        """.trimIndent()

        val results = VendorParser.parseToMap(sample, template)
        assertEquals("JOURNEYS - COTTONWOOD MALL #A-214", results[FieldKey.SITE_NAME])
        assertEquals("A-214", results[FieldKey.SITE_ID])
        assertEquals(
            "COTTONWOOD MALL #A-214; 10000 COORS BLVD BYP NW, ALBUQUERQUE, NM 87114-4040",
            results[FieldKey.ADDRESS]
        )
        assertEquals("Manager, On Duty", results[FieldKey.CONTACT])
        assertEquals("(505) 898-9101", results[FieldKey.PHONE])
        assertEquals("07/12/2026 12:15 PM", results[FieldKey.ARRIVE_BY])
        assertEquals("Security Grille Gate cannot open or close. Please repair.", results[FieldKey.PROBLEM])
        // Make sure the NTE amount is never captured anywhere.
        results.values.forEach { assertEquals(false, it.contains("249.99")) }
    }

    @Test
    fun `cbre vendor extracts fields from a merged two-column OCR layout`() {
        val template = loadVendor("cbre.json")
        val sample = """
            CBRE facilitysource IFM
            WEB-3558198 - Pending Acceptance
            Client Request Type
            IFM DOORS EXTERIOR
            Location ID Request Code
            SBH-0793 DOORS EXTERIOR
            Address DNE
            702 W CAMELBACK ROAD , Phoenix, ARIZONA 85013 USD 1,400.00
            Location Phone Service Location
            602-277-5405 OTHER
            Priority Arrive on site by Complete work by
            P4 08/26/2026 01:12 AM MDT 08/29/2026 09:12 AM MDT
            WO Description
            Door doesn't latch itself correctly and the chime doesn't activate.
            Accept Reject
            Location Description Location Notes
            Sally Beauty: SBH-0793 N/A
            Requesting Contact Alternate Contact
            StoreManager0793 SBH-0793 (602) 405-8635
        """.trimIndent()

        val results = VendorParser.parseToMap(sample, template)
        assertEquals("Sally Beauty", results[FieldKey.SITE_NAME])
        assertEquals("SBH-0793", results[FieldKey.SITE_ID])
        assertEquals("702 W CAMELBACK ROAD , Phoenix, ARIZONA 85013", results[FieldKey.ADDRESS])
        assertEquals("StoreManager0793 SBH-0793", results[FieldKey.CONTACT])
        assertEquals("(602) 405-8635", results[FieldKey.PHONE])
        assertEquals("08/26/2026 01:12 AM MDT", results[FieldKey.ARRIVE_BY])
        assertEquals("08/29/2026 09:12 AM MDT", results[FieldKey.COMPLETE_BY])
        // Make sure the DNE (do-not-exceed) amount is never captured anywhere.
        results.values.forEach { assertEquals(false, it.contains("1,400.00")) }
    }
}
