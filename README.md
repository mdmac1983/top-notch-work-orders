# Top Notch Lock - Work Order Generator (Android)

An Android app that turns a vendor's dispatch request (pasted email text, a PDF
attachment, or a screenshot) into a filled-out, one-page Top Notch Lock work
order PDF, ready to share or print.

- **Ships pre-loaded with all 6 of your vendors** - Dynamic, 23rd Group,
  FrontStreet, Nest, CBRE, and TrueSource - each already tuned to that
  vendor's real dispatch format (see section 3 below).
- **No coding required to use it day-to-day.** Everything vendor-specific
  (what to look for, where) lives in editable JSON rules on the **Vendors**
  screen in the app - not hardcoded in the source. Adding a 7th vendor later
  never requires a new APK.
- **Never pulls in the Not-to-Exceed (NTE/DNE) amount.** None of the 6
  vendor templates have a rule for it, and the work order template itself
  has no field for it - it can never end up on a generated work order.
- **Reads text three ways:** paste it in, import a PDF, import a screenshot,
  or take a photo. You can also just use Android's **Share** button from
  Gmail/Outlook/your screenshot gallery and pick "TNL Work Orders."
- **On-device OCR** (Google ML Kit) - free, no API key, works with weak
  signal on a jobsite. PDFs try a fast direct text read first and only fall
  back to OCR if the PDF is a scanned image with no real text in it.
- **Always fits one page.** The PDF generator measures the whole work order
  and shrinks font size / line spacing / margins step-by-step until it fits
  on a single 8.5" x 11" page, no matter how long the problem description is.
- **Work order numbers auto-increment** (e.g. `TNL-1001`, `TNL-1002`, ...) -
  editable prefix and starting number under **Settings**.
- Signature lines (Technician, Manager, Date, Time In/Out) print as blank
  underscores to be signed by hand after printing, matching your existing
  paper workflow.

---

## 1. Get this code onto GitHub

1. Create a new empty repository on GitHub (no README/license, so it doesn't
   conflict with these files).
2. From this folder:
   ```bash
   git init
   git add .
   git commit -m "Initial commit - Top Notch Lock work order app"
   git branch -M main
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
   git push -u origin main
   ```

That's it - pushing to `main` automatically builds the APK (next section).

## 2. Get the APK from GitHub (no Android Studio needed)

This repo includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that builds a debug APK on every push:

1. On GitHub, open the **Actions** tab of your repo.
2. Click the latest **Build APK** run (it starts automatically after you push).
3. Once it finishes (green check, a few minutes), scroll to **Artifacts** at
   the bottom and download **TNL-WorkOrders-debug-apk**. It's a zip
   containing `app-debug.apk`.
4. Transfer that APK to your Android phone (email it to yourself, Google
   Drive, USB, etc.) and tap it to install. Android will warn about
   "installing from unknown sources" the first time - that's expected for an
   app not published on the Play Store; allow it for that install.

You can also trigger a build manually anytime from **Actions > Build APK >
Run workflow**.

### Building it yourself instead (optional)

If you'd rather build locally with Android Studio: open this folder as a
project, let it sync, then **Build > Build Bundle(s)/APK(s) > Build APK(s)**.
Requires JDK 17 and the Android SDK (Android Studio installs both).

## 3. Your 6 vendors

The app ships pre-loaded with real, tested rules for all 6 vendors, built
from actual sample POs/work orders:

| Vendor | Source format | Fields it can't find in that vendor's format (left blank for you) |
|---|---|---|
| **Dynamic** | PDF | Site ID, Contact, Phone (often no Complete By either - emergency calls only give a response date) |
| **23rd Group** | PDF | Contact, Complete By (only a Priority window like "72 hrs" is given) |
| **FrontStreet** | PDF | Contact, Complete By |
| **Nest** | PDF | Contact, Complete By |
| **TrueSource** | Email / screenshot | Complete By (only a response window like "4 Hour Response" is given) |
| **CBRE** | Screenshot (Affiliate Connect portal) | Contact isn't always reliable - see note below |

A blank field just means that vendor's format never states it plainly enough
to auto-fill safely - the Review screen highlights it so you type it in by
hand from the source in a few seconds. Every other field is auto-filled.

