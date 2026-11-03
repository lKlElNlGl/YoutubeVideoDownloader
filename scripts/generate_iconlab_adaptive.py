"""Android 8+ adaptive launcher assets, using the already-approved icon art.

No new background and no artwork mutations. The image is only scaled
uniformly to fit the central safe region for Android launcher masks.
"""
from pathlib import Path
from PIL import Image

ADAPTIVE_SIZE = 432
ADAPTIVE_SCALES = (0.69, 0.70, 0.71, 0.72, 0.73, 0.74, 0.75, 0.76)


def adaptive_foreground(source, size, scale):
    """Render a centered copy; no recoloring, reshaping or composited matte."""
    source = source.convert("RGBA")
    side = round(size * scale)
    artwork = source.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - side) // 2
    canvas.paste(artwork, (offset, offset))
    return canvas


def render_adaptive(source, res):
    foreground_dir = res / "drawable-nodpi"
    xml_dir = res / "mipmap-anydpi-v26"
    foreground_dir.mkdir(parents=True, exist_ok=True)
    xml_dir.mkdir(parents=True, exist_ok=True)
    for folder, pattern in (
        (foreground_dir, "lab_adaptive_foreground_*.png"),
        (xml_dir, "lab_size_*.xml"),
    ):
        for stale in folder.glob(pattern):
            stale.unlink()
    for i, scale in enumerate(ADAPTIVE_SCALES, start=1):
        name = f"{i:02d}"
        foreground = adaptive_foreground(source, ADAPTIVE_SIZE, scale)
        foreground.save(foreground_dir / f"lab_adaptive_foreground_{name}.png",
                        optimize=True)
        (xml_dir / f"lab_size_{name}.xml").write_text(
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@android:color/transparent" />\n'
            f'    <foreground android:drawable="@drawable/lab_adaptive_foreground_{name}" />\n'
            '</adaptive-icon>\n',
            encoding="utf-8")
