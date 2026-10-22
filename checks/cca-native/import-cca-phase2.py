#!/usr/bin/env python3
"""Phase-2 importer for Create Crafts & Additions (MIT, by MRH0) content in the Create fork.

The phase-2 models, textures, block states, loot tables and recipes were copied by an earlier run;
this script adds what that run left out and can be re-run safely (idempotent):
  - ponder schematics for the phase-2 scenes, renamed to the `create:` namespace;
  - the phase-2 recipes, re-ported from upstream to the 26.3 formats (string item ingredients,
    `{amount, ingredient}` fluid ingredients, unwrapped enchantment components);
  - the `has_fluid_tag` conditions of liquid burning recipes, as NeoForge `not(tag_empty)` on fluids;
  - block tags (pickaxe, fan catalysts/transparency, casing) for the phase-2 blocks;
  - an item application recipe for the Copper Wire Casing (upstream never registered one).

Usage: python3 -I checks/cca-native/import-cca-phase2.py <createaddition clone> <create fork root>
"""
import gzip
import io
import json
import struct
import sys
from pathlib import Path

SRC = Path(sys.argv[1])
DST = Path(sys.argv[2])
CA_MAIN = SRC / "src/main/resources/assets/createaddition"
CA_DATA = SRC / "src/generated/resources/data/createaddition"
MAIN = DST / "src/main/resources"
GEN = DST / "src/generated/resources"


def fail(msg):
    sys.exit("import-cca-phase2: " + msg)


def rename_ns(text):
    return text.replace("createaddition:", "create:")


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


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



PONDER = ["tesla_coil", "tesla_coil_hurt", "liquid_blaze_burner", "pei_transfer", "pei_redstone",
          "electric_pump_flow", "servo"]
for name in PONDER:
    raw = gzip.decompress((CA_MAIN / "ponder" / f"{name}.nbt").read_bytes())
    t, root, value = nbt_read(io.BytesIO(raw))
    out = io.BytesIO()
    nbt_write(out, t, root, nbt_rename(t, value))
    if b"createaddition" in out.getvalue():
        fail(f"ponder {name}: namespace left after rename")
    dst = MAIN / "assets/create/ponder" / f"{name}.nbt"
    data = gzip.compress(out.getvalue(), mtime=0)
    if dst.exists() and dst.read_bytes() != data:
        fail(f"collision {dst}")
    dst.write_bytes(data)
print(f"import-cca-phase2: {len(PONDER)} ponder schematics")

def item_ingredient(v):
    """1.21.1 item ingredient objects to the 26.x string form ("id", "#tag", or a list of those)."""
    if isinstance(v, list):
        return [item_ingredient(i) for i in v]
    if isinstance(v, dict):
        if set(v) == {"item"}:
            return rename_ns(v["item"])
        if set(v) == {"tag"}:
            return "#" + v["tag"]
        fail(f"unsupported ingredient {v}")
    return rename_ns(v)


def fluid_ingredient(v):
    """NeoForge 21.1 sized fluid ingredients to the 26.x {amount, ingredient} form."""
    if v.get("type") == "neoforge:tag":
        return {"amount": v["amount"], "ingredient": "#" + v["tag"]}
    if set(v) == {"fluid", "amount"}:
        return {"amount": v["amount"], "ingredient": rename_ns(v["fluid"])}
    fail(f"unsupported fluid ingredient {v}")


def components(c):
    """Item components whose codec changed: stored/applied enchantments lost their "levels" wrapper."""
    out = {}
    for k, v in c.items():
        if k in ("minecraft:stored_enchantments", "minecraft:enchantments") and isinstance(v, dict) and "levels" in v:
            v = v["levels"]
        out[k] = v
    return out