**One thing worth knowing about CBRE specifically:** their work order page
is wider than a phone screen, so a portrait screenshot can crop off real
data on the right side (like the full Complete-By date). Before
screenshotting, try rotating to landscape or using "Desktop site" in your
browser so the whole table is captured. Because CBRE comes from OCR'd
screenshots rather than a clean PDF, it's also the one most likely to need
a small tweak if your store's layout differs slightly from the sample this
was built from - see below.

### If a field ever comes out wrong (any vendor)

1. Open **Vendors**, tap the vendor.
2. Paste the actual text (or a sanitized copy) into **Test Extraction** and
   hit **Run Test** - you'll see exactly what each field extracted.
3. Adjust that field's rule and re-test until it's right, then **Save
   Vendor Rules**. No rebuild, no waiting on anyone - it's all in the app.

Each field's rule uses one of these strategies:
- **AFTER_LABEL** - grabs the text right after a label like `Site Name:`
  on the same line (or the next line, if the label is alone on its own
  line).
- **BETWEEN_LABELS** - grabs everything between two labels. Best for
  multi-line problem descriptions.
- **REGEX** - an advanced pattern with one capture group, for vendors whose
  layout mixes values together (most of the 6 above use this under the
  hood - it's more reliable than it sounds once it's set up).
- **STATIC** - always fills in a fixed value.

Send me a new vendor's name and 1-2 sample requests (sensitive info blanked
out) any time and I'll write ready-to-paste JSON for it the same way.

### Vendor rule JSON, by hand

Each vendor is one JSON file with this shape:

```json
{
  "id": "acme_property_mgmt",
  "name": "Acme Property Management",
  "notes": "Anything you want to remember about this vendor's quirks.",
  "rules": [
    { "field": "SITE_NAME", "strategy": "AFTER_LABEL", "label": "Property Name", "startLabel": "", "endLabel": "", "pattern": "", "staticValue": "", "occurrence": 0 },
    { "field": "PROBLEM", "strategy": "BETWEEN_LABELS", "label": "", "startLabel": "Issue Description:", "endLabel": "Notes:", "pattern": "", "staticValue": "", "occurrence": 0 }
  ]
}
```

`field` must be one of: `SITE_NAME`, `SITE_ID`, `ADDRESS`, `CONTACT`, `PHONE`,
`ARRIVE_BY`, `COMPLETE_BY`, `PROBLEM`. See any file in
`app/src/main/assets/vendors/` for full working examples (including REGEX
patterns) built from your real vendors.

## 4. Everyday use

1. **New Work Order** > pick the vendor > paste the email text (or import
   the PDF/screenshot/photo).
2. Review the auto-filled fields - anything the app couldn't find is
   outlined and labeled "not found" so you can type it in by hand.
3. **Generate PDF & Share** - creates the one-page PDF, saves it to
   **History**, and opens Android's share sheet so you can text/email/print
   it immediately.

Sharing an email or screenshot directly from Gmail/Outlook/your gallery's
share menu into "TNL Work Orders" jumps straight to step 2 with the vendor
picker ready to go.

## Project structure

```
app/src/main/java/com/topnotchlock/workorder/
  data/           Work order + vendor template models, Room DB, WO# counter
  ocr/            ML Kit image OCR, PDF text extraction (+ OCR fallback)
  parsing/        Applies a vendor's JSON rules to raw text
  pdfgen/         Renders the work order to a single-page PDF (shrink-to-fit)
  ui/             Jetpack Compose screens + navigation
app/src/main/assets/vendors/   Your 6 bundled vendor templates (JSON)
app/src/test/                  Unit tests for the parsing engine, including
                                tests that load each bundled vendor JSON file
                                and verify it against a real sample request
.github/workflows/build-apk.yml   Builds the APK automatically on push
```

## Notes / known limitations

- The app targets Android 8.0 (API 26) and up.
- ML Kit's text recognition model downloads once via Google Play services
  (needs internet the first time you OCR anything); after that it runs
  fully offline.
- Work order numbers advance every time you start a new work order, even if
  you back out without generating the PDF - this guarantees no two work
  orders ever get the same number, at the cost of occasionally skipping one.
- Signature lines print blank for signing on paper (no in-app signature pad)
  - see Settings if you'd like that changed later.
