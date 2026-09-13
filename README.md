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
- **Branded end to end.** The Top Notch Lock logo is the app icon, the splash
  screen you see on launch, the letterhead-style logo at the top of every
  generated PDF, and a large faded watermark behind the page content.
- **Supports more than one business.** Add companies (name + phone) under
  **Settings > Companies**; a dropdown on the Review screen picks which one's
  name/phone prints in a given work order's header - handy if Top Notch Lock
  isn't the only business you run work orders through.
- **Built-in PDF viewer.** Tap **View PDF** (on Review or from History) to
  read/zoom a work order right in the app - no separate PDF reader app
  required. **Share** is still there for handing it off to another app, and
  **Save to Downloads** copies it into your phone's regular Downloads folder.
- **Version + changelog in Settings.** Every build bumps the version by 0.1,
  and Settings > What's new lists exactly what changed in each one - so you
  can always tell whether a freshly installed APK actually has your latest
  changes.
- **Signed release build.** APKs are a proper signed release build (not
  debug), using a signing key that's checked into this repo so every future
  build can update in place over the last one.

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

This repo includes GitHub Actions workflows that build a **signed release
APK** (not a debug build) on every push - `.github/workflows/build-apk.yml`
for a normal git push, or `.github/workflows/unzip.yml` when you upload the
project as a `.zip` through GitHub's web UI:

1. On GitHub, open the **Actions** tab of your repo.
2. Click the latest run. **If you uploaded a `.zip`, use the "Unzip
   Project" run** - not "Build APK." Both may appear for the same push, but
   only "Unzip Project" is building from your actual new files; "Build APK"
   ignores zip uploads entirely as of this version, so there shouldn't be a
   stale one to confuse with anymore, but if you ever see both, "Unzip
   Project" is the one with your changes.
3. Once it finishes (green check, a few minutes), scroll to **Artifacts** at
   the bottom and download **TNL-WorkOrders-release-apk**. It's a zip
   containing `app-release.apk`.
4. Transfer that APK to your Android phone (email it to yourself, Google
   Drive, USB, etc.) and tap it to install. Android will warn about
   "installing from unknown sources" the first time - that's expected for an
   app not published on the Play Store; allow it for that install.

You can also trigger a build manually anytime from **Actions > Build APK >
Run workflow**.

**One-time step if you already have the app installed from before this
version:** it was previously a debug build, and this version switches to a
signed release build under a different signing key, so Android will refuse
to install the new one over the old one ("app not installed - conflicts
with an existing package"). Uninstall the old app once, then install this
release APK. Every release build from here on reuses the same signing key
(`keystore/release.keystore`, committed in this repo), so future updates
will install right over each other with no more uninstalling.

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
   outlined and labeled "not found" so you can type it in by hand. If this
   job is under a different company than the default, pick it from the
   **Company** dropdown at the top first.
3. **Generate PDF** - creates the one-page PDF and saves it to **History**.
   Then **View PDF** to read it in the app, **Share PDF** to hand it to
   another app (text/email/print), or open it later from **History** and use
   **Save to Downloads** there.

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
- Generated PDFs are stored in the app's own private storage, so History
  always has something to open - they're not affected by Android clearing
  cached data under low storage. They (and History) are still cleared if you
  uninstall the app or manually clear its storage in Android's App info
  screen, same as any app's data. Use **Save to Downloads** on a work order
  you want to keep somewhere outside the app entirely.
- **This update resets History once.** Adding multi-company support required
  a small database change, and the simplest safe way to apply it is to clear
  old History rows on upgrade rather than migrate them - after installing
  this version, History starts empty again, but every 6-vendor parsing rule,
  your WO# counter/prefix, and all vendor customizations are untouched.
- No storage permission is needed for OCR, generating, viewing, or sharing
  PDFs - those all use the app's own private storage plus Android's
  FileProvider/share-sheet mechanisms, which don't need it. The one time it
  matters is **Save to Downloads** on Android 8-9 (Pie and below); the app
  asks for it right when you tap that button. Android 10+ saves to Downloads
  through the OS's MediaStore API instead, which needs no permission prompt
  at all.
