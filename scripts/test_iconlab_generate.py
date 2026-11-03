"""Icon Lab: exact user-supplied icons with no rendering or masks."""
import hashlib
import struct
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "iconlab/src/main/res"
A = RES / "drawable-nodpi/lab_user_a_192.png"
B = RES / "drawable-nodpi/lab_user_b_large.png"

EXPECTED = [
    (A, (192, 189),
     "c54107553b5bdce6de2e5b333d8c136df02b0a2f62cf6d76b437327805c74a6f"),
    (B, (1035, 1019),
     "d479071d33054a04e95ddd8710f1d691b902f4edb06dc046b61646c5dde32112"),
]


class ExactUserIconTests(unittest.TestCase):
    def test_user_source_files_are_exact_original_png_bytes(self):
        for path, dimensions, expected_hash in EXPECTED:
            with self.subTest(path=path):
                self.assertTrue(path.is_file())
                content = path.read_bytes()
                self.assertEqual(hashlib.sha256(content).hexdigest(), expected_hash)
                self.assertTrue(content.startswith(b"\x89PNG\r\n\x1a\n"))
                self.assertEqual(struct.unpack(">II", content[16:24]), dimensions)
                with Image.open(path) as image:
                    self.assertEqual(image.size, dimensions)
                    self.assertEqual(image.mode, "RGBA")
                    for x,y in ((0,0),(image.width-1,0),
                                (0,image.height-1),(image.width-1,image.height-1)):
                        self.assertEqual(image.getpixel((x,y))[3], 0)

    def test_two_aliases_use_direct_nodpi_original_png_resources(self):
        root = ET.parse(RES.parent / "AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        app = root.find("application")
        self.assertEqual(app.attrib[ns+"icon"], "@drawable/lab_app_icon")
        self.assertNotIn(ns+"roundIcon", app.attrib)
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases),2)
        self.assertEqual(
            [(a.attrib[ns+"name"],a.attrib[ns+"label"],a.attrib[ns+"icon"])
             for a in aliases],
            [(".LabSourceSmall","_A_192px","@drawable/lab_user_a_192"),
             (".LabSourceLarge","_B_Большая","@drawable/lab_user_b_large")])
        self.assertTrue(all(a.attrib[ns+"enabled"]=="false" for a in aliases))
        self.assertFalse((RES/"mipmap-anydpi-v26").exists())

    def test_no_automatic_generation_or_shortcuts(self):
        gradle = (ROOT/"iconlab/build.gradle").read_text()
        self.assertIn("versionName '0.5.11'",gradle)
        self.assertNotIn("generateIconLabIcons",gradle)
        self.assertNotIn("preBuild",gradle)
        java=(ROOT/"iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("Показать 2 значка",java)
        self.assertIn("LabSourceSmall",java)
        self.assertIn("LabSourceLarge",java)
        self.assertIn("setComponentEnabledSetting",java)
        self.assertNotIn("ShortcutManager",java)
        self.assertNotIn("requestPinShortcut",java)


if __name__ == "__main__":
    unittest.main()
