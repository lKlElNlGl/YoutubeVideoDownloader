"""Decisive Realme launcher A/B test: old proven icon vs new approved art.

A = EXACT Icon Lab v0.3.2 Clean C2 from original source+generator.
B = EXACT Icon Lab v0.5.5 100% from approved new art+generator.
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
        newfolder = temp / "new"
        with Image.open(OLD_SOURCE) as artwork:
            old.render_variants(artwork, oldfolder)
        with Image.open(NEW_SOURCE) as artwork:
            new.render_variants(artwork, newfolder)
        # Old: successful, untouched Clean C2 from v0.3.2.
        copyfile(oldfolder / "mipmap-xxxhdpi/lab_clean_c2.png",
                 folder / "lab_probe_old.png")
        # New: 100% PNG from v0.5.5, before any launcher processing.
        copyfile(newfolder / "mipmap-xxxhdpi/lab_size_05.png",
                 folder / "lab_probe_new.png")


if __name__ == "__main__":
    render_controls()
    print("Control A = original Clean C2; control B = new approved art 100%.")
