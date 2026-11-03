"""A/B icon test proves the two bitmaps are built from known, unmodified sources."""
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image, ImageChops, ImageFilter

from scripts import iconlab_reference_v032 as old
from scripts import iconlab_legacy_v055 as new
from scripts.generate_iconlab_icons import (
    render_controls, render_new_c2, prepare_new_c2_alpha,
    OLD_SOURCE, NEW_SOURCE)
from scripts.iconlab_edge_refine import (
    render_smooth_legacy_icon, smooth_proven_mask, trim_top_and_shoulders,
    extend_bezel_colors)
from PIL import ImageDraw

ROOT = Path(__file__).resolve().parents[1]


class DiagnosticOldVsNewTest(unittest.TestCase):
    def test_uses_actual_historical_and_approved_source_not_repaint(self):
        self.assertEqual(OLD_SOURCE, ROOT/"app/src/main/res/drawable-nodpi/ic_launcher_reference.webp")
        self.assertEqual(NEW_SOURCE, ROOT/"iconlab/assets/approved_master_display.webp")
        self.assertTrue(OLD_SOURCE.is_file())
        self.assertTrue(NEW_SOURCE.is_file())
        self.assertNotEqual(OLD_SOURCE.read_bytes(), NEW_SOURCE.read_bytes())

    def test_only_two_plain_legacy_icons_aliases_without_adaptive_resources(self):
        manifest = ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot()
        ns = "{http://schemas.android.com/apk/res/android}"
        app = manifest.find("application")
        self.assertEqual(app.attrib[ns+"icon"], "@drawable/lab_app_icon")
        self.assertNotIn(ns+"roundIcon", app.attrib)
        aliases = app.findall("activity-alias")
        self.assertEqual(len(aliases), 2)
        self.assertEqual([a.attrib[ns+"name"] for a in aliases],
                         [".LabProbeOld", ".LabProbeNew"])
        self.assertEqual([a.attrib[ns+"icon"] for a in aliases],
                         ["@mipmap/lab_probe_old","@mipmap/lab_probe_new"])
        self.assertEqual([a.attrib[ns+"label"] for a in aliases],
                         ["_A_Старый C2","_B_Сглаженная маска"])
        self.assertTrue(all(a.attrib[ns+"enabled"]=="false" for a in aliases))
        self.assertFalse((ROOT/"iconlab/src/main/res/mipmap-anydpi-v26").exists())

    def test_single_button_enables_only_two_app_drawer_aliases(self):
        java=(ROOT/"iconlab/src/main/java/com/lklelnlgl/iconlab/MainActivity.java").read_text()
        self.assertIn("Показать 2 значка",java)
        self.assertIn("LabProbeOld",java)
        self.assertIn("LabProbeNew",java)
        self.assertIn("setComponentEnabledSetting",java)
        self.assertNotIn("requestPinShortcut",java)
        self.assertNotIn("ShortcutManager",java)

    def test_old_exact_c2_and_new_exact_smooth_rebuild(self):
        with tempfile.TemporaryDirectory() as root:
            dst=Path(root)
            render_controls(dst)
            old_png=dst/"mipmap-xxxhdpi/lab_probe_old.png"
            new_png=dst/"mipmap-xxxhdpi/lab_probe_new.png"
            self.assertTrue(old_png.is_file())
            self.assertTrue(new_png.is_file())
            with tempfile.TemporaryDirectory() as temp:
                temp=Path(temp)
                with Image.open(OLD_SOURCE) as art:
                    old.render_variants(art,temp/"old")
                with Image.open(NEW_SOURCE) as art:
                    with Image.open(old_png) as old_icon:
                        expected_new=render_smooth_legacy_icon(render_new_c2(art),old_icon)
                self.assertEqual(old_png.read_bytes(),
                    (temp/"old/mipmap-xxxhdpi/lab_clean_c2.png").read_bytes())
                with Image.open(new_png) as got_new:
                    self.assertEqual(got_new.tobytes(),expected_new.tobytes())
            for icon_file in [old_png,new_png]:
                with Image.open(icon_file) as icon:
                    self.assertEqual(icon.size,(192,192))
                    self.assertEqual(icon.mode,"RGBA")
                    self.assertEqual(icon.getpixel((0,0))[3],0)
                    self.assertEqual(icon.getpixel((191,191))[3],0)
                    self.assertGreater(icon.getpixel((96,96))[3],220)
            self.assertNotEqual(old_png.read_bytes(),new_png.read_bytes())


    def test_new_c2_cleanup_changes_only_source_alpha_and_never_adds_opacity(self):
        # Every RGB component remains bit-for-bit identical to the prepared
        # approved source. External contour alone changes, as requested.
        with Image.open(NEW_SOURCE) as master:
            raw=master.convert("RGBA")
            original=raw.tobytes()
            prepared=new.prepare_128_source(raw)
            clipped=prepare_new_c2_alpha(raw)
            self.assertEqual(raw.tobytes(), original)
            self.assertEqual(clipped.size,(128,128))
            source_px=list(prepared.getdata())
            clipped_px=list(clipped.getdata())
            self.assertTrue(all(a[:3]==b[:3] for a,b in zip(source_px,clipped_px)))
            self.assertTrue(all(b[3]<=a[3] for a,b in zip(source_px,clipped_px)))
            self.assertTrue(any(b[3]<a[3] for a,b in zip(source_px,clipped_px)))
            self.assertEqual(clipped.getpixel((64,64))[3],prepared.getpixel((64,64))[3])

    def test_new_c2_is_not_old_v055_png(self):
        with Image.open(NEW_SOURCE) as art:
            source=new.prepare_128_source(art)
            previous=new.fit_canvas_subpixel(source,192,1.00)
            updated=render_new_c2(art)
            self.assertNotEqual(previous.tobytes(), updated.tobytes())
            self.assertEqual(updated.getpixel((0,0))[3],0)
            self.assertEqual(updated.getpixel((191,191))[3],0)


    def test_new_silhouette_is_smooth_and_near_old_coverage(self):
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            render_controls(folder)
            with Image.open(folder/"mipmap-xxxhdpi/lab_probe_old.png") as a, \
                 Image.open(folder/"mipmap-xxxhdpi/lab_probe_new.png") as b, \
                 Image.open(NEW_SOURCE) as artwork:
                reference = a.convert("RGBA")
                result = b.convert("RGBA")
                raw = render_new_c2(artwork)
                baseline_mask = smooth_proven_mask(reference.getchannel("A"))
                expected_mask = trim_top_and_shoulders(baseline_mask)
                self.assertEqual(result.getchannel("A").tobytes(),
                                 expected_mask.tobytes())
                # Rebuilt high-res mask is a distinct antialiased output.
                self.assertNotEqual(result.getchannel("A").tobytes(),
                                    reference.getchannel("A").tobytes())
                self.assertEqual(result.getpixel((0,0))[3],0)
                self.assertEqual(result.getpixel((191,191))[3],0)
                old_coverage = sum(x >= 128 for x in reference.getchannel("A").getdata())
                new_coverage = sum(x >= 128 for x in result.getchannel("A").getdata())
                self.assertLess(abs(old_coverage-new_coverage), 1100)
                self.assertLess(new_coverage, old_coverage)
                self.assertEqual(
                    result.getchannel("A").crop((0,130,192,192)).tobytes(),
                    baseline_mask.crop((0,130,192,192)).tobytes())
                # Never recolor any solid part of the new approved image.
                pixels_original = list(raw.getdata())
                pixels_clean = list(result.getdata())
                self.assertTrue(all(
                    a[:3] == b[:3] for a,b in zip(pixels_original,pixels_clean)
                    if a[3] >= 200))
                self.assertGreater(result.getpixel((96,96))[3],240)

    def test_newly_revealed_fringe_uses_nearest_bezel_not_transparent_garbage(self):
        source=Image.new("RGBA",(192,192),(255,0,255,0))
        draw=ImageDraw.Draw(source)
        draw.rounded_rectangle((40,40,151,151),radius=28,fill=(18,20,23,255))
        alpha=Image.new("L",(192,192),0)
        ImageDraw.Draw(alpha).rounded_rectangle(
            (35,35,156,156),radius=28,fill=255)
        old_reference=Image.new("RGBA",(192,192))
        old_reference.putalpha(alpha)
        result=render_smooth_legacy_icon(source, old_reference)
        self.assertGreater(result.getpixel((37,96))[3],200)
        self.assertEqual(result.getpixel((37,96))[:3],(18,20,23))
        self.assertEqual(result.getpixel((96,96))[:3],(18,20,23))
        self.assertEqual(result.getpixel((0,0))[3],0)
        self.assertEqual(source.getpixel((37,96)),(255,0,255,0))
        self.assertEqual(source.getpixel((96,96)),(18,20,23,255))


    def test_upper_edges_contract_but_lower_third_unchanged(self):
        baseline = Image.new("L", (192,192),0)
        ImageDraw.Draw(baseline).rounded_rectangle(
            (4,3,187,186), radius=30, fill=255)
        baseline=baseline.filter(ImageFilter.GaussianBlur(0.45))
        corrected=trim_top_and_shoulders(baseline)
        initial=list(baseline.getdata())
        updated=list(corrected.getdata())
        self.assertTrue(all(a<=b for a,b in zip(updated,initial)))
        self.assertTrue(any(a<b for a,b in zip(updated,initial)))
        # Top and shoulder areas are trimmed, centre stays solid.
        self.assertLess(sum(a>=128 for a in updated[:70*192]),
                        sum(a>=128 for a in initial[:70*192]))
        self.assertLess(updated[90*192+4],initial[90*192+4])
        self.assertEqual(corrected.getpixel((96,96)),255)
        # User specifically approved NO changes to the bottom.
        self.assertEqual(updated[130*192:],initial[130*192:])
        # A feathered rim, not a binary/hard cut.
        self.assertTrue(any(0<a<255 for a in updated[:70*192]))

    def test_trim_validates_supersampling_and_never_changes_opaque_interior(self):
        mask=Image.new("L",(192,192),0)
        ImageDraw.Draw(mask).rounded_rectangle(
            (4,4,187,187), radius=30, fill=255)
        corrected=trim_top_and_shoulders(mask,supersample=8)
        self.assertEqual(corrected.getpixel((96,96)),255)
        self.assertEqual(corrected.getpixel((96,180)),255)
        with self.assertRaises(ValueError):
            trim_top_and_shoulders(mask,supersample=4)
        with self.assertRaises(ValueError):
            trim_top_and_shoulders(mask,inset_px=4.0)

    def test_legacy_mask_requires_8x_highres(self):
        with Image.open(OLD_SOURCE) as source, tempfile.TemporaryDirectory() as d:
            old.render_variants(source,Path(d))
            with Image.open(Path(d)/"mipmap-xxxhdpi/lab_clean_c2.png") as image:
                alpha=image.getchannel("A")
                mask=smooth_proven_mask(alpha, supersample=8)
                self.assertEqual(mask.size,(192,192))
                self.assertEqual(mask.mode,"L")
                with self.assertRaises(ValueError):
                    smooth_proven_mask(alpha,supersample=4)

    def test_cleans_stale_test_adaptive_and_old_variant_resources(self):
        with tempfile.TemporaryDirectory() as d:
            d=Path(d)
            stale=d/"mipmap-anydpi-v26/lab_size_01.xml"
            stale.parent.mkdir(parents=True)
            stale.write_text("<adaptive-icon/>")
            oldpng=d/"mipmap-xxxhdpi/lab_size_01.png"
            oldpng.parent.mkdir(parents=True)
            oldpng.write_bytes(b"stale")
            render_controls(d)
            self.assertFalse(stale.exists())
            self.assertFalse(oldpng.exists())
            files=sorted((d/"mipmap-xxxhdpi").glob("*.png"))
            self.assertEqual([f.name for f in files],
                             ["lab_probe_new.png","lab_probe_old.png"])


if __name__=="__main__":
    unittest.main()
