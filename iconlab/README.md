# Icon Lab v0.3.2 — clean upper contour, neutral reflection

## Approved immutable reference

**Clean C** in first position is the exact same generated bitmap as Clean C in
v0.3.0. It is our fixed reference: the user chose its scale, black button,
white symbols and bottom-edge treatment. The original warm-tinted reflection
also remains unchanged on the reference so comparisons are fair.

| Icon | Difference relative to Clean C |
|---|---|
| Clean C | Identical unmodified reference (warm highlight included) |
| Clean C1 | Neutral-color (gray) upper-left glossy reflection; minimal 0.5 px upper exterior trim |
| Clean C2 | Same neutral highlight; stronger 1.0 px upper exterior trim |
| Clean C3 | Same neutral highlight; 1.0 px top trim plus 2 px upper corner radius adjustment |

All use original **Icon 04b scale**, 192×192 legacy `mipmap-xxxhdpi`
resource, transparent outside, and exactly the same lower edge. No modifications
to the red Play button, white arrow, black body volume or position.

Only alpha pixels near the *upper* exterior contour are cut; reflection color is
neutralized without changing its luminance. The approved glossy reflection is
not erased.

## Test

1. Uninstall earlier Icon Lab to avoid debug-signature incompatibility.
2. Install `IconLab-v0.3.2.apk`, launch it, tap `Показать 4 варианта`.
3. Compare Clean C, Clean C1, Clean C2, Clean C3 in the app drawer.
4. Choose the least reddish icon with a clean top edge and intact button.

Launcher-specific edge treatment must still be checked visually on the
user's phone. This is a disposable standalone tester without downloader dependencies.
