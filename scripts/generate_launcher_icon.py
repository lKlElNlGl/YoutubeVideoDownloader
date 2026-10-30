"""Generate alpha-masked foreground from the original approved artwork.
All pixels outside the complete rounded glossy button are transparent.
The internal black bezel, red play symbol, white arrow, highlights and shadows are unchanged.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageChops

source=Path(__file__).resolve().parents[1]/"app/src/main/res/drawable-nodpi/ic_launcher_reference.webp"
target=source.with_name("ic_launcher_clean.png")
artwork=Image.open(source).convert("RGBA")
width,height=artwork.size

# Supersample the contour, so transparent corners stay smooth at launcher sizes.
supersample=4
edge=min(width,height)/128.0
mask=Image.new("L",(width*supersample,height*supersample),0)
drawer=ImageDraw.Draw(mask)
inset=7.0*edge
radius=23.0*edge
drawer.rounded_rectangle(
 (round(inset*supersample),round(inset*supersample),
  round((width-1-inset)*supersample),round((height-1-inset)*supersample)),
 radius=round(radius*supersample),fill=255
)
mask=mask.resize((width,height),Image.Resampling.LANCZOS)
mask=mask.filter(ImageFilter.GaussianBlur(0.4*edge))
artwork.putalpha(ImageChops.darker(artwork.getchannel("A"),mask))
artwork.save(target,optimize=True)
assert artwork.getpixel((0,0))[3]==0
assert artwork.getpixel((width//2,height//2))[3]>230
print(f"Generated {target.name}: {width}x{height}, transparent matte removed")
