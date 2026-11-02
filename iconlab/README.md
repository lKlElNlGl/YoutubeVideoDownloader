# Icon Lab — tiny Android test application

Independent Android package `com.lklelnlgl.iconlab` (no yt-dlp, ffmpeg, network, storage or download permissions).

1. Install IconLab-v0.1.0.apk from release `iconlab-v0.1.0`.
2. Launch **Icon Lab** and press **Показать 8 вариантов** once.
3. Open the launcher **app drawer**. Eight actual launcher activities **Icon 01** — **Icon 08** appear.
4. Drag them to a free home-screen page. Compare their visible edges and scale, then report the winning number.

01 = current padded legacy baseline.
02 = transparent legacy 90%.
03 = transparent legacy 98%.
04 = transparent legacy 106%.
05 = adaptive transparent foreground 80% (close to baseline).
06 = adaptive transparent foreground 65%.
07 = adaptive transparent foreground 73%.
08 = adaptive transparent foreground 86%.

### Why not put the icons automatically on the last page?

Android does not grant apps permission to choose a home-screen page or add arbitrary launcher entries without launcher control. Activity aliases provide **real app icons** in the app drawer; manual drag ensures a faithful launcher comparison. Pinned shortcuts are unsuitable because they typically require a separate consent dialog for each shortcut and may use a shortcut badge.

The approved artwork comes directly from `app/src/main/res/drawable-nodpi/ic_launcher_reference.webp`. The generator removes the exterior matte before scaling. No main app changes or large download dependencies are required. APK is debug-signed and intentionally disposable. On a future rebuild, uninstall the old test app first if the temporary signing identity differs.
