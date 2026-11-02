"""Protect Android density and exact Icon 04 baseline; test fine adjustments."""
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_iconlab_icons import (
    BASE_SCALE, COUNT, OUTPUT_SIZE, STEP, fit_canvas, render_variants,
    remove_external_matte, variant_scales,
)

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_NAMES = [f"04{chr(ord('a') + i)}" for i in range(9)]


class IconLabTests(unittest.TestCase):
    def test_nine_scale_steps_relative_to_original_icon_04(self):
        scales = variant_scales()
        self.assertEqual(list(scales), EXPECTED_NAMES)
        self.assertEqual(COUNT, 9)
        self.assertAlmostEqual(scales["04a"], 1.06)
        self.assertAlmostEqual(scales["04i"], 1.06 * 0.98)
        for i in range(9):
            self.assertAlmostEqual(scales[EXPECTED_NAMES[i]],
                                   BASE_SCALE * (1 - i * STEP))

    def test_exact_old_04_baseline_and_high_resolution_density_tag(self):
        source = Image.new("RGBA", (128, 128), (240, 10, 20, 255))
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp)
            clean = remove_external_matte(source)
            old_icon_04 = fit_canvas(clean, 192, 1.06)
            render_variants(source, output)
            self.assertEqual(OUTPUT_SIZE, 192)
            path = output / "mipmap-xxxhdpi" / "lab_icon_04a.png"
            self.assertTrue(path.exists(), "Icon 04a must be a 192px xxxhdpi icon")
            with Image.open(path) as actual:
                self.assertEqual(actual.size, (192, 192))
                self.assertEqual(actual.tobytes(), old_icon_04.tobytes(),
                                 "04a must match the approved old Icon 04 pixel for pixel")
            self.assertFalse((output / "mipmap-nodpi" / "lab_icon_04a.png").exists())

    def test_nine_distinct_bitmaps_with_transparent_corners(self):
        source = Image.new("RGBA", (128, 128), (240, 10, 20, 255))
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp)
            render_variants(source, output)
            files = sorted((output / "mipmap-xxxhdpi").glob("lab_icon_*.png"))
            self.assertEqual([p.stem for p in files],
                             [f"lab_icon_{name}" for name in EXPECTED_NAMES])
            fingerprints = []
            for path in files:
                with Image.open(path) as image:
                    self.assertEqual(image.size, (192, 192))
                    self.assertEqual(image.getpixel((0, 0))[3], 0)
                    self.assertGreater(image.getpixel((96, 96))[3], 230)
                    fingerprints.append(image.tobytes())
            self.assertEqual(len(set(fingerprints)), 9)

    def test_removes_obsolete_nodpi_icon_generated_by_previous_release(self):
        with tempfile.TemporaryDirectory() as temp:
            output = Path(temp)
            stale = output / "mipmap-nodpi" / "lab_icon_04a.png"
            stale.parent.mkdir(parents=True)
            stale.write_bytes(b"old invalid resource")
            render_variants(Image.new("RGBA", (128, 128), "black"), output)
            self.assertFalse(stale.exists())

    def test_manifest_exposes_exactly_nine_disabled_launcher_aliases(self):
        root = ET.parse(ROOT / "iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        aliases = root.find("application").findall("activity-alias")
        self.assertEqual(len(aliases), 9)
        for alias, name in zip(aliases, EXPECTED_NAMES):
            self.assertEqual(alias.attrib[ns + "name"], f".LabVariant{name}")
            self.assertEqual(alias.attrib[ns + "label"], f"Icon {name}")
            self.assertEqual(alias.attrib[ns + "enabled"], "false")
            self.assertEqual(alias.attrib[ns + "icon"], f"@mipmap/lab_icon_{name}")
            self.assertIsNotNone(alias.find("intent-filter"))


if __name__ == "__main__":
    unittest.main()
