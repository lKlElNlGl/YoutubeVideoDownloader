"""Fail the build unless the exact user-approved Icon Lab v0.5.11 PNG is packaged."""
from pathlib import Path
import hashlib
import struct

ROOT = Path(__file__).resolve().parents[1]
PNG = ROOT / "app/src/main/res/drawable-nodpi/ic_launcher.png"
EXPECTED = "c54107553b5bdce6de2e5b333d8c136df02b0a2f62cf6d76b437327805c74a6f"
payload = PNG.read_bytes()
assert hashlib.sha256(payload).hexdigest() == EXPECTED, "Approved launcher PNG was modified"
assert payload[:8] == b"\x89PNG\r\n\x1a\n", "Not a PNG image"
width, height, depth, color = struct.unpack(">IIBB", payload[16:26])
assert (width, height, depth, color) == (192, 189, 8, 6), "Approved RGBA dimensions changed"
manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
assert 'android:icon="@drawable/ic_launcher"' in manifest
assert 'android:roundIcon="@drawable/ic_launcher"' in manifest
assert not (ROOT / "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml").exists(), "Adaptive icon is forbidden"
assert not (ROOT / "app/src/main/res/drawable/ic_launcher.xml").exists(), "Legacy launcher wrapper is forbidden"
assert "generate_launcher_icon" not in (ROOT / "app/build.gradle").read_text(encoding="utf-8")
print(f"Approved launcher: {width}x{height}, SHA-256 {EXPECTED}")
