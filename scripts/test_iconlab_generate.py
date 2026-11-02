"""8 upper-rim variants, immutable Clean C2 and no accidental resizing."""
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

sys.path.insert(0,str(Path(__file__).resolve().parent))
from generate_iconlab_icons import (
    VARIANTS, SOURCE, APPROVED_SCALE, OUTPUT_SIZE, remove_external_matte,
    apply_cleanup, apply_top_cleanup, neutralize_upper_left_bezel,
    clean_c2_reference, repair_upper_rim, extend_upper_black,
    fit_canvas_subpixel, render_variants, smooth_canvas_top_if_crowded)
ROOT=Path(__file__).resolve().parents[1]
LABELS=[f"{n:02d}" for n in range(1,9)]


def fixture():
    im=Image.new("RGBA",(128,128),(54,55,59,255))
    for x in range(10,118):
        for y in range(3,10):
            im.putpixel((x,y),(122,121,121,255))
    for x in range(28,100):
        for y in range(24,95):
            im.putpixel((x,y),(230,35,39,255))
    for x in range(52,78):
        for y in range(53,76):
            im.putpixel((x,y),(249,249,249,255))
    im.putpixel((18,14),(125,70,74,255))
    im.putpixel((20,12),(199,130,136,255))
    return im


class IconLabTests(unittest.TestCase):
    def test_four_same_size_and_four_gradual_extensions(self):
        self.assertEqual([v[0] for v in VARIANTS],LABELS)
        self.assertEqual([v[2] for v in VARIANTS],[0,0,0,0,.5,1,1.5,2])
        self.assertEqual([v[1] for v in VARIANTS[:4]],[0,.35,.65,.95])
        self.assertEqual(OUTPUT_SIZE,192)
        self.assertAlmostEqual(APPROVED_SCALE,1.06*.9975)

    def test_first_icon_pixel_identical_to_clean_c2_previous_version(self):
        old_c=apply_cleanup(remove_external_matte(fixture()),2,16,0)
        old_c2=apply_top_cleanup(neutralize_upper_left_bezel(old_c),1.0,0)
        self.assertEqual(clean_c2_reference(fixture()).tobytes(),old_c2.tobytes())
        expected=fit_canvas_subpixel(old_c2,192,APPROVED_SCALE)
        with tempfile.TemporaryDirectory() as td:
            render_variants(fixture(),Path(td))
            with Image.open(Path(td)/"mipmap-xxxhdpi/lab_top_01.png") as got:
                self.assertEqual(got.tobytes(),expected.tobytes())

    def test_rim_recolor_reduces_gray_brightness_with_exact_alpha_and_body(self):
        base=clean_c2_reference(fixture())
        variants=[repair_upper_rim(base,s) for s in [0,.35,.65,.95]]
        sums=[sum(sum(img.getpixel((64,y))[:3]) for y in range(12))
              for img in variants]
        self.assertTrue(all(a>b for a,b in zip(sums,sums[1:])),sums)
        for im in variants:
            self.assertEqual(im.getchannel("A").tobytes(),
                             base.getchannel("A").tobytes())
            self.assertEqual(im.getpixel((64,36)),base.getpixel((64,36)))
            self.assertEqual(im.getpixel((64,66)),base.getpixel((64,66)))

    def test_extension_does_not_shift_any_pixel_below_red_panel_or_on_sides(self):
        base=repair_upper_rim(clean_c2_reference(fixture()),.95)
        variants=[extend_upper_black(base,g) for g in [.5,1,1.5,2]]
        for im in variants:
            for y in range(23,128):
                for x in range(128):
                    self.assertEqual(im.getpixel((x,y)),base.getpixel((x,y)))
            for y in range(23):
                for x in list(range(11))+list(range(117,128)):
                    self.assertEqual(im.getpixel((x,y)),base.getpixel((x,y)))
        area=lambda im:sum(im.getchannel("A").crop((40,0,88,15)).get_flattened_data())
        self.assertGreater(area(variants[-1]),area(variants[0]))

    def test_eight_distinct_transparent_192px_png_files_no_stale_images(self):
        with tempfile.TemporaryDirectory() as td:
            d=Path(td)
            stale=d/"mipmap-xxxhdpi/lab_clean_c2.png"
            stale.parent.mkdir(parents=True)
            stale.write_bytes(b"stale")
            render_variants(fixture(),d)
            self.assertFalse(stale.exists())
            files=sorted((d/"mipmap-xxxhdpi").glob("lab_top_*.png"))
            self.assertEqual([p.stem for p in files],[f"lab_top_{n}" for n in LABELS])
            seen=set()
            for p in files:
                with Image.open(p) as im:
                    self.assertEqual(im.size,(192,192))
                    self.assertEqual(im.mode,"RGBA")
                    self.assertEqual(im.getpixel((0,0))[3],0)
                    self.assertEqual(im.getpixel((191,191))[3],0)
                    self.assertGreater(im.getpixel((96,96))[3],225)
                    seen.add(im.tobytes())
            self.assertEqual(len(seen),8)

    def test_launcher_aliases_order_and_labels(self):
        doc=ET.parse(ROOT/"iconlab/src/main/AndroidManifest.xml").getroot()
        ns="{http://schemas.android.com/apk/res/android}"
        aliases=doc.find("application").findall("activity-alias")
        self.assertEqual(len(aliases),8)
        for a,n in zip(aliases,LABELS):
            self.assertTrue(a.attrib[ns+"label"].startswith(n+" "))
            self.assertEqual(a.attrib[ns+"name"],f".LabTop{n}")
            self.assertEqual(a.attrib[ns+"enabled"],"false")
            self.assertEqual(a.attrib[ns+"icon"],f"@mipmap/lab_top_{n}")

    def test_top_boundary_antialias_only_when_growing_rim_reaches_canvas(self):
        image=Image.new("RGBA",(192,192),(25,25,25,255))
        # Only the top 3 output rows are softened. Interior is untouched.
        fixed=smooth_canvas_top_if_crowded(image)
        self.assertEqual(fixed.getpixel((96,0))[3],24)
        self.assertEqual(fixed.getpixel((96,1))[3],170)
        self.assertEqual(fixed.getpixel((96,2))[3],240)
        self.assertEqual(fixed.getpixel((96,3)),image.getpixel((96,3)))
        self.assertEqual(fixed.getpixel((96,120)),image.getpixel((96,120)))
        self.assertEqual(fixed.getpixel((96,0))[:3],image.getpixel((96,0))[:3])

    def test_actual_art_not_opaque_cutoff_across_top_png_edge(self):
        if not SOURCE.is_file():
            self.skipTest("requires real source from GitHub")
        with Image.open(SOURCE) as original,tempfile.TemporaryDirectory() as td:
            render_variants(original,Path(td))
            for n in LABELS:
                with Image.open(Path(td)/"mipmap-xxxhdpi"/f"lab_top_{n}.png") as im:
                    self.assertLess(max(im.getchannel("A")
                                       .crop((0,0,192,1)).get_flattened_data()),48,n)

if __name__=="__main__":
    unittest.main()
