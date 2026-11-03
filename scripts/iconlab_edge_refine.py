"""Smooth a proven legacy launcher silhouette without damaging approved artwork.

The working old Clean C2 alpha identifies a silhouette that Realme 8i
renders without an injected white square. Copying it verbatim (v0.5.8)
exposed pixels beyond the new artwork's original coverage, so translucent
garbage/uninitialised RGB appeared at its contour.

This module reconstructs the contour at 8x scale, downscales it with
antialiasing, and extends ONLY the new artwork's opaque bezel colors
into newly exposed edge pixels. RGB in the solid artwork is unchanged.
"""
from collections import deque
from PIL import Image, ImageFilter


def smooth_proven_mask(reference_alpha, supersample=8, feather_px=0.50):
    if reference_alpha.mode != "L":
        raise ValueError("reference alpha must be L")
    if supersample < 8:
        raise ValueError("at least 8x supersampling is required")

    w, h = reference_alpha.size
    # Shape from verified old icon; the threshold removes source alpha specks.
    contour = reference_alpha.point(lambda value: 255 if value >= 128 else 0)
    enlarged = contour.resize((w * supersample, h * supersample),
                              Image.Resampling.BICUBIC)
    enlarged = enlarged.filter(
        ImageFilter.GaussianBlur(feather_px * supersample))
    return enlarged.resize((w, h), Image.Resampling.LANCZOS)


def trim_top_and_shoulders(alpha, supersample=8, inset_px=1.0,
                           feather_px=0.40, blend_start=0.39,
                           blend_end=0.67):
    """Gently tighten the upper edge and upper side contours only.

    Reconstruct the existing silhouette at 8x resolution; erode the
    contour by ~1px with a soft antialiased edge. The correction has full
    effect near the top, fades smoothly through the middle, and becomes
    EXACTLY zero in the bottom third. Never add opacity at any pixel.

    This operation applies ONLY to the generated PNG alpha, not artwork.
    """
    if alpha.mode != "L":
        raise ValueError("alpha mask must be L")
    if supersample < 8:
        raise ValueError("at least 8x supersampling is required")
    if not 0 < inset_px <= 2.0:
        raise ValueError("edge inset must be positive and subtle")
    if not 0.0 < blend_start < blend_end < 1.0:
        raise ValueError("invalid vertical blend limits")

    w, h = alpha.size
    highres = alpha.point(lambda a: 255 if a >= 128 else 0).resize(
        (w * supersample, h * supersample), Image.Resampling.NEAREST)
    inset_subpixels = round(inset_px * supersample)
    eroded = highres.filter(
        ImageFilter.MinFilter(2 * inset_subpixels + 1))
    eroded = eroded.filter(
        ImageFilter.GaussianBlur(feather_px * supersample))
    candidate = eroded.resize((w, h), Image.Resampling.LANCZOS)

    original_values = list(alpha.getdata())
    candidate_values = list(candidate.getdata())
    result_values = []
    start = blend_start * h
    end = blend_end * h
    for index, (before, suggested) in enumerate(
            zip(original_values, candidate_values)):
        y = index // w
        if y <= start:
            strength = 1.0
        elif y >= end:
            strength = 0.0
        else:
            fraction = (y - start) / (end - start)
            strength = 1.0 - fraction * fraction * (3.0 - 2.0 * fraction)

        # Never expand the old working silhouette; merely shave its edge.
        target = min(before, suggested)
        result_values.append(round(before - (before - target) * strength))

    output = Image.new("L", alpha.size)
    output.putdata(result_values)
    return output


def extend_bezel_colors(image, trusted_alpha=200):
    """Fill fringe RGB from nearest solid pixel, without touching solid art.

    Multi-source four-neighbour BFS is deterministic and runs once for a
    tiny 192x192 PNG. It only changes colors at pixels whose source alpha
    is below trusted_alpha. Pixels outside the new image's silhouette
    therefore get edge colors instead of undefined transparent RGB.
    """
    original = image.convert("RGBA")
    w, h = original.size
    source = list(original.getdata())
    output = source.copy()
    visited = bytearray(w * h)
    queue = deque()
    for i, (_, _, _, alpha) in enumerate(source):
        if alpha >= trusted_alpha:
            visited[i] = 1
            queue.append(i)
    if not queue:
        raise ValueError("no opaque artwork available for edge extension")

    while queue:
        p = queue.popleft()
        x, y = p % w, p // w
        for n in (p - 1 if x else -1,
                  p + 1 if x < w - 1 else -1,
                  p - w if y else -1,
                  p + w if y < h - 1 else -1):
            if n < 0 or visited[n]:
                continue
            visited[n] = 1
            # Inherit RGB from nearest fully visible neighbour only.
            r, g, b, _ = output[p]
            output[n] = (r, g, b, source[n][3])
            queue.append(n)

    cleaned = Image.new("RGBA", original.size)
    cleaned.putdata(output)
    return cleaned


def render_smooth_legacy_icon(new_icon, proven_old_icon):
    if new_icon.size != proven_old_icon.size or new_icon.size != (192, 192):
        raise ValueError("both launcher icons must be 192x192")
    cleaned = extend_bezel_colors(new_icon)
    mask = smooth_proven_mask(
        proven_old_icon.convert("RGBA").getchannel("A"), supersample=8)
    # Match the annotated upper/side crop, with softened ~1px inward edge.
    # Preserve the entire lower third and all solid artwork RGB exactly.
    cleaned.putalpha(trim_top_and_shoulders(mask, supersample=8))
    return cleaned
