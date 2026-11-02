# Icon Lab v0.2.1 — comparison with true original Icon 04

v0.2.0 displayed white squares: generator switched the original 192px xxxhdpi
launcher PNGs to 1024px nodpi. This version restores the resource type and
resolution **exactly as used by the successful Icon 04 in v0.1.0**.

1. Uninstall the old Icon Lab. Debug-signing keys can differ across APKs.
2. Install `IconLab-v0.2.1.apk` from the separate prerelease.
3. Open **Icon Lab** and tap **Показать 9 вариантов**.
4. Compare Icon 04a .. Icon 04i. Icon 04a should match the original v0.1.0 Icon 04, including its clipped edges.

| Icon | Percentage of original Icon 04 |
|---|---:|
| Icon 04a | 100.00% — exact original |
| Icon 04b | 99.75% |
| Icon 04c | 99.50% |
| Icon 04d | 99.25% |
| Icon 04e | 99.00% |
| Icon 04f | 98.75% |
| Icon 04g | 98.50% |
| Icon 04h | 98.25% |
| Icon 04i | 98.00% |

The original WebP image and alpha mask are unchanged. New variants are
rendered at high internal resolution and downsampled to 192x192 legacy
xxxhdpi PNGs, so 0.25% adjustments remain distinct. No adaptive icon or
1024px nodpi icon is used. The platform launcher may still apply its own
normalization to individual variants; the exact-result check is on device.

The app is a separate minimal Android package without download logic, network
permissions or ffmpeg. It enables nine genuine launcher entries by one button.
