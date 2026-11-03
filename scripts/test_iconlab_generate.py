"""Check the proven legacy 128 -> 192 launcher mechanism and no home pins."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image
from scripts.generate_iconlab_adaptive import ADAPTIVE_SIZE, ADAPTIVE_SCALES
from scripts.generate_iconlab_icons import (
    APPROVED_SCALE, OUTPUT_SIZE, SOURCE_SIZE, SOURCE,
    VARIANTS, fit_canvas_subpixel, render_variants)

ROOT = Path(__file__).resolve().parents[1]
NUMBERS = [f"{i:02d}" for i in range(1, 9)]


class IconLabLegacyLauncherTests(unittest.TestCase):
    def test_reference_scale_and_128_to_192_resource_as_prior_working_version(self):
        self.assertEqual(SOURCE_SIZE, 128)
        self.assertEqual(OUTPUT_SIZE, 192)
        self.assertAlmostEqual(APPROVED_SCALE, 1.06 * 0.9975)
        self.assertEqual(VARIANTS[0], ("01", APPROVED_SCALE))
        self.assertEqual([n for n, _ in VARIANTS], NUMBERS)
        self.assertEqual([round(100*s) for _,s in VARIANTS[1:]],
                         [109, 110, 111, 112, 113, 114, 115])

    def test_master_asset_and_alpha_are_preserved_through_scaling(self):
        self.assertTrue(SOURCE.is_file())
        with Image.open(SOURCE) as source:
            self.assertEqual(source.size, (192, 192))
            self.assertEqual(source.mode, "RGBA")
            self.assertEqual(source.getpixel((0, 0))[3], 0)
            old = source.tobytes()
            with tempfile.TemporaryDirectory() as tmp:
                base = render_variants(source, Path(tmp))
                self.assertEqual(source.tobytes(), old)
                self.assertEqual(base.size, (128, 128))
                self.assertEqual(base.getpixel((0, 0))[3], 0)
                self.assertEqual(base.getpixel((127, 127))[3], 0)
                generated = Path(tmp) / "mipmap-xxxhdpi"
                files = sorted(generated.glob("lab_size_*.png"))
                self.assertEqual([p.stem for p in files],
                                 [f"lab_size_{n}" for n in NUMBERS])
                with Image.open(files[0]) as icon01:
                    reference = fit_canvas_subpixel(base, 192, APPROVED_SCALE)
                    self.assertEqual(icon01.tobytes(), reference.tobytes())
                for p in files:
                    with Image.open(p) as icon:
                        self.assertEqual(icon.size, (192, 192))
                        self.assertEqual(icon.mode, "RGBA")
                        self.assertEqual(icon.getpixel((0,0))[3], 0)
                        self.assertEqual(icon.getpixel((191,191))[3], 0)
                        self.assertLessEqual(max(icon.getchannel("A").crop(
                            (0,0,192,1)).getdata()), 4)

    def test_manifest_uses_legacy_nonadaptive_activity_aliases(self):
        ns = "{http://schemas.android.com/apk/res/android}"
        app = ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot().find("application")
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases), 8)
        for alias, n in zip(aliases, NUMBERS):
            self.assertEqual(alias.attrib[ns+"name"], f".LabSize{n}")
            self.assertEqual(alias.attrib[ns+"icon"], f"@mipmap/lab_size_{n}")
            self.assertEqual(alias.attrib[ns+"enabled"], "false")
        # Android 8+ uses adaptive resources; legacy PNG remains as fallback.
        # The adaptive XML is generated during preBuild, not checked in.
        self.assertEqual(len(ADAPTIVE_SCALES), 8)

    def test_button_only_enables_app_drawer_aliases_no_home_screen_pinning(self):
        code=(ROOT/"iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("Показать 8 вариантов", code)
        self.assertIn("setComponentEnabledSetting", code)
        self.assertIn("COMPONENT_ENABLED_STATE_ENABLED", code)
        self.assertIn("PackageManager.DONT_KILL_APP", code)
        self.assertIn("в списке приложений", code)
        self.assertNotIn("requestPinShortcut", code)
        self.assertNotIn("ShortcutManager", code)
        self.assertNotIn("ShortcutInfo", code)
        self.assertNotIn("Добавить 8 значков на рабочий стол", code)

    def test_adaptive_icons_are_transparent_and_fit_safe_region(self):
        with Image.open(SOURCE) as source, tempfile.TemporaryDirectory() as td:
            before = source.tobytes()
            render_variants(source, Path(td))
            self.assertEqual(source.tobytes(), before)
            xmlns = "{http://schemas.android.com/apk/res/android}"
            for i, scale in enumerate(ADAPTIVE_SCALES, start=1):
                number = f"{i:02d}"
                xml = Path(td)/"mipmap-anydpi-v26"/f"lab_size_{number}.xml"
                root = ET.parse(xml).getroot()
                self.assertEqual(root.tag, "adaptive-icon")
                self.assertEqual(root.find("background").attrib[xmlns+"drawable"],
                                 "@android:color/transparent")
                self.assertEqual(root.find("foreground").attrib[xmlns+"drawable"],
                                 f"@drawable/lab_adaptive_foreground_{number}")
                icon_path = Path(td)/"drawable-nodpi"/f"lab_adaptive_foreground_{number}.png"
                with Image.open(icon_path) as icon:
                    self.assertEqual(icon.mode, "RGBA")
                    self.assertEqual(icon.size, (ADAPTIVE_SIZE, ADAPTIVE_SIZE))
                    self.assertEqual(icon.getpixel((0,0))[3], 0)
                    self.assertEqual(icon.getpixel((431,431))[3], 0)
                    # The visible icon must remain fully within the central
                    # Android adaptive safe region, avoiding clipped black rim.
                    bbox = icon.getchannel("A").getbbox()
                    self.assertGreaterEqual(bbox[0], 72)
                    self.assertGreaterEqual(bbox[1], 72)
                    self.assertLessEqual(bbox[2], 360)
                    self.assertLessEqual(bbox[3], 360)


if __name__ == "__main__":
    unittest.main()
