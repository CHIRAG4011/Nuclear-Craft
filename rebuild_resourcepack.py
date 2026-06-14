#!/usr/bin/env python3
"""
Rebuilds NuclearCraft-ResourcePack.zip by injecting any files
found in custom_textures/ over the existing pack.

Usage:
    python3 rebuild_resourcepack.py
"""

import zipfile, os, shutil

SRC_ZIP  = "nuclearcraft-plugin/src/main/resources/NuclearCraft-ResourcePack.zip"
OUT_ZIP  = "nuclearcraft-plugin/src/main/resources/NuclearCraft-ResourcePack.zip"
CUSTOM   = "custom_textures"
TMP_DIR  = "_rp_tmp"

if os.path.exists(TMP_DIR):
    shutil.rmtree(TMP_DIR)
os.makedirs(TMP_DIR)

print(f"Extracting {SRC_ZIP} ...")
with zipfile.ZipFile(SRC_ZIP) as z:
    z.extractall(TMP_DIR)

replaced = []
added    = []

SKIP_EXTS = {".md", ".txt", ".DS_Store"}

for root, dirs, files in os.walk(CUSTOM):
    for fname in files:
        if any(fname.endswith(ext) for ext in SKIP_EXTS):
            continue
        src_path = os.path.join(root, fname)
        rel_path = os.path.relpath(src_path, CUSTOM)
        dst_path = os.path.join(TMP_DIR, rel_path)
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        if os.path.exists(dst_path):
            replaced.append(rel_path)
        else:
            added.append(rel_path)
        shutil.copy2(src_path, dst_path)

print(f"  Replaced : {len(replaced)} file(s)")
for r in replaced:
    print(f"    ✅  {r}")
print(f"  Added    : {len(added)} file(s)")
for a in added:
    print(f"    ➕  {a}")

tmp_zip = OUT_ZIP + ".tmp"
with zipfile.ZipFile(tmp_zip, "w", zipfile.ZIP_DEFLATED) as zout:
    for root, dirs, files in os.walk(TMP_DIR):
        for fname in files:
            full = os.path.join(root, fname)
            arcname = os.path.relpath(full, TMP_DIR)
            zout.write(full, arcname)

shutil.move(tmp_zip, OUT_ZIP)
shutil.rmtree(TMP_DIR)

size_mb = os.path.getsize(OUT_ZIP) / 1024 / 1024
print(f"\nDone! {OUT_ZIP} rebuilt ({size_mb:.2f} MB)")
print("Remember to also copy the zip to the server and update resourcepack.yml with the new SHA-1.")
