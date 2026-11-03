"""SSH Button Next-style two-layer adaptive icons for the approved master art.

SSH Button Next uses: 720px WebP artwork + a 9dp InsetDrawable foreground
+ a separate background Drawable + an adaptive icon in mipmap-anydpi-v26.
Our approved icon MUST retain transparent corners, so its separate
background layer is fully transparent (unlike SSH Next's opaque blue art).
Do not add a dark/white square or modify the actual approved bitmap.
A particular OEM launcher may still force its own fill behind transparent
adaptive icons; this is not controllable by an APK.
"""
from pathlib import Path
from PIL import Image

SIZE = 720
INSET_DP = 9
SCALES = tuple(round((73 + i) / 100, 2) for i in range(8))


def centered_art(source, size, scale):
    side = round(size * scale)
    scaled = source.convert("RGBA").resize((side, side), Image.Resampling.LANCZOS)
    dst = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dst.paste(scaled, ((size - side) // 2, (size - side) // 2))
    return dst


def render_adaptive(source, res):
    drawable = res / "drawable-nodpi"
    xml_drawable = res / "drawable"
    xml_mipmap = res / "mipmap-anydpi-v26"
    for d in (drawable, xml_drawable, xml_mipmap):
        d.mkdir(parents=True, exist_ok=True)

    # Remove every old IconLab adaptive resource to avoid stale resources
    # from previous local builds. Never touch unrelated app resources.
    for directory, pattern in (
        (drawable, "lab_adaptive_foreground_*.png"),
        (drawable, "lab_background_art.*"),
        (drawable, "lab_foreground_art_*.webp"),
        (xml_drawable, "lab_foreground_*.xml"),
        (xml_mipmap, "lab_size_*.xml"),
    ):
        for stale in directory.glob(pattern):
            stale.unlink()

    # Background must be a drawable, as in SSH Button Next; however this
    # icon differs from SSH because its master has intentionally clear corners.
    empty_background = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    empty_background.save(
        drawable / "lab_background_art.webp",
        format="WEBP", lossless=True, method=6)

    for i, scale in enumerate(SCALES, start=1):
        number = f"{i:02d}"
        foreground = centered_art(source, SIZE, scale)
        foreground.save(
            drawable / f"lab_foreground_art_{number}.webp",
            format="WEBP", lossless=True, method=6)

        # Same InsetDrawable type and 9dp inset as SSH Button Next:
        # app/src/main/res/drawable/ic_launcher_foreground.xml
        (xml_drawable / f"lab_foreground_{number}.xml").write_text(
            '<inset xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:drawable="@drawable/lab_foreground_art_{number}"\n'
            f'    android:insetLeft="{INSET_DP}dp"\n'
            f'    android:insetTop="{INSET_DP}dp"\n'
            f'    android:insetRight="{INSET_DP}dp"\n'
            f'    android:insetBottom="{INSET_DP}dp" />\n',
            encoding="utf-8")

        # Same two-layer adaptive structure as SSH Button Next:
        # app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
        (xml_mipmap / f"lab_size_{number}.xml").write_text(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/lab_background_art" />\n'
            f'    <foreground android:drawable="@drawable/lab_foreground_{number}" />\n'
            '</adaptive-icon>\n',
            encoding="utf-8")