def port_recipe(data, fluid=False):
    data = json.loads(rename_ns(json.dumps(data)))
    if "key" in data:
        data["key"] = {k: item_ingredient(v) for k, v in data["key"].items()}
    if "ingredients" in data:
        data["ingredients"] = [fluid_ingredient(i) if fluid else item_ingredient(i) for i in data["ingredients"]]
    for result in data.get("results", []) + ([data["result"]] if isinstance(data.get("result"), dict) else []):
        if "components" in result:
            result["components"] = components(result["components"])
    conditions = []
    for c in data.get("neoforge:conditions", []):
        if c.get("type") == "create:has_fluid_tag":
            # Upstream's own condition; NeoForge's tag_empty does the same on the fluid registry.
            c = {"type": "neoforge:not", "value": {"type": "neoforge:tag_empty", "registry": "minecraft:fluid",
                                                   "tag": c["fluidTag"]}}
        conditions.append(c)
    if fluid:
        # 26.3 refuses a recipe whose fluid tag does not exist, so every fuel from another mod (or from
        # the phase-3 fluids) waits for its tag to be filled; upstream only gated some of them.
        for i in data["ingredients"]:
            tag = i["ingredient"][1:] if i["ingredient"].startswith("#") else None
            if tag and not tag.startswith("minecraft:") and not any(
                    c.get("value", {}).get("tag") == tag for c in conditions):
                conditions.append({"type": "neoforge:not", "value": {"type": "neoforge:tag_empty",
                                                                      "registry": "minecraft:fluid", "tag": tag}})
    if conditions:
        data = {"neoforge:conditions": conditions, **{k: v for k, v in data.items() if k != "neoforge:conditions"}}
    if "create" + "addition" in json.dumps(data):
        fail(f"namespace left in recipe {data}")
    return data


RECIPES = {
    "charging": [p.stem for p in sorted((CA_DATA / "recipe/charging").glob("*.json"))],
    "liquid_burning": [p.stem for p in sorted((CA_DATA / "recipe/liquid_burning").glob("*.json"))],
    "crafting": ["barbed_wire", "digital_adapter", "portable_energy_interface", "redstone_relay", "servo_motor"],
    "mechanical_crafting": ["electric_pump", "tesla_coil"],
}
count = 0
for kind, names in RECIPES.items():
    for name in names:
        data = json.loads((CA_DATA / "recipe" / kind / f"{name}.json").read_text(encoding="utf-8"))
        write_json(GEN / "data/create/recipe" / kind / f"{name}.json", port_recipe(data, fluid=kind == "liquid_burning"))
        count += 1
# Upstream ships the straw rolling recipes under the create namespace already.
for name in ["bamboo", "paper"]:
    data = json.loads((SRC / "src/generated/resources/data/create/recipe/rolling" / f"{name}.json").read_text(encoding="utf-8"))
    write_json(GEN / "data/create/recipe/rolling" / f"{name}.json", port_recipe(data))
    count += 1
print(f"import-cca-phase2: {count} recipes ported")

write_json(GEN / "data/create/recipe/item_application/copper_wire_casing.json", {
    "type": "create:item_application",
    "ingredients": ["create:copper_casing", "#c:wires/copper"],
    "results": [{"id": "create:copper_wire_casing"}],
})

PHASE2_BLOCKS = ["tesla_coil", "servo_motor", "electric_pump", "redstone_relay", "digital_adapter",
                 "portable_energy_interface", "liquid_blaze_burner", "barbed_wire", "copper_wire_casing"]
TAGS = {
    "minecraft:mineable/pickaxe": [f"create:{b}" for b in PHASE2_BLOCKS],
    "create:fan_processing_catalysts/blasting": ["create:liquid_blaze_burner"],
    "create:fan_transparent": ["create:liquid_blaze_burner"],
    "create:casing": ["create:copper_wire_casing"],
}
for tag, values in TAGS.items():
    ns, path = tag.split(":", 1)
    file = GEN / "data" / ns / "tags/block" / f"{path}.json"
    data = json.loads(file.read_text(encoding="utf-8")) if file.exists() else {"values": []}
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write_json(file, data)
item_casing = GEN / "data/create/tags/item/casing.json"
data = json.loads(item_casing.read_text(encoding="utf-8"))
if "create:copper_wire_casing" not in data["values"]:
    data["values"].append("create:copper_wire_casing")
write_json(item_casing, data)
print("import-cca-phase2: tags merged")
