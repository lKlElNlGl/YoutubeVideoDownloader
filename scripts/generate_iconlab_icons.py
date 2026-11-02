"""Generate nine ultra-close true launcher icons based on the approved Icon 04.

Icon 04 was the original transparent legacy icon at scale 1.06. All variants
use the SAME approved original WebP with the same matte removal algorithm.
04a = 100% of the Icon 04 scale; 04b..04i decrease by 0.25% each.

Render at 1024 px with nodpi resources, to avoid rounding the fine changes
away at the old 192 px output size. Never rescale a previously clipped icon.
"""
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
RES = ROOT / "iconlab/src/main/res"
BASE_SCALE = 1.06
STEP = 0.0025
COUNT = 9
OUTPUT_SIZE = 1024


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


def variant_scales():
    return {
        f"04{chr(ord('a') + index)}": BASE_SCALE * (1 - index * STEP)
        for index in range(COUNT)
    }


def render_variants(source, output_dir):
    icon_dir = output_dir / "mipmap-nodpi"
    icon_dir.mkdir(parents=True, exist_ok=True)

    # Prevent stale v0.1 icons when building in a reused working tree.
    for stale_dir in ("mipmap-xxxhdpi", "mipmap-anydpi-v26", "drawable-nodpi"):
        directory = output_dir / stale_dir
        if directory.exists():
            for pattern in ("lab_icon_*.png", "lab_icon_*.xml", "lab_foreground_*.png"):
                for old in directory.glob(pattern):
                    old.unlink()

    clean = remove_external_matte(source)
    for name, scale in variant_scales().items():
        icon = fit_canvas(clean, OUTPUT_SIZE, scale)
        icon.save(icon_dir / f"lab_icon_{name}.png", optimize=True)
    return clean


if __name__ == "__main__":
    with Image.open(SOURCE) as source:
        clean = render_variants(source, RES)
    assert clean.getpixel((0, 0))[3] == 0
    assert clean.getpixel((clean.width // 2, clean.height // 2))[3] > 230
    print("Generated 9 Icon 04 variants: 04a (100%) through 04i (98%), step 0.25%")
