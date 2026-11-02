"""Regression tests: actual matte transparency and eight distinct launchers."""
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
from generate_iconlab_icons import render_variants

ROOT = Path(__file__).resolve().parents[1]


class IconLabTests(unittest.TestCase):
    def test_renders_eight_icons_with_clear_outside_corners(self):
        image = Image.new("RGBA", (128, 128), "white")
        draw = ImageDraw.Draw(image)
        draw.rounded_rectangle((4, 4, 123, 123), radius=27, fill="black")
        with tempfile.TemporaryDirectory() as temp:
            res = Path(temp)
            render_variants(image, res)
            images = []
            for index in range(1, 9):
                path = res / "mipmap-xxxhdpi" / f"lab_icon_{index:02d}.png"
                self.assertTrue(path.is_file())
                with Image.open(path) as actual:
                    self.assertEqual(actual.size, (192, 192))
                    self.assertEqual(actual.getpixel((0, 0))[3], 0)
                    self.assertGreater(actual.getpixel((96, 96))[3], 200)
                    images.append(actual.tobytes())
            self.assertEqual(len(set(images)), 8)

    def test_four_adaptive_resources_have_transparent_background(self):
        with tempfile.TemporaryDirectory() as temp:
            render_variants(Image.new("RGBA", (128, 128), "black"), Path(temp))
            for index in range(5, 9):
                xml = Path(temp) / "mipmap-anydpi-v26" / f"lab_icon_{index:02d}.xml"
                tree = ET.parse(xml)
                self.assertEqual(tree.getroot().tag, "adaptive-icon")
                self.assertEqual(
                    tree.getroot().find("background").attrib[
                        "{http://schemas.android.com/apk/res/android}drawable"],
                    "@android:color/transparent")
                self.assertTrue((Path(temp) / "drawable-nodpi" /
                                 f"lab_foreground_{index:02d}.png").exists())

    def test_manifest_exposes_eight_disabled_launcher_aliases(self):
        tree = ET.parse(ROOT / "iconlab/src/main/AndroidManifest.xml")
        ns = "{http://schemas.android.com/apk/res/android}"
        aliases = tree.getroot().find("application").findall("activity-alias")
        self.assertEqual(len(aliases), 8)
        self.assertEqual(
            {a.attrib[ns + "name"] for a in aliases},
            {f".LabVariant{i:02d}" for i in range(1, 9)})
        for alias in aliases:
            self.assertEqual(alias.attrib[ns + "enabled"], "false")
            self.assertEqual(alias.attrib[ns + "targetActivity"], ".MainActivity")
            self.assertTrue(alias.find("intent-filter") is not None)


if __name__ == "__main__":
    unittest.main()
