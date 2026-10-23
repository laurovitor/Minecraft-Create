"""Offline guard for the Create Crafts & Additions content imported natively into Create (phases 1 and 2).

Run from the repository root: python3 -I checks/cca-native/verify-cca-native.py [built.jar]

Sources (always):
  - every imported block/item id is registered under REGISTRATE in AllBlocks/AllItems;
  - every lang key the electricity code, JEI, ponder scenes, sounds and tooltips need is present in the
    generated en_us and in pt_br, translated (pt_br differs from en_us, small allowlist), and en_us matches
    lang/default where the key is defined there;
  - every Ponder title/text in the scene sources matches its en_us key (Ponder numbers texts in call order);
  - sounds.json lists every imported sound and each file exists; every ponder schematic exists;
  - no hardcoded Component.literal text in the imported packages;
  - no `createaddition` / `com.mrh0` left in Java, assets or data of the fork.
JAR (when given):
  - item definitions, block states and block loot tables exist for every id;
  - packaged en_us/pt_br carry the same keys; sounds and ponder schematics are packaged;
  - every `create:` item referenced by an imported recipe exists in the JAR; rolling, charging and
    liquid burning recipes are present;
  - no entry name or text entry of the JAR mentions `createaddition`;
  - the MIT notice (THIRD_PARTY_NOTICES.md) is packaged.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

BLOCKS = ['rolling_mill', 'electric_motor', 'alternator', 'connector', 'small_light_connector',
          'large_connector', 'modular_accumulator', 'creative_energy', 'electrum_block',
          # phase 2
          'tesla_coil', 'servo_motor', 'electric_pump', 'redstone_relay', 'digital_adapter',
          'portable_energy_interface', 'liquid_blaze_burner', 'barbed_wire', 'copper_wire_casing']
ITEMS = ['electrum_ingot', 'electrum_nugget', 'electrum_sheet', 'zinc_sheet', 'copper_rod', 'iron_rod',
         'gold_rod', 'electrum_rod', 'brass_rod', 'copper_wire', 'iron_wire', 'gold_wire', 'electrum_wire',
         'diamond_grit', 'spool', 'copper_spool', 'iron_spool', 'gold_spool', 'electrum_spool', 'festive_spool',
         'capacitor', 'diamond_grit_sandpaper',
         # phase 2
         'straw']
SCENE_SOURCES = ['ElectricityScenes.java', 'ElectricityScenes2.java']
PONDER_SCHEMATICS = ['alternator', 'automate_rolling_mill', 'connector', 'electric_motor', 'modular_accumulator',
                     'rolling_mill', 'tesla_coil', 'tesla_coil_hurt', 'liquid_blaze_burner', 'pei_transfer',
                     'pei_redstone', 'electric_pump_flow', 'servo']
# sound event -> number of files (name, name_1, name_2, ...)
SOUNDS = {'electric_motor_buzz': 1, 'electric_charge': 1, 'tesla_coil': 1, 'loud_zap': 3, 'little_zap': 3}
RECIPE_KINDS = ['rolling', 'charging', 'liquid_burning']
# Same word in both languages.
SAME_OK = {'item.create.capacitor'}

root = Path('src/main/java/com/simibubi/create')
code_dirs = [root / 'content/electricity', root / 'content/kinetics/rollingMill']
code_files = [f for d in code_dirs for f in d.rglob('*.java')] + [
    root / 'compat/jei/category/RollingMillCategory.java',
    root / 'compat/jei/category/ChargingCategory.java',
    root / 'compat/jei/category/LiquidBurningCategory.java',
    root / 'compat/jei/category/animations/AnimatedRollingMill.java',
    root / 'compat/jei/category/animations/AnimatedTeslaCoil.java',
    root / 'content/equipment/sandPaper/DiamondGritSandpaperItem.java',
    root / 'infrastructure/config/CElectricity.java',
] + [root / 'infrastructure/ponder/scenes' / s for s in SCENE_SOURCES]
failures = []


def fail(msg):
    failures.append(msg)


# 1. Registration.
registered = set(re.findall(r'\.(?:block|item)\(\s*"([a-z_]+)"', (root / 'AllBlocks.java').read_text()))
items_src = (root / 'AllItems.java').read_text()
registered |= set(re.findall(r'(?:item|taggedIngredient|ingredient)\(\s*"([a-z_]+)"', items_src))
for i in BLOCKS + ITEMS:
    if i not in registered:
        fail(f'not registered: create:{i}')

# 2. Lang keys.
default = {}
for f in Path('src/main/resources/assets/create/lang/default').glob('*.json'):
    default.update(json.loads(f.read_text()))
en = json.loads(Path('src/generated/resources/assets/create/lang/en_us.json').read_text())
pt = json.loads(Path('src/main/resources/assets/create/lang/pt_br.json').read_text())

keys = {f'block.create.{b}' for b in BLOCKS} | {f'item.create.{i}' for i in ITEMS}
keys |= {k for k in set(en) | set(pt) | set(default)
         if re.match(r'(block|item)\.create\.(%s)\.tooltip\.' % '|'.join(BLOCKS + ITEMS), k)}
ponder_scenes = 0
for source in SCENE_SOURCES:
    text = (root / 'infrastructure/ponder/scenes' / source).read_text()
    for method in re.split(r'\n\tpublic static void ', text)[1:]:
        title = re.search(r'scene\.title\("([a-z_]+)", "([^"]+)"\)', method)
        if not title:
            continue
        ponder_scenes += 1
        expected = {f'create.ponder.{title.group(1)}.header': title.group(2)}
        for i, line in enumerate(re.findall(r'\.text\("([^"]+)"\)', method), 1):
            expected[f'create.ponder.{title.group(1)}.text_{i}'] = line
        for k, v in expected.items():
            if en.get(k) != v:
                fail(f'ponder text out of sync with {source}: {k}')
        keys |= set(expected)
keys |= {'create.ponder.tag.electric', 'create.ponder.tag.electric.description',
         'effect.create.shocking', 'create.recipe.assembly.charging'}
keys |= {f'create.recipe.{k}' for k in RECIPE_KINDS}
keys |= {'create.display_source.modular_accumulator', 'create.display_source.digital_adapter'}
keys |= {f'create.subtitle.{s}' for s in SOUNDS}
keys |= {f'death.attack.create.{d}{p}' for d in ('barbed_wire', 'tesla_coil') for p in ('', '.player')}
for f in code_files:
    text = f.read_text()
    for k in re.findall(r'translatable\("([a-z0-9_.]+)"', text):
        keys.add(k)
    for k in re.findall(r'translatable\(Create\.ID \+ "(\.[a-z0-9_.]+)"', text):
        keys.add('create' + k)
    for k in re.findall(r'CreateLang\.translate(?:Direct)?\("([a-z0-9_.]+)"', text):
        keys.add('create.' + k)
    for lit in re.findall(r'Component\.literal\("([^"]*[A-Za-z]{2,}[^"]*)"', text):
        fail(f'hardcoded text {lit!r} in {f}')


def check_lang(label, en_map, pt_map):
    for k in sorted(keys):
        if k not in en_map:
            fail(f'{label}: en_us missing {k}')
        elif k not in pt_map:
            fail(f'{label}: pt_br missing {k}')
        elif en_map[k] == pt_map[k] and k not in SAME_OK and not re.fullmatch(r'[\W\d_]*', en_map[k]):
            fail(f'{label}: pt_br not translated {k} = {en_map[k]!r}')


for k in keys:
    if k in default and default[k] != en.get(k):
        fail(f'generated en_us drifted from lang/default: {k}')
check_lang('sources', en, pt)

# 3. Sounds and ponder schematics.
sounds = json.loads(Path('src/generated/resources/assets/create/sounds.json').read_text())
for name, count in SOUNDS.items():
    files = [e['name'] for e in sounds.get(name, {}).get('sounds', [])]
    if len(files) != count:
        fail(f'sounds.json: {name} has {len(files)} file(s), expected {count}')
    for f in files:
        if not Path('src/main/resources/assets/create/sounds', f.split(':')[1] + '.ogg').exists():
            fail(f'missing sound file {f}')
for name in PONDER_SCHEMATICS:
    if not Path('src/main/resources/assets/create/ponder', name + '.nbt').exists():
        fail(f'missing ponder schematic {name}')

# 3b. Client assets resolve: item definitions and block states -> models -> parents/textures, and the
# partial models Java loads. A missing file is only a log error plus a magenta cube in game, so check here.
ASSET_ROOTS = [Path('src/main/resources/assets/create'), Path('src/generated/resources/assets/create')]


def asset(kind, rel):
    return next((r / kind / rel for r in ASSET_ROOTS if (r / kind / rel).exists()), None)


def model_refs(obj):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == 'model' and isinstance(v, str):
                yield v
            else:
                yield from model_refs(v)
    elif isinstance(obj, list):
        for v in obj:
            yield from model_refs(v)


seen_models = set()


def check_model(ref, owner):
    if ':' not in ref:
        ref = 'minecraft:' + ref
    ns, path = ref.split(':', 1)
    if ns != 'create' or ref in seen_models:
        return
    seen_models.add(ref)
    f = asset('models', path + '.json')
    if f is None:
        fail(f'missing model {ref} (used by {owner})')
        return
    model = json.loads(f.read_text())
    if 'parent' in model:
        check_model(model['parent'], ref)
    for tex in model.get('textures', {}).values():
        if isinstance(tex, str) and not tex.startswith('#'):
            tns, tpath = (tex if ':' in tex else 'minecraft:' + tex).split(':', 1)
            if tns == 'create' and asset('textures', tpath + '.png') is None:
                fail(f'missing texture {tex} (used by {ref})')


for i in BLOCKS + ITEMS:
    f = asset('items', i + '.json')
    if f is not None:
        for ref in model_refs(json.loads(f.read_text())):
            check_model(ref, f'items/{i}')
for b in BLOCKS:
    f = asset('blockstates', b + '.json')
    if f is None:
        fail(f'missing blockstate {b}')
        continue
    for ref in model_refs(json.loads(f.read_text())):
        check_model(ref, f'blockstates/{b}')
partials = (root / 'AllPartialModels.java').read_text()
for path in re.findall(r'block\("((?:%s)[a-z_/]*)"\)' % '|'.join(BLOCKS), partials):
    check_model('create:block/' + path, 'AllPartialModels')

# 4. Imported recipes use 26.3 formats (a 1.21.1 recipe makes the server refuse to load the datapack).
recipe_root = Path('src/generated/resources/data/create/recipe')
imported_recipes = [f for k in RECIPE_KINDS for f in (recipe_root / k).glob('*.json')]
imported_recipes += list((recipe_root / 'crafting/electricity').glob('*.json'))
imported_recipes += [recipe_root / p for p in ('crafting/barbed_wire.json', 'crafting/digital_adapter.json',
                                               'crafting/portable_energy_interface.json', 'crafting/redstone_relay.json',
                                               'crafting/servo_motor.json', 'mechanical_crafting/electric_pump.json',
                                               'mechanical_crafting/tesla_coil.json')]


def old_format(recipe):
    ingredients = list(recipe.get('ingredients', [])) + list(recipe.get('key', {}).values())
    for i in ingredients:
        if isinstance(i, dict) and ({'item', 'tag', 'fluid'} & set(i) or i.get('type') == 'neoforge:tag'):
            return True
    for r in recipe.get('results', []) + [recipe.get('result') or {}]:
        for v in r.get('components', {}).values():
            if isinstance(v, dict) and 'levels' in v:
                return True
    return False


for f in imported_recipes:
    if old_format(json.loads(f.read_text())):
        fail(f'1.21.1 recipe format left in {f}')

# 5. No upstream namespace left behind.
for base in ('src/main/java', 'src/main/resources', 'src/generated/resources'):
    for f in Path(base).rglob('*'):
        if f.is_file() and f.suffix in ('.java', '.json', '.mcmeta', '.toml', '.txt'):
            t = f.read_text(errors='ignore')
            if 'createaddition' in t or 'com.mrh0' in t:
                fail(f'upstream namespace in {f}')
        if 'createaddition' in str(f):
            fail(f'upstream namespace in path {f}')

# 6. Packaged JAR.
if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        names = set(jar.namelist())
        if b'Copyright (c) 2024 MRH' not in jar.read('Create_THIRD_PARTY_NOTICES.md'):
            fail('jar: Create Crafts & Additions MIT notice missing')
        for i in BLOCKS + ITEMS:
            if f'assets/create/items/{i}.json' not in names:
                fail(f'jar: no item definition for {i}')
        for b in BLOCKS:
            for n in (f'assets/create/blockstates/{b}.json', f'data/create/loot_table/blocks/{b}.json'):
                if n not in names:
                    fail(f'jar: missing {n}')
        for s, count in SOUNDS.items():
            for suffix in [''] + [f'_{k}' for k in range(1, count)]:
                if f'assets/create/sounds/{s}{suffix}.ogg' not in names:
                    fail(f'jar: missing sound {s}{suffix}')
        for s in PONDER_SCHEMATICS:
            if f'assets/create/ponder/{s}.nbt' not in names:
                fail(f'jar: missing ponder schematic {s}')
        check_lang('jar', json.loads(jar.read('assets/create/lang/en_us.json')),
                   json.loads(jar.read('assets/create/lang/pt_br.json')))
        items_in_jar = {n[len('assets/create/items/'):-5] for n in names
                        if n.startswith('assets/create/items/') and n.endswith('.json')}
        imported = '|'.join(BLOCKS + ITEMS + ['electrum', 'spool', 'accumulator', 'bamboo', 'paper'])
        recipes = sorted(n for n in names if n.endswith('.json') and n.startswith('data/create/recipe/') and (
            any(n.startswith(f'data/create/recipe/{k}/') for k in RECIPE_KINDS)
            or n.startswith('data/create/recipe/crafting/electricity/')
            or re.search(r'/(%s)[a-z0-9_]*\.json$' % imported, n)))
        for kind in RECIPE_KINDS:
            if not any(n.startswith(f'data/create/recipe/{kind}/') for n in recipes):
                fail(f'jar: no {kind} recipes')
        for n in recipes:
            text = jar.read(n).decode()
            json.loads(text)
            for ref in re.findall(r'"(?:item|id)"\s*:\s*"create:([a-z0-9_/]+)"', text):
                if ref not in items_in_jar:
                    fail(f'jar: {n} references unknown create:{ref}')
        for n in names:
            if 'createaddition' in n:
                fail(f'jar: upstream namespace in entry {n}')
            elif n.endswith(('.json', '.mcmeta', '.toml')) and b'createaddition' in jar.read(n):
                fail(f'jar: upstream namespace inside {n}')
        print(f'jar: {len(recipes)} imported recipes checked')

        # Client assets of the whole JAR: a model whose texture is absent or not stitched into an atlas
        # renders as the purple/black "missing" texture in game.
        def jtex(ref):
            ns, path = (ref if ':' in ref else 'minecraft:' + ref).split(':', 1)
            return ns, path

        atlas = set()
        for a in ('blocks', 'items'):
            n = f'assets/minecraft/atlases/{a}.json'
            if n in names:
                atlas |= {s.get('resource') for s in json.loads(jar.read(n))['sources']}
        # The vanilla atlases stitch the block/ and item/ directories; anything else must be listed.
        stitched_dirs = ('block/', 'item/')
        # Pre-existing Create core gaps outside the CC&A scope (cardboard armor trim permutations and the
        # train track overlay), reported separately, not part of this guard.
        known_gaps = ('trims/items/', 'block/track')
        jar_models = [n for n in names if n.startswith('assets/create/models/') and n.endswith('.json')]
        textures_checked = 0
        for n in jar_models:
            model = json.loads(jar.read(n))
            parent = model.get('parent')
            if isinstance(parent, str) and not parent.startswith('#'):
                pns, ppath = jtex(parent)
                if pns == 'create' and f'assets/create/models/{ppath}.json' not in names:
                    fail(f'jar: {n} has missing parent {parent}')
            for tex in (model.get('textures') or {}).values():
                if not isinstance(tex, str) or tex.startswith('#'):
                    continue
                tns, tpath = jtex(tex)
                if tns != 'create' or tpath.startswith(known_gaps):
                    continue
                textures_checked += 1
                if f'assets/create/textures/{tpath}.png' not in names:
                    fail(f'jar: missing texture {tex} (used by {n})')
                elif not tpath.startswith(stitched_dirs) and f'create:{tpath}' not in atlas:
                    fail(f'jar: texture {tex} (used by {n}) is not in any atlas')
        # Item definitions use the 26.x format ({"model": {"type": ...}}) and point at existing models.
        for n in names:
            if n.startswith('assets/create/items/') and n.endswith('.json'):
                d = json.loads(jar.read(n))
                if not isinstance(d.get('model'), dict) or 'type' not in d['model']:
                    fail(f'jar: {n} is not a 26.x item definition')
                    continue
                for ref in model_refs(d['model']):
                    mns, mpath = jtex(ref)
                    if mns == 'create' and f'assets/create/models/{mpath}.json' not in names:
                        fail(f'jar: {n} points at missing model {ref}')
        # Fluid models are built from fluid/<name>_still and _flow (AllFluidsClient.tintedModel).
        fluids = re.findall(r'tintedModel\("([a-z_]+)"', (root / 'AllFluids.java').read_text())
        for fl in fluids:
            for part in ('still', 'flow'):
                tpath = f'fluid/{fl}_{part}'
                textures_checked += 1
                if f'assets/create/textures/{tpath}.png' not in names:
                    fail(f'jar: missing fluid texture {tpath}')
                elif f'create:{tpath}' not in atlas:
                    fail(f'jar: fluid texture {tpath} is not in the blocks atlas')
        print(f'jar: {len(jar_models)} models, {textures_checked} texture references, {len(fluids)} fluids checked')

if failures:
    print('\n'.join(failures))
    sys.exit(f'{len(failures)} failure(s)')
print(f'OK: {len(BLOCKS)} blocks, {len(ITEMS)} items, {len(seen_models)} models, {len(keys)} lang keys, {ponder_scenes} ponder scenes, '
      f'{len(SOUNDS)} sounds')
