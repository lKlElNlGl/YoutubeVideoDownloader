"""Approved icon, rendered using the proven Icon Lab v0.2.1/04b pipeline.

The source is a 192 px display export of the approved 1254 px artwork.
Recreate the former 128 x 128 source, then use the EXACT legacy
fit_canvas_subpixel algorithm and 192 px xxxhdpi resource placement.

No artwork recoloring, cropping, background or Android adaptive mask.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "iconlab/assets/approved_master_display.webp"
RES = ROOT / "iconlab/src/main/res"
SOURCE_SIZE = 128
OUTPUT_SIZE = 192
SUPER_SAMPLE = 8
APPROVED_SCALE = 1.06 * (1 - 0.0025)  # Verified old Icon 04b
VARIANTS = (("01", APPROVED_SCALE),) + tuple(
    (f"{i:02d}", (107 + i) / 100.0) for i in range(2, 9)
)


def fit_canvas_subpixel(artwork, size, scale):
    """Pixel-level same algorithm as Icon Lab v0.2.1 and v0.3.2."""
    big = size * SUPER_SAMPLE
    side = max(1, round(big * scale))
    resized = artwork.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    offset = (big - side) // 2
    canvas.paste(resized, (offset, offset))
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def render_variants(source, output_dir=RES):
    # Same source geometry as old working Icon Lab: 128x128.
    original = source.convert("RGBA")
    base = original.resize((SOURCE_SIZE, SOURCE_SIZE), Image.Resampling.LANCZOS)
    folder = output_dir / "mipmap-xxxhdpi"
    folder.mkdir(parents=True, exist_ok=True)
    for pattern in ("lab_top_*.png", "lab_size_*.png",
                    "lab_icon_*.png", "lab_clean_*.png"):
        for stale in folder.glob(pattern):
            stale.unlink()
    for name, scale in VARIANTS:
        icon = fit_canvas_subpixel(base, OUTPUT_SIZE, scale)
        icon.save(folder / f"lab_size_{name}.png", optimize=True)
    return base


if __name__ == "__main__":
    if not SOURCE.is_file():
        raise SystemExit("Approved master display asset missing — old art fallback forbidden")
    with Image.open(SOURCE) as image:
        base = render_variants(image)
    assert base.size == (128, 128)
    assert base.getpixel((0, 0))[3] == 0
    print("Approved icon: old 128 source -> legacy 192 xxxhdpi pipeline; 8 aliases.")
