#!/usr/bin/env python3
"""One-shot importer: copies Create Crafts & Additions (MIT, by MRH0) phase-1 assets and data into the
Create fork under the `create:` namespace.

Kept in the repo so the import can be re-run or audited. It never overwrites a Create file that it did
not create: any id or path collision aborts the run.

Usage: python3 -I checks/cca-native/import-cca-assets.py <createaddition clone> <create fork root>
"""
import gzip
import io
import json
import shutil
import struct
import sys
from pathlib import Path

SRC = Path(sys.argv[1])
DST = Path(sys.argv[2])

CA_MAIN = SRC / "src/main/resources/assets/createaddition"
CA_GEN = SRC / "src/generated/resources/assets/createaddition"
CA_DATA = SRC / "src/generated/resources/data"

MAIN = DST / "src/main/resources"
GEN = DST / "src/generated/resources"

BLOCKS = ["rolling_mill", "electric_motor", "alternator", "connector", "small_light_connector",
          "large_connector", "modular_accumulator", "creative_energy", "electrum_block"]
ITEMS = ["electrum_ingot", "electrum_nugget", "electrum_sheet", "zinc_sheet", "copper_rod", "iron_rod",
         "gold_rod", "electrum_rod", "brass_rod", "copper_wire", "iron_wire", "gold_wire", "electrum_wire",
         "diamond_grit", "spool", "copper_spool", "gold_spool", "electrum_spool", "festive_spool", "capacitor",
         "diamond_grit_sandpaper"]
# Models Java code loads directly (AllPartialModels), relative to models/.
PARTIALS = ["block/connector/small_light", "block/modular_accumulator/gauge", "block/modular_accumulator/dial"]
# Texture folders copied whole: connected-texture sheets are referenced from code, not models.
TEXTURE_DIRS = ["block/modular_accumulator"]
# File renames (CC&A path -> fork path), relative to models/ or textures/.
RENAMES = {"block/modular_accumulator/guage": "block/modular_accumulator/gauge"}

RECIPES = {
    "crafting": ["capacitor_1", "capacitor_2", "connector", "copper_spool", "diamond_grit_sandpaper",
                 "electrum_block", "electrum_ingot_from_electrum_block", "electrum_ingot_from_nugget",
                 "electrum_nugget", "electrum_spool", "festive_spool", "gold_spool", "large_connector",
                 "modular_accumulator", "rolling_mill", "small_light_connector", "spool"],
    "mechanical_crafting": ["alternator", "electric_motor"],
    "mixing": ["electrum"],
    "pressing": ["electrum_ingot", "zinc_ingot"],
    "crushing": ["diamond"],
    "rolling": ["brass_rod", "copper_ingot", "copper_plate", "electrum_ingot", "electrum_plate", "gold_ingot",
                "gold_plate", "iron_ingot", "iron_plate"],
}
# Rolling recipes CC&A already shipped under the create namespace.
CREATE_NS_RECIPES = {"rolling": ["bamboo", "paper"]}
# Recipe ids get a prefix where CC&A's file name would read badly next to Create's own recipes.
RECIPE_RENAME = {
    ("crafting", "connector"): "small_connector",
    ("pressing", "electrum_ingot"): "electrum_sheet",
    ("pressing", "zinc_ingot"): "zinc_sheet",
    ("crushing", "diamond"): "diamond_grit",
}

written = []


def fail(msg):
    sys.exit("import-cca-assets: " + msg)


def rename_ns(text):
    return text.replace("createaddition:", "create:")


def fix_path(rel):
    for old, new in RENAMES.items():
        if rel == old:
            return new
    return rel


