"""Regression tests for size, strip removal, and launcher resources."""
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_iconlab_icons import (
    APPROVED_SCALE, CLEANUP_STYLES, OUTPUT_SIZE,
    apply_cleanup, fit_canvas_subpixel, remove_external_matte, render_variants,
)

ROOT = Path(__file__).resolve().parents[1]
LABELS = "ABCDEF"


class IconLabCleanupTests(unittest.TestCase):
    def test_six_cleanup_styles_keep_exact_04b_scale(self):
        self.assertEqual(list(CLEANUP_STYLES), list(LABELS))
        self.assertAlmostEqual(APPROVED_SCALE, 1.06 * 0.9975)
        self.assertEqual(OUTPUT_SIZE, 192)

    def test_clean_a_matches_original_04b_pixel_for_pixel(self):
        source = Image.new("RGBA", (128, 128), (80, 35, 20, 255))
        with tempfile.TemporaryDirectory() as tmp:
            clean = remove_external_matte(source)
            old_04b = fit_canvas_subpixel(clean, 192, 1.06 * (1 - 0.0025))
            render_variants(source, Path(tmp))
            with Image.open(Path(tmp) / "mipmap-xxxhdpi/lab_clean_a.png") as actual:
                self.assertEqual(actual.tobytes(), old_04b.tobytes())

    def test_bottom_variants_remove_increasing_bottom_fringe_without_changing_center(self):
        source = Image.new("RGBA", (128, 128), (55, 55, 55, 255))
        clean = remove_external_matte(source)
        versions = [apply_cleanup(clean, *CLEANUP_STYLES[k]) for k in LABELS]
        # The center's RGB and opacity must remain unaltered.
        self.assertTrue(all(version.getpixel((64, 64)) == clean.getpixel((64, 64))
                            for version in versions))
        self.assertEqual(versions[0].tobytes(), clean.tobytes())
        # A deliberately grey exterior strip at y=119..123 must fade under cleanup.
        for y in (118, 120, 122):
            values = [version.getpixel((64, y))[3] for version in versions]
            self.assertGreaterEqual(values[0], values[-1])
        self.assertLess(versions[-1].getpixel((64, 120))[3],
                        versions[0].getpixel((64, 120))[3])

    def test_all_six_icons_render_as_transparent_192px_legacy_pngs(self):
        source = Image.new("RGBA", (128, 128), (60, 60, 60, 255))
        with tempfile.TemporaryDirectory() as tmp:
            res = Path(tmp)
            render_variants(source, res)
            icons = sorted((res / "mipmap-xxxhdpi").glob("lab_clean_*.png"))
            self.assertEqual([p.stem for p in icons],
                             [f"lab_clean_{key.lower()}" for key in LABELS])
            fingerprints = []
            for path in icons:
                with Image.open(path) as image:
                    self.assertEqual(image.size, (192, 192))
                    self.assertEqual(image.getpixel((0, 0))[3], 0)
                    self.assertEqual(image.getpixel((191, 191))[3], 0)
                    self.assertGreater(image.getpixel((96, 96))[3], 230)
                    fingerprints.append(image.tobytes())
            self.assertEqual(len(set(fingerprints)), 6)
            self.assertFalse((res / "mipmap-nodpi/lab_clean_a.png").exists())

    def test_cleanup_changes_only_alpha_not_artwork_rgb(self):
        source = Image.new("RGBA", (128, 128), (110, 30, 50, 255))
        clean = remove_external_matte(source)
        for style in CLEANUP_STYLES.values():
            changed = apply_cleanup(clean, *style)
            self.assertEqual(changed.convert("RGB").tobytes(),
                             clean.convert("RGB").tobytes())
            self.assertLessEqual(sum(changed.getchannel("A").getdata()),
                                 sum(clean.getchannel("A").getdata()))

    def test_manifest_contains_six_disabled_real_launcher_aliases(self):
        root = ET.parse(ROOT / "iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        aliases = root.find("application").findall("activity-alias")
        self.assertEqual(len(aliases), 6)
        for alias, key in zip(aliases, LABELS):
            self.assertEqual(alias.attrib[ns + "name"], f".LabClean{key}")
            self.assertEqual(alias.attrib[ns + "label"], f"Clean {key}")
            self.assertEqual(alias.attrib[ns + "enabled"], "false")
            self.assertEqual(alias.attrib[ns + "icon"],
                             f"@mipmap/lab_clean_{key.lower()}")
            self.assertIsNotNone(alias.find("intent-filter"))

    def test_regeneration_removes_stale_icons(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp)
            old = base / "mipmap-xxxhdpi/lab_icon_04b.png"
            old.parent.mkdir(parents=True, exist_ok=True)
            old.write_bytes(b"obsolete")
            render_variants(Image.new("RGBA", (128, 128), "black"), base)
            self.assertFalse(old.exists())


if __name__ == "__main__":
    unittest.main()
