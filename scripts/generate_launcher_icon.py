"""Clean the original glossy launcher icon: transparent outside button silhouette.
Keep the approved red play symbol, white arrow and glossy outer frame unchanged.
Requires Pillow; run deterministically before Android resources are merged.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops
src=Path(__file__).resolve().parents[1]/"app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
dst=src.with_name("ic_launcher_clean.png")
photo=Image.open(src).convert("RGBA")
w,h=photo.size
scale=min(w,h)/128.0
# Remove the original outer matte, following the visible rounded button frame.
mask=Image.new("L",(w,h),0)
d=ImageDraw.Draw(mask)
d.rounded_rectangle((round(3*scale),round(3*scale),w-1-round(3*scale),h-1-round(3*scale)),radius=round(19*scale),fill=255)
mask=mask.filter(ImageFilter.GaussianBlur(0.7*scale))
photo.putalpha(ImageChops.darker(photo.getchannel("A"),mask))
photo.save(dst,optimize=True)
print(f"Generated transparent launcher foreground: {dst}")
