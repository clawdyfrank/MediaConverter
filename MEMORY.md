# 🧠 MEMORY.md - Long-Term Memory

## Tools & Services
### GitHub
- **Account:** clawdyfrank
- **Authentication:** Personal Access Token (PAT) configured via `gh auth login`.
- **Note:** Do not print full PAT in logs. If authentication fails, ask the user to provide the PAT again.

## Projects
### MediaConverter (Android)
- **Repo:** https://github.com/clawdyfrank/MediaConverter
- **Tech:** Kotlin, Jetpack Compose, FFmpeg (ffmpeg-kit-lts-min-16kb).
- **Features:** Video to audio extraction (MP3, M4A, WAV), bitrate selection, real-time progress bar.
- **Output:** Saves to `Music/Convert` folder on the device.

### Monthly Invoicing (Google Sheets)
- **File:** `Invoice - 26` (ID: `1byyG6am58wYqX278-Nicdz7MV6MVZR7uYZIFFHvEJP4`)
- **Workflow:** Duplicate the previous month's sheet (named by month number, e.g., '01', '02'). Always copy from the most recent month's sheet (e.g., to create February '02', duplicate January '01').
- **Formatting:**
  - **Invoice #:** Format as `'00yymm` (e.g., `'002601`). The leading `'` is required for text formatting.
  - **Invoice Date:** First day of the month *following* the invoice period (e.g., February 1st for January's invoice).
  - **Description:** Update month/year in the description text.
  - **Amount:** Update the **line item amount** (e.g., cell G16). The **Total** (e.g., cell G19) should be a sum of the line items.
- **Auth:** Uses `google_tokens.json` in the workspace root.
