# Icon Lab v0.2.0 — tiny Android launcher icon test

Stand-alone Android package `com.lklelnlgl.iconlab`. No downloader functionality, ffmpeg, yt-dlp, network or storage permissions.

1. **Uninstall the previous Icon Lab** (v0.1.0) first; each disposable APK may use a different debug signature.
2. Install `IconLab-v0.2.0.apk`.
3. Open **Icon Lab** and tap the only button, **Показать 9 вариантов**.
4. Find **Icon 04a**, **Icon 04b**, ... **Icon 04i** in the Android app list.
5. Compare 9 actual launcher icons (5 + 4 on the five-column launcher). You can drag them to the last home-screen page manually.

All variants use the approved Icon 04 source image, with a *transparent legacy icon* like the original. The only change between variants is the scale, measured relative to Icon 04:

| Icon | Scale relative to Icon 04 | Absolute scale in the old generator |
|---|---:|---:|
| 04a | 100.00% | 1.06000 |
| 04b | 99.75% | 1.05735 |
| 04c | 99.50% | 1.05470 |
| 04d | 99.25% | 1.05205 |
| 04e | 99.00% | 1.04940 |
| 04f | 98.75% | 1.04675 |
| 04g | 98.50% | 1.04410 |
| 04h | 98.25% | 1.04145 |
| 04i | 98.00% | 1.03880 |

Output PNGs use a 1024 px canvas with `mipmap-nodpi` to preserve the quarter-percent steps; the previous 192 px generation would quantize some neighboring steps to the same pixels. Each candidate is generated directly from the original WebP (no progressive degradation or rescaling of a clipped icon). The exterior matte is transparent.

Android launchers control placement. The app enables all nine *real launcher activities* after one tap; the launcher cannot be forced to arrange icons on a particular home-screen page.

The small app remains separate from the main YoutubeVideoDownloader, and its prerelease does not change the main app latest release.
