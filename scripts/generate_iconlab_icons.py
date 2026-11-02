"""Build real Android launcher icon resources from the approved source WebP.

Variants 01-04 use legacy transparent PNG launchers.
Variants 05-08 use adaptive icons with transparent backdrops and different
foreground sizes. 01 matches the current final legacy image padding.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
RES = ROOT / "iconlab/src/main/res"
LEGACY_SCALES = {2: 0.90, 3: 0.98, 4: 1.06}
ADAPTIVE_SCALES = {5: 0.80, 6: 0.65, 7: 0.73, 8: 0.86}


def remove_external_matte(source):
    artwork = source.convert("RGBA").copy()
    w, h = artwork.size
    factor = min(w, h) / 128.0
    ss = 4
    inset = 4.0 * factor
    radius = 27.0 * factor
    mask = Image.new("L", (w * ss, h * ss), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle(
        (round(inset * ss), round(inset * ss),
         round((w - 1 - inset) * ss), round((h - 1 - inset) * ss)),
        radius=round(radius * ss), fill=255)
    mask = mask.resize((w, h), Image.Resampling.LANCZOS)
    mask = mask.filter(ImageFilter.GaussianBlur(0.4 * factor))
    artwork.putalpha(ImageChops.darker(artwork.getchannel("A"), mask))
    return artwork


def fit_canvas(artwork, size, scale):
    side = max(1, round(size * scale))
    resized = artwork.resize((side, side), Image.Resampling.LANCZOS)
    image = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    offset = (size - side) // 2
    image.paste(resized, (offset, offset))
    return image


def render_variants(source, output_dir):
    icon_dir = output_dir / "mipmap-xxxhdpi"
    adaptive_dir = output_dir / "mipmap-anydpi-v26"
    drawable_dir = output_dir / "drawable-nodpi"
    for directory in (icon_dir, adaptive_dir, drawable_dir):
        directory.mkdir(parents=True, exist_ok=True)

    clean = remove_external_matte(source)

    # Exact padding of scripts/generate_launcher_icon.py: 16 around 128.
    padded = Image.new("RGBA", (clean.width + 32, clean.height + 32), (0, 0, 0, 0))
    padded.paste(clean, (16, 16))
    padded.resize((192, 192), Image.Resampling.LANCZOS).save(
        icon_dir / "lab_icon_01.png", optimize=True)

    for index, scale in LEGACY_SCALES.items():
        fit_canvas(clean, 192, scale).save(
            icon_dir / f"lab_icon_{index:02d}.png", optimize=True)

    for index, scale in ADAPTIVE_SCALES.items():
        # Non-adaptive fallback, for completeness on unusual launchers.
        fit_canvas(clean, 192, scale).save(
            icon_dir / f"lab_icon_{index:02d}.png", optimize=True)
        fit_canvas(clean, 432, scale).save(
            drawable_dir / f"lab_foreground_{index:02d}.png", optimize=True)
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '  <background android:drawable="@android:color/transparent" />\n'
            f'  <foreground android:drawable="@drawable/lab_foreground_{index:02d}" />\n'
            '</adaptive-icon>\n'
        )
        (adaptive_dir / f"lab_icon_{index:02d}.xml").write_text(xml, encoding="utf-8")
    return clean


if __name__ == "__main__":
    with Image.open(SOURCE) as source:
        clean = render_variants(source, RES)
    assert clean.getpixel((0, 0))[3] == 0
    assert clean.getpixel((clean.width // 2, clean.height // 2))[3] > 230
    print("Generated 8 test launcher icons (4 legacy + 4 adaptive)")