def fix_refs(obj):
    """Namespace + renamed paths inside a parsed JSON value."""
    if isinstance(obj, dict):
        return {k: fix_refs(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [fix_refs(v) for v in obj]
    if isinstance(obj, str):
        s = rename_ns(obj)
        if s.startswith("create:"):
            s = "create:" + fix_path(s[len("create:"):])
        return s
    return obj


def write_json(path, data, allow_existing=False):
    if path.exists() and not allow_existing:
        fail(f"collision, refusing to overwrite {path}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    written.append(path)


def copy_file(src, dst):
    if dst.exists():
        if dst.read_bytes() == src.read_bytes():
            return
        fail(f"collision, refusing to overwrite {dst}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
    written.append(dst)


# ---------------------------------------------------------------- models and textures

def find_model(rel):
    """CC&A model json for 'block/x' style path (searching main then generated)."""
    for base in (CA_MAIN, CA_GEN):
        for name in (rel, {v: k for k, v in RENAMES.items()}.get(rel, rel)):
            p = base / "models" / (name + ".json")
            if p.exists():
                return p
    return None


models_done = set()
textures = set()


def import_model(rel):
    rel = fix_path(rel)
    if rel in models_done:
        return
    models_done.add(rel)
    src = find_model(rel)
    if src is None:
        fail(f"model not found: {rel}")
    data = json.loads(src.read_text(encoding="utf-8"))
    parent = data.get("parent", "")
    if parent.startswith("createaddition:"):
        import_model(parent.split(":", 1)[1])
    for tex in (data.get("textures") or {}).values():
        if isinstance(tex, str) and tex.startswith("createaddition:"):
            textures.add(fix_path(tex.split(":", 1)[1]))
    write_json(MAIN / "assets/create/models" / (rel + ".json"), fix_refs(data))


def models_in_blockstate(data):
    found = []

    def walk(o):
        if isinstance(o, dict):
            m = o.get("model")
            if isinstance(m, str) and m.startswith("createaddition:"):
                found.append(m.split(":", 1)[1])
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)
    walk(data)
    return found


for block in BLOCKS:
    src = CA_GEN / "blockstates" / f"{block}.json"
    data = json.loads(src.read_text(encoding="utf-8"))
    for m in models_in_blockstate(data):
        import_model(m)
    write_json(GEN / "assets/create/blockstates" / f"{block}.json", fix_refs(data))

for item in BLOCKS + ITEMS:
    import_model(f"item/{item}")
    write_json(GEN / "assets/create/items" / f"{item}.json",
               {"model": {"type": "minecraft:model", "model": f"create:item/{item}"}})

for partial in PARTIALS:
    import_model(partial)

for tex_dir in TEXTURE_DIRS:
    for png in sorted((CA_MAIN / "textures" / tex_dir).glob("*.png")):
        if "_old" in png.stem or png.stem.endswith("_dev"):
            continue
        textures.add(f"{tex_dir}/{png.stem}")

for tex in sorted(textures):
    src = CA_MAIN / "textures" / (tex + ".png")
    if not src.exists():
        fail(f"texture not found: {tex}")
    copy_file(src, MAIN / "assets/create/textures" / (tex + ".png"))
    meta = src.with_suffix(".png.mcmeta")
    if meta.exists():
        copy_file(meta, MAIN / "assets/create/textures" / (tex + ".png.mcmeta"))


# ---------------------------------------------------------------- ponder schematics (NBT)

def nbt_read(buf):
    def rd(fmt):
        size = struct.calcsize(fmt)
        return struct.unpack(fmt, buf.read(size))[0]

    def rstr():
        n = rd(">H")
        return buf.read(n).decode("utf-8")

    def payload(t):
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 7: return buf.read(rd(">i"))
        if t == 8: return rstr()
        if t == 9:
            et = rd(">b")
            n = rd(">i")
            return (et, [payload(et) for _ in range(n)])
        if t == 10:
            out = []
            while True:
                ct = rd(">b")
                if ct == 0:
                    return out
                out.append((ct, rstr(), payload(ct)))
        if t == 11: return [rd(">i") for _ in range(rd(">i"))]
        if t == 12: return [rd(">q") for _ in range(rd(">i"))]
        raise ValueError(t)

    t = rd(">b")
    name = rstr()
    return t, name, payload(t)


def nbt_write(out, t, name, value):
    def wstr(s):
        b = s.encode("utf-8")
        out.write(struct.pack(">H", len(b)))
        out.write(b)

    def payload(t, v):
        if t == 1: out.write(struct.pack(">b", v))
        elif t == 2: out.write(struct.pack(">h", v))
        elif t == 3: out.write(struct.pack(">i", v))
        elif t == 4: out.write(struct.pack(">q", v))
        elif t == 5: out.write(struct.pack(">f", v))
        elif t == 6: out.write(struct.pack(">d", v))
        elif t == 7:
            out.write(struct.pack(">i", len(v)))
            out.write(v)
        elif t == 8: wstr(v)
        elif t == 9:
            et, items = v
            out.write(struct.pack(">b", et))
            out.write(struct.pack(">i", len(items)))
            for i in items:
                payload(et, i)
        elif t == 10:
            for ct, cn, cv in v:
                out.write(struct.pack(">b", ct))
                wstr(cn)
                payload(ct, cv)
            out.write(b"\x00")
        elif t == 11:
            out.write(struct.pack(">i", len(v)))
            for i in v: out.write(struct.pack(">i", i))
        elif t == 12:
            out.write(struct.pack(">i", len(v)))
            for i in v: out.write(struct.pack(">q", i))
        else:
            raise ValueError(t)

    out.write(struct.pack(">b", t))
    wstr(name)
    payload(t, value)


def nbt_rename(t, v):
    if t == 8:
        return rename_ns(v)
    if t == 9:
        et, items = v
        return (et, [nbt_rename(et, i) for i in items])
    if t == 10:
        return [(ct, cn, nbt_rename(ct, cv)) for ct, cn, cv in v]
    return v


PONDER = {"alternator": "alternator", "electric_motor": "electric_motor", "rolling_mill": "rolling_mill",
          "automate_rolling_mill": "automate_rolling_mill", "accumulator": "modular_accumulator",
          "connector": "connector"}
for src_name, dst_name in PONDER.items():
    raw = gzip.decompress((CA_MAIN / "ponder" / f"{src_name}.nbt").read_bytes())
    t, name, value = nbt_read(io.BytesIO(raw))
    value = nbt_rename(t, value)
    out = io.BytesIO()
    nbt_write(out, t, name, value)
    if b"createaddition" in out.getvalue():
        fail(f"ponder {src_name}: namespace left after rename")
    dst = MAIN / "assets/create/ponder" / f"{dst_name}.nbt"
    if dst.exists():
        fail(f"collision {dst}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_bytes(gzip.compress(out.getvalue(), mtime=0))
    written.append(dst)


# ---------------------------------------------------------------- loot tables and recipes

for block in BLOCKS:
    src = CA_DATA / "createaddition/loot_table/blocks" / f"{block}.json"
    data = fix_refs(json.loads(src.read_text(encoding="utf-8")))
    data.pop("random_sequence", None)
    write_json(GEN / "data/create/loot_table/blocks" / f"{block}.json", data)

def ingredient(v):
    """1.21.1 ingredient objects to the 26.x string form ("id", "#tag", or a list of those)."""
    if isinstance(v, list):
        return [ingredient(i) for i in v]
    if isinstance(v, dict):
        if set(v) == {"item"}:
            return v["item"]
        if set(v) == {"tag"}:
            return "#" + v["tag"]
        fail(f"unsupported ingredient {v}")
    return v


def port_recipe(data):
    if "key" in data:
        data["key"] = {k: ingredient(v) for k, v in data["key"].items()}
    if "ingredients" in data:
        data["ingredients"] = [ingredient(i) for i in data["ingredients"]]
    return data


# Crafting recipes sit in the same subfolders Create sorts its own into.
CRAFTING_FOLDER = {"rolling_mill": "kinetics", "electrum_block": "materials",
                   "electrum_ingot_from_electrum_block": "materials", "electrum_ingot_from_nugget": "materials",
                   "electrum_nugget": "materials", "diamond_grit_sandpaper": "materials"}

for kind, names in RECIPES.items():
    for name in names:
        src = CA_DATA / "createaddition/recipe" / kind / f"{name}.json"
        data = port_recipe(fix_refs(json.loads(src.read_text(encoding="utf-8"))))
        out_name = RECIPE_RENAME.get((kind, name), name)
        folder = kind
        if kind == "crafting":
            folder = "crafting/" + CRAFTING_FOLDER.get(name, "electricity")
        write_json(GEN / "data/create/recipe" / folder / f"{out_name}.json", data)
for kind, names in CREATE_NS_RECIPES.items():
    for name in names:
        src = CA_DATA / "create/recipe" / kind / f"{name}.json"
        write_json(GEN / "data/create/recipe" / kind / f"{name}.json",
                   port_recipe(fix_refs(json.loads(src.read_text(encoding="utf-8")))))

print(f"import-cca-assets: wrote {len(written)} files")
for p in written:
    print("  " + str(p.relative_to(DST)))


# ---------------------------------------------------------------- tags (merged into existing files)

TAGS = {
    "item": {
        "c:ingots/electrum": ["create:electrum_ingot"],
        "c:ingots": ["#c:ingots/electrum"],
        "c:nuggets/electrum": ["create:electrum_nugget"],
        "c:nuggets": ["#c:nuggets/electrum"],
        "c:plates/electrum": ["create:electrum_sheet"],
        "c:plates/zinc": ["create:zinc_sheet"],
        "c:plates": ["create:electrum_sheet", "create:zinc_sheet"],
        "c:rods/copper": ["create:copper_rod"],
        "c:rods/iron": ["create:iron_rod"],
        "c:rods/gold": ["create:gold_rod"],
        "c:rods/electrum": ["create:electrum_rod"],
        "c:rods/brass": ["create:brass_rod"],
        "c:rods/all_metal": ["#c:rods/copper", "#c:rods/iron", "#c:rods/gold", "#c:rods/electrum", "#c:rods/brass"],
        "c:rods": ["#c:rods/all_metal"],
        "c:wires/copper": ["create:copper_wire"],
        "c:wires/iron": ["create:iron_wire"],
        "c:wires/gold": ["create:gold_wire"],
        "c:wires/electrum": ["create:electrum_wire"],
        "c:wires/all_metal": ["#c:wires/copper", "#c:wires/iron", "#c:wires/gold", "#c:wires/electrum"],
        "c:wires": ["#c:wires/all_metal"],
        "c:dusts/diamond": ["create:diamond_grit"],
        "c:dusts": ["#c:dusts/diamond"],
        "c:storage_blocks/electrum": ["create:electrum_block"],
        "c:storage_blocks": ["#c:storage_blocks/electrum"],
        "create:sandpaper": ["create:diamond_grit_sandpaper"],
        "create:spools": ["create:copper_spool", "create:gold_spool", "create:electrum_spool", "create:festive_spool"],
        "create:large_connector_usable_rods": ["#c:rods/gold", "#c:rods/electrum"],
        "create:modular_accumulator_usable_wires": ["#c:wires/gold", "#c:wires/electrum"],
    },
    "block": {
        "c:storage_blocks/electrum": ["create:electrum_block"],
        "c:storage_blocks": ["#c:storage_blocks/electrum"],
        "minecraft:mineable/pickaxe": ["create:rolling_mill", "create:electric_motor", "create:alternator",
                                       "create:connector", "create:small_light_connector", "create:large_connector",
                                       "create:modular_accumulator", "create:creative_energy", "create:electrum_block"],
        "minecraft:mineable/axe": ["create:rolling_mill"],
        "minecraft:needs_iron_tool": ["create:electrum_block"],
        "minecraft:beacon_base_blocks": ["create:electrum_block"],
        "create:safe_nbt": ["create:creative_energy"],
    },
}

for kind, tags in TAGS.items():
    for tag, values in tags.items():
        ns, path = tag.split(":", 1)
        file = GEN / "data" / ns / "tags" / kind / f"{path}.json"
        data = json.loads(file.read_text(encoding="utf-8")) if file.exists() else {"values": []}
        changed = False
        for v in values:
            if v not in data["values"]:
                data["values"].append(v)
                changed = True
        if changed:
            write_json(file, data, allow_existing=True)
print(f"import-cca-assets: tags merged")
