"""Icon Lab v0.4.0: Clean C2 baseline + seven upper black rim alternatives.

01 unchanged Clean C2; 02-04 blacken gray top line without size changes;
05-08 repair and extend black upper cap by 0.5-2px (128px source units).
Red, white, lower part and lateral edges stay where they were.
"""
import math
from pathlib import Path
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
RES = ROOT / "iconlab/src/main/res"
OUTPUT_SIZE = 192
SUPER_SAMPLE = 8
APPROVED_SCALE = 1.06 * (1 - 0.0025)

VARIANTS = (
    ("01",0.0,0.0), ("02",.35,0.0), ("03",.65,0.0), ("04",.95,0.0),
    ("05",.95,.5), ("06",.95,1.0), ("07",.95,1.5), ("08",.95,2.0),
)


def rounded_alpha_mask(width, height, bottom_lift=0, extra_radius=0, top_lift=0):
    ss = 4
    factor = min(width, height) / 128.0
    inset = 4.0 * factor
    radius = (27.0 + extra_radius) * factor
    mask = Image.new("L", (width * ss, height * ss), 0)
    drawer = ImageDraw.Draw(mask)
    drawer.rounded_rectangle(
        (round(inset * ss), round((inset + top_lift * factor) * ss),
         round((width - 1 - inset) * ss),
         round((height - 1 - inset - bottom_lift * factor) * ss)),
        radius=round(radius * ss), fill=255)
    return mask.resize((width, height), Image.Resampling.LANCZOS).filter(
        ImageFilter.GaussianBlur(0.4 * factor))


def remove_external_matte(source):
    artwork = source.convert("RGBA").copy()
    width, height = artwork.size
    alpha = ImageChops.darker(
        artwork.getchannel("A"), rounded_alpha_mask(width, height))
    artwork.putalpha(alpha)
    return artwork


def apply_cleanup(clean, bottom_lift, alpha_cutoff, extra_radius):
    if bottom_lift == 0 and alpha_cutoff == 0 and extra_radius == 0:
        return clean.copy()

    result = clean.copy()
    width, height = result.size
    contour = rounded_alpha_mask(width, height, bottom_lift, extra_radius)
    alpha = ImageChops.darker(clean.getchannel("A"), contour)

    if alpha_cutoff:
        # Remove only faint semi-transparent pixels. Opaque parts stay intact.
        alpha = alpha.point(lambda value: 0 if value <= alpha_cutoff else value)

    result.putalpha(alpha)
    return result



def neutralize_upper_left_bezel(artwork):
    """Remove warm COLOR CAST, not brightness, from the upper-left black bezel.

    Bounded to the top rim and left shoulder. The vivid inner red button
    and white symbol are not in this mask; reflection luminance is preserved.
    """
    result = artwork.copy()
    pixels = result.load()
    w, h = result.size
    for y in range(round(h * 0.35)):
        for x in range(round(w * 0.62)):
            if not (y <= h * 0.18 or (x <= w * 0.17 and y <= h * 0.32)):
                continue
            r, g, b, a = pixels[x, y]
            # Preserve vivid red artwork; desaturate only a muted warm reflection.
            if a == 0 or r <= g + 5 or r <= b + 5 or (
                    r > 170 and r - min(g, b) > 85):
                continue
            luma = round(0.2126 * r + 0.7152 * g + 0.0722 * b)
            pixels[x, y] = (luma, luma, luma, a)
    return result


def apply_top_cleanup(reference, top_lift, corner_radius):
    """Trim upper alpha only. The approved bottom and inner artwork are fixed."""
    result = reference.copy()
    width, height = reference.size
    upper_mask = rounded_alpha_mask(
        width, height, top_lift=top_lift, extra_radius=corner_radius)
    old_alpha = reference.getchannel("A")
    result_alpha = old_alpha.copy()
    top_end = round(height * 0.34)
    for y in range(top_end):
        for x in range(width):
            a = old_alpha.getpixel((x, y))
            candidate = upper_mask.getpixel((x, y))
            if candidate < a:
                result_alpha.putpixel((x, y), candidate)
    result.putalpha(result_alpha)
    return result


def clean_c2_reference(source):
    # EXACT old Clean C2: never alter this transformation order.
    c = apply_cleanup(remove_external_matte(source), 2, 16, 0)
    return apply_top_cleanup(neutralize_upper_left_bezel(c), 1.0, 0)


def is_red(r, g, b):
    return r >= 100 and r > g * 1.4 and r > b * 1.3


