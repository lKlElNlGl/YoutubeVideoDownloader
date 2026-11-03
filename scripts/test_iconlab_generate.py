"""Regression tests for Icon Lab legacy PNG variants at 96–106%, step 1%."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from scripts.generate_iconlab_icons import (
    SOURCE, SOURCE_SIZE, SOURCE_INSET, OUTPUT_SIZE, SUPER_SAMPLE,
    VARIANTS, prepare_128_source, fit_canvas_subpixel, render_variants)

ROOT = Path(__file__).resolve().parents[1]
PERCENTAGES = list(range(96, 107))
IDS = [f"{i:02d}" for i in range(1, 12)]


class RealmeLegacyElevenIconsTest(unittest.TestCase):
    def test_variants_are_exactly_11_full_art_scales_96_through_106(self):
        self.assertEqual((SOURCE_SIZE, SOURCE_INSET, OUTPUT_SIZE, SUPER_SAMPLE),
                         (128, 4, 192, 8))
        self.assertEqual(len(VARIANTS), 11)
        self.assertEqual([name for name, _ in VARIANTS], IDS)
        self.assertEqual([round(scale*100) for _,scale in VARIANTS],
                         PERCENTAGES)
        for _,scale in VARIANTS:
            self.assertGreaterEqual(scale, 0.96)
            self.assertLessEqual(scale, 1.06)

    def test_manifest_has_exactly_eleven_plain_nonadaptive_launcher_aliases(self):
        manifest = ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        app = manifest.find("application")
        self.assertEqual(app.attrib[ns+"icon"], "@drawable/lab_app_icon")
        self.assertNotIn(ns+"roundIcon", app.attrib)
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases), 11)
        for alias, number, percent in zip(aliases, IDS, PERCENTAGES):
            self.assertEqual(alias.attrib[ns+"name"], f".LabSize{number}")
            self.assertEqual(alias.attrib[ns+"label"], f"{number} · {percent}%")
            self.assertEqual(alias.attrib[ns+"icon"], f"@mipmap/lab_size_{number}")
            self.assertEqual(alias.attrib[ns+"enabled"], "false")
        self.assertFalse((ROOT/"iconlab/src/main/res/mipmap-anydpi-v26").exists())

    def test_button_enables_eleven_icons_in_app_drawer_only(self):
        code = (ROOT/"iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("VARIANT_COUNT = 11", code)
        self.assertIn("Показать 11 вариантов", code)
        self.assertIn("Включены все 11 вариантов", code)
        self.assertIn("Сравните значки 01–11 в списке приложений.", code)
        self.assertIn("setComponentEnabledSetting", code)
        self.assertIn("PackageManager.DONT_KILL_APP", code)
        self.assertNotIn("ShortcutManager", code)
        self.assertNotIn("requestPinShortcut", code)

    def test_master_unchanged_and_all_outputs_transparent_and_distinct(self):
        self.assertTrue(SOURCE.is_file())
        with Image.open(SOURCE) as source, tempfile.TemporaryDirectory() as temp:
            orig = source.convert("RGBA")
            master_bytes = orig.tobytes()
            prepared = prepare_128_source(orig)
            self.assertEqual(orig.tobytes(), master_bytes)
            self.assertEqual(prepared.size, (128,128))
            self.assertEqual(prepared.getpixel((0,0))[3], 0)
            self.assertEqual(prepared.getpixel((127,127))[3], 0)
            output = Path(temp)
            result = render_variants(orig, output)
            self.assertEqual(result.tobytes(), prepared.tobytes())
            self.assertEqual(orig.tobytes(), master_bytes)

            files = sorted((output/"mipmap-xxxhdpi").glob("lab_size_*.png"))
            self.assertEqual([p.stem for p in files],
                             [f"lab_size_{name}" for name in IDS])
            image_bytes = set()
            areas = []
            for p,(name, scale) in zip(files, VARIANTS):
                with Image.open(p) as icon:
                    self.assertEqual(icon.size, (192,192))
                    self.assertEqual(icon.mode, "RGBA")
                    self.assertEqual(icon.getpixel((0,0))[3],0)
                    self.assertEqual(icon.getpixel((191,191))[3],0)
                    self.assertGreater(icon.getpixel((96,96))[3],200)
                    expected = fit_canvas_subpixel(prepared, 192, scale)
                    self.assertEqual(icon.tobytes(), expected.tobytes())
                    image_bytes.add(icon.tobytes())
                    alpha = icon.getchannel("A")
                    areas.append(sum(a >= 128 for a in alpha.getdata()))
            self.assertEqual(len(image_bytes), 11)
            self.assertEqual(areas, sorted(areas))
            self.assertGreater(areas[-1], areas[0])

    def test_generation_removes_old_adaptive_resources(self):
        with Image.open(SOURCE) as source, tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            old_xml = root/"mipmap-anydpi-v26/lab_size_01.xml"
            old_xml.parent.mkdir(parents=True)
            old_xml.write_text("<adaptive-icon />")
            background = root/"drawable-nodpi/lab_background_art.webp"
            background.parent.mkdir(parents=True)
            background.write_bytes(b"leftover")
            render_variants(source, root)
            self.assertFalse(old_xml.exists())
            self.assertFalse(background.exists())


if __name__ == "__main__":
    unittest.main()
