"""Regression suite for Clean C baseline and three clean upper-edge variants."""
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_iconlab_icons import (
    TOP_STYLES, APPROVED_SCALE, OUTPUT_SIZE, apply_cleanup,
    apply_top_cleanup, fit_canvas_subpixel, neutralize_upper_left_bezel,
    remove_external_matte, render_variants,
)

ROOT = Path(__file__).resolve().parents[1]
LABELS = ["C", "C1", "C2", "C3"]


def fixture():
    img = Image.new("RGBA", (128, 128), (48, 47, 46, 255))
    img.putpixel((18, 14), (125, 70, 74, 255))
    img.putpixel((20, 12), (199, 130, 136, 255))
    img.putpixel((64, 32), (240, 30, 35, 255))
    img.putpixel((64, 64), (255, 255, 255, 255))
    return img


class IconLabTests(unittest.TestCase):
    def test_four_named_styles_at_original_04b_scale(self):
        self.assertEqual(list(TOP_STYLES), LABELS)
        self.assertEqual(OUTPUT_SIZE, 192)
        self.assertAlmostEqual(APPROVED_SCALE, 1.06 * 0.9975)

    def test_clean_c_is_pixel_identical_to_v030_reference(self):
        base = apply_cleanup(remove_external_matte(fixture()), 2, 16, 0)
        expected = fit_canvas_subpixel(base, OUTPUT_SIZE, APPROVED_SCALE)
        with tempfile.TemporaryDirectory() as td:
            render_variants(fixture(), Path(td))
            with Image.open(Path(td) / "mipmap-xxxhdpi/lab_clean_c.png") as actual:
                self.assertEqual(actual.tobytes(), expected.tobytes())

    def test_muted_and_bright_warm_reflection_become_neutral_without_dimming(self):
        base = apply_cleanup(remove_external_matte(fixture()), 2, 16, 0)
        changed = neutralize_upper_left_bezel(base)
        for pos in [(18, 14), (20, 12)]:
            original = base.getpixel(pos)
            fixed = changed.getpixel(pos)
            self.assertGreater(original[0], original[1])
            self.assertEqual(fixed[0], fixed[1])
            self.assertEqual(fixed[1], fixed[2])
            self.assertGreater(fixed[0], 0)
            self.assertEqual(fixed[3], original[3])
        self.assertEqual(changed.getpixel((20, 12))[:3], (145, 145, 145))
        self.assertEqual(changed.getpixel((64, 32)), base.getpixel((64, 32)))
        self.assertEqual(changed.getpixel((64, 64)), base.getpixel((64, 64)))

    def test_upper_trims_never_change_bottom_or_red_and_white_center(self):
        base = apply_cleanup(remove_external_matte(fixture()), 2, 16, 0)
        versions = [base]
        for label in LABELS[1:]:
            a, b = TOP_STYLES[label]
            trimmed = apply_top_cleanup(neutralize_upper_left_bezel(base), a, b)
            versions.append(trimmed)
            for y in range(50, 128):
                for x in (4, 20, 64, 105, 123):
                    self.assertEqual(trimmed.getpixel((x, y)), base.getpixel((x, y)))
            self.assertEqual(trimmed.getpixel((64, 32)), base.getpixel((64, 32)))
        alphas = [v.getchannel("A") for v in versions]
        for earlier, later in zip(alphas, alphas[1:]):
            self.assertLessEqual(
                sum(later.crop((0, 0, 128, 44)).get_flattened_data()),
                sum(earlier.crop((0, 0, 128, 44)).get_flattened_data()))

    def test_four_distinct_transparent_icons_and_no_stale_files(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            stale = root / "mipmap-xxxhdpi/lab_clean_a.png"
            stale.parent.mkdir(parents=True)
            stale.write_bytes(b"obsolete")
            render_variants(fixture(), root)
            self.assertFalse(stale.exists())
            icons = sorted((root / "mipmap-xxxhdpi").glob("lab_clean_*.png"))
            self.assertEqual([i.stem for i in icons],
                             ["lab_clean_c", "lab_clean_c1", "lab_clean_c2", "lab_clean_c3"])
            outputs = []
            for path in icons:
                with Image.open(path) as image:
                    self.assertEqual(image.size, (192, 192))
                    self.assertEqual(image.getpixel((0, 0))[3], 0)
                    self.assertEqual(image.getpixel((191, 191))[3], 0)
                    self.assertGreater(image.getpixel((96, 96))[3], 230)
                    outputs.append(image.tobytes())
            self.assertEqual(len(set(outputs)), 4)

    def test_manifest_has_four_disabled_launcher_aliases_in_approved_order(self):
        xml = ET.parse(ROOT / "iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        aliases = xml.find("application").findall("activity-alias")
        self.assertEqual([a.attrib[ns + "label"] for a in aliases],
                         ["Clean C", "Clean C1", "Clean C2", "Clean C3"])
        for alias, label in zip(aliases, LABELS):
            self.assertEqual(alias.attrib[ns + "enabled"], "false")
            self.assertEqual(alias.attrib[ns + "icon"],
                             f"@mipmap/lab_clean_{label.lower()}")
            self.assertEqual(alias.attrib[ns + "name"], f".LabClean{label}")


if __name__ == "__main__":
    unittest.main()
