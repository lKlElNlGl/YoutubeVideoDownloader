"""Generate eight legacy launcher PNGs from the approved YoutubeVideoDownloader art.

The asset is a display-size export (192x192, with alpha) of the approved
1254x1254 master. NEVER recolor, crop, retouch or regenerate artwork.
Only uniform size changes around the canvas center are allowed.
"""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "iconlab/assets/approved_master_display.webp"
RES = ROOT / "iconlab/src/main/res"
OUTPUT_SIZE = 192
SUPERSAMPLE = 8
VARIANTS = tuple((f"{i:02d}", (107 + i) / 100.0)
                 for i in range(1, 9))


def render_icon(source, scale, size=OUTPUT_SIZE):
    """Scale x/y uniformly and center the art, never modifying RGBA design."""
    if not 1.0 <= scale <= 1.2:
        raise ValueError("unsafe launcher scale")
    source = source.convert("RGBA")
    canvas_size = size * SUPERSAMPLE
    side = round(canvas_size * scale)
    resized = source.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    offset = (canvas_size - side) // 2
    canvas.paste(resized, (offset, offset))
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def render_variants(source, dest=RES):
    folder = dest / "mipmap-xxxhdpi"
    folder.mkdir(parents=True, exist_ok=True)
    for pattern in ("lab_icon_*.png", "lab_clean_*.png",
                    "lab_top_*.png", "lab_size_*.png"):
        for stale in folder.glob(pattern):
            stale.unlink()
    for name, scale in VARIANTS:
        image = render_icon(source, scale)
        image.save(folder / f"lab_size_{name}.png", optimize=True)


if __name__ == "__main__":
    if not SOURCE.is_file():
        raise SystemExit("Approved master test asset missing; refuse old artwork fallback")
    with Image.open(SOURCE) as source:
        if source.size != (192, 192) or source.mode != "RGBA":
            raise SystemExit("Approved master asset must be RGBA 192x192")
        render_variants(source)
    print("Generated 8 legacy xxxhdpi icons, approved art unchanged; scales 108-115%.")
