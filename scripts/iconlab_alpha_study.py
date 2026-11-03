"""IconLab diagnostic: inspect the ACTUAL legacy Android icon pixel silhouettes."""
from PIL import Image
from pathlib import Path
from tempfile import TemporaryDirectory
from collections import Counter
from scripts import iconlab_reference_v032 as old
from scripts import iconlab_legacy_v055 as new
from scripts.generate_iconlab_icons import render_controls
def report(label, image):
    im=image.convert("RGBA"); alpha=im.getchannel("A")
    print("\n"+label, "size", im.size)
    for threshold in [0,2,8,16,32,64,128,200,250]:
        mask=alpha.point(lambda a: 255 if a>threshold else 0)
        bb=mask.getbbox(); pix=sum(a>threshold for a in alpha.getdata())
        print(f" alpha>{threshold} bbox={bb} count={pix} areaFraction={pix/(im.width*im.height):.4f}")
    print("rgba corners", [im.getpixel(pos) for pos in [(0,0),(im.width-1,0),(0,im.height-1),(im.width-1,im.height-1)]])
    for y in range(0,im.height,max(1,im.height//16)):
        s=""
        for x in range(0,im.width,max(1,im.width//32)):
            v=alpha.getpixel((x,y))
            s+=" " if v==0 else "." if v<32 else "+" if v<128 else "#" if v<240 else "@"
        print(s[:32])
with TemporaryDirectory() as temp:
    out=Path(temp)
    render_controls(out)
    oldpng=Image.open(out/"mipmap-xxxhdpi/lab_probe_old.png")
    newpng=Image.open(out/"mipmap-xxxhdpi/lab_probe_new.png")
    report("A old v0.3.2 Clean C2",oldpng)
    report("B new v0.5.7 C2-alpha",newpng)
    with Image.open(old.SOURCE) as im: report("old 128px source",im)
    with Image.open(new.SOURCE) as im:report("new 192px display source",im)
