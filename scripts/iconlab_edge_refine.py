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
    cleaned.putalpha(mask)
    return cleaned