def repair_upper_rim(art, strength):
    """Recolor only the exterior upper gray line; preserve alpha and gloss."""
    if not strength:
        return art.copy()
    out = art.copy()
    pixels, dst = art.load(), out.load()
    w, h = art.size
    reach = max(1, round(w * 3.5 / 128))
    for x in range(round(w * .1), round(w * .9)):
        edge = next((y for y in range(round(h * .19))
                     if pixels[x, y][3] >= 64), None)
        if edge is None:
            continue
        for y in range(max(0, edge-1), min(h, edge+reach)):
            r, g, b, alpha = pixels[x,y]
            if alpha == 0 or is_red(r,g,b) or min(r,g,b)>215:
                continue
            sr,sg,sb,_ = pixels[x,min(h-1,edge+reach+2)]
            if is_red(sr,sg,sb) or min(sr,sg,sb)>215:
                continue
            target = tuple(min(v,s,46) for v,s in zip((r,g,b),(sr,sg,sb)))
            d = (y-edge+1)/(reach+1)
            weight = strength*max(0,1-d*d)
            dst[x,y] = (*[round(v*(1-weight)+t*weight)
                          for v,t in zip((r,g,b),target)],alpha)
    return out


def extend_upper_black(art, growth):
    """Extend ONLY upper black cap; red, white, sides, lower edge are fixed."""
    if growth <= 0:
        return art.copy()
    out = art.copy()
    inp, dst = art.load(), out.load()
    w,h=art.size
    factor=min(w,h)/128
    end=round(h*21/128)
    taper_start=round(h*8/128)
    for y in range(min(h-2,end)+1):
        if y<=taper_start:
            shift=growth*factor
        else:
            t=min(1,(y-taper_start)/max(1,end-taper_start))
            shift=growth*factor*(1-t*t*(3-2*t))
        sy=y+shift
        lo=min(h-1,math.floor(sy))
        hi=min(h-1,lo+1)
        frac=sy-lo
        for x in range(round(w*.09),round(w*.91)):
            r,g,b,_=inp[x,y]
            p0,p1=inp[x,lo],inp[x,hi]
            if (is_red(r,g,b) or is_red(*p0[:3]) or
                    is_red(*p1[:3]) or min(r,g,b)>215):
                continue
            a0,a1=p0[3]/255,p1[3]/255
            alpha=a0*(1-frac)+a1*frac
            if alpha<=0:
                dst[x,y]=(0,0,0,0)
                continue
            rgb=[round((p0[c]*a0*(1-frac)+p1[c]*a1*frac)/alpha)
                 for c in range(3)]
            dst[x,y]=(*rgb,round(255*alpha))
    return out


def smooth_canvas_top_if_crowded(icon):
    """Prevent a hard-cut straight top when a growing rim reaches 192px edge.

    Preserve exact black color/shape and all interior pixels. The only
    correction is 3-pixel anti-aliasing at the canvas boundary itself.
    This is NOT a top contour trim, scale change, or red-panel translation.
    """
    alpha = icon.getchannel("A")
    w, h = icon.size
    if max(alpha.crop((0, 0, w, 1)).get_flattened_data()) < 40:
        return icon
    alpha = alpha.copy()
    for y, cap in enumerate((24, 170, 240)):
        for x in range(w):
            old = alpha.getpixel((x, y))
            if old > cap:
                alpha.putpixel((x, y), cap)
    result = icon.copy()
    result.putalpha(alpha)
    return result


def fit_canvas_subpixel(artwork, size, scale):
    """Exact v0.2.1 rendering for reference 04b, including subpixel centering."""
    big = size * SUPER_SAMPLE
    side = max(1, round(big * scale))
    resized = artwork.resize((side, side), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    offset = (big - side) // 2
    canvas.paste(resized, (offset, offset))
    return canvas.resize((size, size), Image.Resampling.LANCZOS)


def render_variants(source, output_dir):
    icon_dir = output_dir / "mipmap-xxxhdpi"
    icon_dir.mkdir(parents=True, exist_ok=True)
    # Remove old generated resources; never leave stale launcher aliases/images.
    for location, glob in (
        ("mipmap-xxxhdpi", "lab_icon_*.png"),
        ("mipmap-xxxhdpi", "lab_clean_*.png"),
        ("mipmap-xxxhdpi", "lab_top_*.png"),
        ("mipmap-nodpi", "lab_icon_*.png"),
        ("mipmap-anydpi-v26", "lab_icon_*.xml"),
        ("drawable-nodpi", "lab_foreground_*.png"),
    ):
        directory = output_dir / location
        if directory.exists():
            for path in directory.glob(glob):
                path.unlink()

    base=clean_c2_reference(source)
    for label,strength,growth in VARIANTS:
        repaired=repair_upper_rim(base,strength)
        sample=extend_upper_black(repaired,growth)
        icon = fit_canvas_subpixel(sample,OUTPUT_SIZE,APPROVED_SCALE)
        if growth > 0:
            icon = smooth_canvas_top_if_crowded(icon)
        icon.save(icon_dir / f"lab_top_{label}.png",optimize=True)
    return base


if __name__ == "__main__":
    with Image.open(SOURCE) as source:
        clean = render_variants(source, RES)
    assert clean.getpixel((0, 0))[3] == 0
    assert clean.getpixel((clean.width // 2, clean.height // 2))[3] > 230
    print("Generated 8 top black-rim alternatives; 01 exact Clean C2, 05–08 grow upper cap.")
