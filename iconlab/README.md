# Icon Lab v0.3.0 — clean-grey-strip comparison

**Purpose:** choose a grey-strip-free Android launcher icon using the EXACT
scale of Icon 04b from Icon Lab v0.2.1, without changing the app's artwork.

## Test specification

- One button, `Показать 6 вариантов`, enables six real launcher icons.
- All variants use the same 192px PNG in `mipmap-xxxhdpi`, the same
  scale `1.06 * 0.9975`, the same position, and transparent corners.
- No adaptive icon, white background, red-button edits or size changes.
- Only the transparency mask along the lower edge/corners and the faint
  exterior alpha pixels changes.

| Icon | Change to baseline Icon 04b |
|---|---|
| Clean A | Exact Icon 04b, unmodified reference |
| Clean B | Minimal trim of the lower transparent/grey fringe |
| Clean C | Minimal lower trim plus faint outer halo removal |
| Clean D | Stronger lower fringe trim |
| Clean E | Stronger bottom trim with slightly tighter rounded corners |
| Clean F | Maximum conservative trim, corner and faint-halo cleanup |

## How to check

1. **Uninstall the previous Icon Lab** first (debug signing may differ).
2. Install `IconLab-v0.3.0.apk` from the separate prerelease.
3. Open **Icon Lab** and press **Показать 6 вариантов**.
4. Compare Clean A .. Clean F in the app drawer or on your home screen.
5. Choose the cleanest outer edge that preserves the complete black button.

Android launcher normalization can differ from source transparency, so
visual verification **on your phone** is mandatory before porting the winning
icon to the main YouTube downloader.

No yt-dlp, ffmpeg, network, cookies or downloader functionality is included.
This test package has a separate applicationId.
