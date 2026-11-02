"""Regression tests: 9 distinct high-resolution variants of the original Icon 04."""
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
    def test_scale_ladder_centers_on_original_04_and_shrinks_in_quarter_percent_steps(self):
        scales = variant_scales()
        self.assertEqual(list(scales), EXPECTED_NAMES)
        self.assertEqual(COUNT, 9)
        self.assertAlmostEqual(scales["04a"], 1.06)
        self.assertAlmostEqual(scales["04i"], 1.06 * 0.98)
        for i in range(8):
            self.assertAlmostEqual(
                scales[EXPECTED_NAMES[i + 1]] / BASE_SCALE,
                1 - (i + 1) * STEP)

    def test_renders_nine_distinct_transparent_icons(self):
        original = Image.new("RGBA", (128, 128), (255, 0, 0, 255))
        with tempfile.TemporaryDirectory() as temp:
            res = Path(temp)
            render_variants(original, res)
            outputs = sorted((res / "mipmap-nodpi").glob("lab_icon_*.png"))
            self.assertEqual([p.stem for p in outputs],
                             [f"lab_icon_{name}" for name in EXPECTED_NAMES])
            fingerprints = []
            areas = []
            for path in outputs:
                with Image.open(path) as icon:
                    self.assertEqual(icon.size, (OUTPUT_SIZE, OUTPUT_SIZE))
                    self.assertEqual(icon.getpixel((0, 0))[3], 0)
                    self.assertGreater(icon.getpixel((OUTPUT_SIZE // 2,
                                                      OUTPUT_SIZE // 2))[3], 230)
                    fingerprints.append(icon.tobytes())
                    bounds = icon.getchannel("A").point(
                        lambda p: 255 if p >= 128 else 0).getbbox()
                    areas.append((bounds[2] - bounds[0]) * (bounds[3] - bounds[1]))
            self.assertEqual(len(set(fingerprints)), 9)
            self.assertTrue(all(a > b for a, b in zip(areas, areas[1:])), areas)

    def test_no_old_variant_resources_survive_regeneration(self):
        with tempfile.TemporaryDirectory() as temp:
            res = Path(temp)
            directory = res / "mipmap-xxxhdpi"
            directory.mkdir(parents=True)
            old = directory / "lab_icon_04.png"
            old.write_bytes(b"old")
            render_variants(Image.new("RGBA", (128, 128), "black"), res)
            self.assertFalse(old.exists())

    def test_manifest_has_exactly_nine_disabled_aliases(self):
        root = ET.parse(ROOT / "iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        app = root.find("application")
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases), 9)
        self.assertEqual(
            [a.attrib[ns + "name"] for a in aliases],
            [f".LabVariant{name}" for name in EXPECTED_NAMES])
        self.assertEqual(
            [a.attrib[ns + "label"] for a in aliases],
            [f"Icon {name}" for name in EXPECTED_NAMES])
        for alias, name in zip(aliases, EXPECTED_NAMES):
            self.assertEqual(alias.attrib[ns + "enabled"], "false")
            self.assertEqual(alias.attrib[ns + "targetActivity"], ".MainActivity")
            self.assertEqual(alias.attrib[ns + "icon"], f"@mipmap/lab_icon_{name}")
            self.assertIsNotNone(alias.find("intent-filter"))


if __name__ == "__main__":
    unittest.main()
