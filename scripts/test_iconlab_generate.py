"""Icon Lab approved artwork and shortcut behavior regressions."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from scripts.generate_iconlab_icons import VARIANTS, SOURCE, render_icon, render_variants

ROOT = Path(__file__).resolve().parents[1]
NUMBERS = [f"{i:02d}" for i in range(1, 9)]


class IconLabApprovedMasterTest(unittest.TestCase):
    def test_eight_uniform_scales_not_art_edits(self):
        self.assertEqual([name for name, scale in VARIANTS], NUMBERS)
        self.assertEqual([round(100*scale) for _, scale in VARIANTS],
                         [108, 109, 110, 111, 112, 113, 114, 115])
        self.assertTrue(SOURCE.is_file(), "Approved icon source must be committed")
        with Image.open(SOURCE) as art:
            self.assertEqual(art.size, (192, 192))
            self.assertEqual(art.mode, "RGBA")
            self.assertEqual(art.getpixel((0, 0))[3], 0)

    def test_all_outputs_are_transparent_and_no_opaque_white_background(self):
        with Image.open(SOURCE) as source, tempfile.TemporaryDirectory() as td:
            render_variants(source, Path(td))
            files = sorted((Path(td) / "mipmap-xxxhdpi").glob("lab_size_*.png"))
            self.assertEqual([p.stem for p in files],
                             [f"lab_size_{i}" for i in NUMBERS])
            for p in files:
                with Image.open(p) as result:
                    self.assertEqual(result.mode, "RGBA")
                    self.assertEqual(result.size, (192,192))
                    self.assertEqual(result.getpixel((0,0))[3], 0)
                    self.assertEqual(result.getpixel((191,191))[3], 0)
                    self.assertGreater(result.getpixel((96,96))[3], 240)
                    self.assertEqual(max(result.getchannel("A").crop(
                        (0,0,192,1)).getdata()), 0, p.stem)

    def test_size_variants_are_distinct_and_increase_image_coverage(self):
        with Image.open(SOURCE) as source:
            icons = [render_icon(source, scale) for _, scale in VARIANTS]
            areas = [sum(a > 128 for a in img.getchannel("A").getdata())
                     for img in icons]
            self.assertEqual(len(set(img.tobytes() for img in icons)), 8)
            self.assertEqual(areas, sorted(areas))
            self.assertGreater(areas[-1], areas[0])

    def test_eight_aliases_and_images_have_consistent_ids_and_labels(self):
        manifest=ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot()
        ns="{http://schemas.android.com/apk/res/android}"
        aliases=manifest.find("application").findall("activity-alias")
        self.assertEqual(len(aliases), 8)
        for i, (alias, (name, scale)) in enumerate(zip(aliases, VARIANTS)):
            self.assertEqual(alias.attrib[ns+"name"], f".LabSize{name}")
            self.assertEqual(alias.attrib[ns+"icon"], f"@mipmap/lab_size_{name}")
            self.assertEqual(alias.attrib[ns+"label"],
                             f"{name} · {round(scale*100)}%")
            self.assertEqual(alias.attrib[ns+"enabled"], "false")

    def test_one_button_requests_shortcuts_and_keeps_launcher_alias_fallback(self):
        code=(ROOT/"iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("button.setOnClickListener(view -> addAllIcons())", code)
        self.assertIn("setComponentEnabledSetting", code)
        self.assertIn("requestPinShortcut", code)
        self.assertIn("isRequestPinShortcutSupported", code)
        self.assertIn('new ShortcutInfo.Builder(this, "iconlab_size_" + number)', code)
        self.assertIn("SCALES.length", code)
        self.assertIn("Запрошено размещение", code)
        self.assertIn("Ваш лаунчер не поддерживает", code)


if __name__ == "__main__":
    unittest.main()
