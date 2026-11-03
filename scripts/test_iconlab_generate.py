"""Regression: v0.3.2 legacy launcher resources with new approved art.

The previous v0.5.1 legacy build was not equivalent: its new master had
much wider transparent margins than the old 128px source.
"""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from scripts.generate_iconlab_icons import (
    SOURCE, SOURCE_SIZE, SOURCE_INSET, OUTPUT_SIZE, APPROVED_SCALE,
    VARIANTS, prepare_128_source, fit_canvas_subpixel, render_variants)

ROOT = Path(__file__).resolve().parents[1]
IDs = [f"{i:02d}" for i in range(1, 9)]


class RealmeLegacyIconTests(unittest.TestCase):
    def test_no_adaptive_or_pinned_shortcut_fallback(self):
        code = (ROOT / "iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("Показать 8 вариантов", code)
        self.assertIn("setComponentEnabledSetting", code)
        self.assertNotIn("ShortcutManager", code)
        self.assertNotIn("requestPinShortcut", code)
        self.assertFalse((ROOT/"iconlab/src/main/res/mipmap-anydpi-v26").exists())
        manifest = ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        app = manifest.find("application")
        self.assertEqual(app.attrib[ns+"icon"], "@drawable/lab_app_icon")
        self.assertNotIn(ns+"roundIcon", app.attrib)
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases), 8)
        for alias, num in zip(aliases, IDs):
            self.assertEqual(alias.attrib[ns+"icon"], f"@mipmap/lab_size_{num}")
            self.assertEqual(alias.attrib[ns+"enabled"], "false")

    def test_approved_bitmap_unmodified_and_128px_art_matches_old_footprint(self):
        self.assertEqual((SOURCE_SIZE, SOURCE_INSET, OUTPUT_SIZE), (128,4,192))
        self.assertAlmostEqual(APPROVED_SCALE, 1.05735)
        self.assertEqual(VARIANTS[0], ("01", APPROVED_SCALE))
        self.assertEqual([v[0] for v in VARIANTS], IDs)
        with Image.open(SOURCE) as source:
            original = source.convert("RGBA")
            before = original.tobytes()
            base = prepare_128_source(original)
            self.assertEqual(original.tobytes(), before)
            self.assertEqual(base.size, (128,128))
            self.assertEqual(base.getpixel((0,0))[3], 0)
            self.assertEqual(base.getpixel((127,127))[3], 0)
            bbox = base.getchannel("A").point(
                lambda a: 255 if a > 100 else 0).getbbox()
            self.assertGreaterEqual(bbox[0], 3)
            self.assertGreaterEqual(bbox[1], 3)
            self.assertLessEqual(bbox[2], 125)
            self.assertLessEqual(bbox[3], 125)
            self.assertGreaterEqual(bbox[2]-bbox[0], 117)

    def test_pngs_close_to_old_full_size_without_white_or_black_matte(self):
        with Image.open(SOURCE) as original, tempfile.TemporaryDirectory() as td:
            root=Path(td)
            base=render_variants(original, root)
            folder=root/"mipmap-xxxhdpi"
            files=sorted(folder.glob("lab_size_*.png"))
            self.assertEqual([p.stem for p in files],
                             [f"lab_size_{x}" for x in IDs])
            seen=set()
            for p in files:
                with Image.open(p) as icon:
                    self.assertEqual(icon.size, (192,192))
                    self.assertEqual(icon.mode, "RGBA")
                    self.assertEqual(icon.getpixel((0,0))[3], 0)
                    self.assertEqual(icon.getpixel((191,191))[3], 0)
                    self.assertGreater(icon.getpixel((96,96))[3], 200)
                    seen.add(icon.tobytes())
            self.assertEqual(len(seen), 8)
            with Image.open(files[0]) as first:
                expected=fit_canvas_subpixel(base, 192, APPROVED_SCALE)
                self.assertEqual(first.tobytes(), expected.tobytes())
                region=first.getchannel("A").point(
                    lambda a:255 if a>100 else 0).getbbox()
                self.assertLessEqual(region[0], 5)
                self.assertLessEqual(region[1], 5)
                self.assertGreaterEqual(region[2], 186)
                self.assertGreaterEqual(region[3], 186)

    def test_repeated_generation_cleans_stale_adaptive_resources(self):
        with Image.open(SOURCE) as original, tempfile.TemporaryDirectory() as td:
            root=Path(td)
            d=root/"mipmap-anydpi-v26";d.mkdir(parents=True)
            (d/"lab_size_01.xml").write_text("<adaptive-icon />")
            d2=root/"drawable-nodpi";d2.mkdir(parents=True)
            (d2/"lab_background_art.webp").write_bytes(b"legacy-test")
            render_variants(original, root)
            self.assertFalse(list(d.glob("lab_size_*.xml")))
            self.assertFalse((d2/"lab_background_art.webp").exists())


if __name__ == "__main__":
    unittest.main()
