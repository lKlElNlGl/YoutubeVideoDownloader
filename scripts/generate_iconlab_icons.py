"""Decisive Realme launcher A/B test: old proven icon vs new approved art.

A = EXACT Icon Lab v0.3.2 Clean C2 from original source+generator.
B = approved art with EXACT ALPHA silhouette copied from successful A.
The new artwork is never recolored, redrawn, or saved back to source.
Both are legacy 192x192 xxxhdpi PNG and activity-alias, in the same APK.
No adaptive resources, no added background, no modifications to master art.
"""
from pathlib import Path
from PIL import Image
from tempfile import TemporaryDirectory
from shutil import copyfile

try:
    from . import iconlab_reference_v032 as old
    from . import iconlab_legacy_v055 as new
except ImportError:
    import iconlab_reference_v032 as old
    import iconlab_legacy_v055 as new

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "iconlab/src/main/res"
OLD_SOURCE = old.SOURCE
NEW_SOURCE = new.SOURCE


def prepare_new_c2_alpha(source):
    """Apply ONLY the v0.3.2 outer alpha treatment to the new artwork.

    Keep original RGB unchanged at every source pixel. Do not call the old
    neutralize_upper_left_bezel function; that function changes RGB and is
    forbidden for the approved new master. The cleanup may only REDUCE alpha.
    """
    base = new.prepare_128_source(source)
    alpha_only = old.remove_external_matte(base)
    alpha_only = old.apply_cleanup(alpha_only, bottom_lift=2,
                                   alpha_cutoff=16, extra_radius=0)
    alpha_only = old.apply_top_cleanup(alpha_only, top_lift=1.0,
                                       corner_radius=0.0)
    return alpha_only


def render_new_c2(source):
    """Use exactly the reference C2 scaling and supersampling after cleanup."""
    return old.fit_canvas_subpixel(prepare_new_c2_alpha(source),
                                   old.OUTPUT_SIZE, old.APPROVED_SCALE)



def adopt_proven_old_alpha_contour(new_icon, old_icon):
    """Give approved NEW icon the EXACT alpha silhouette of working Clean C2.

    Only the alpha channel in the generated 192x192 PNG is changed; EVERY
    new RGB component remains bit-for-bit unchanged. The approved master
    source remains immutable. Since Realme already renders the old icon
    correctly, matching the whole old alpha mask (not merely near-threshold
    edges) is more decisive than guessing a new scale or radius.
    """
    if new_icon.size != old_icon.size or new_icon.size != (192, 192):
        raise ValueError("both legacy icons must have the same 192px canvas")
    result = new_icon.convert("RGBA").copy()
    result.putalpha(old_icon.convert("RGBA").getchannel("A"))
    return result

def render_controls(res=RES):
    folder = res / "mipmap-xxxhdpi"
    folder.mkdir(parents=True, exist_ok=True)
    for pattern in ("lab_icon_*.png", "lab_clean_*.png",
                    "lab_top_*.png", "lab_size_*.png", "lab_probe_*.png"):
        for stale in folder.glob(pattern):
            stale.unlink()
    # No adaptive resources can override the two legacy icons.
    for directory, patterns in (
        ("mipmap-anydpi-v26", ["lab_*.xml"]),
        ("drawable-nodpi", ["lab_foreground_art_*.webp",
                            "lab_background_art.webp",
                            "lab_adaptive_foreground_*.png"]),
        ("drawable", ["lab_foreground_*.xml"]),
    ):
        directory_path = res / directory
        for pattern in patterns:
            if directory_path.exists():
                for stale in directory_path.glob(pattern):
                    stale.unlink()

    with TemporaryDirectory() as temp:
        temp = Path(temp)
        oldfolder = temp / "old"
        with Image.open(OLD_SOURCE) as artwork:
            old.render_variants(artwork, oldfolder)
        # Old: successful, untouched Clean C2 from v0.3.2.
        copyfile(oldfolder / "mipmap-xxxhdpi/lab_clean_c2.png",
                 folder / "lab_probe_old.png")
        # New: preserve ALL of the new artwork RGB, but use the alpha
        # silhouette of the proven old launcher icon pixel for pixel.
        with Image.open(NEW_SOURCE) as artwork, Image.open(
                folder / "lab_probe_old.png") as old_icon:
            new_icon = render_new_c2(artwork)
            adopt_proven_old_alpha_contour(new_icon, old_icon).save(
                folder / "lab_probe_new.png", optimize=True)


if __name__ == "__main__":
    render_controls()
    print("Control A = original Clean C2; control B = new artwork with exact alpha from A.")
