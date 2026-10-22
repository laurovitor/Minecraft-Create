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
         'diamond_grit', 'spool', 'copper_spool', 'gold_spool', 'electrum_spool', 'festive_spool',
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

# 4. No upstream namespace left behind.
for base in ('src/main/java', 'src/main/resources', 'src/generated/resources'):
    for f in Path(base).rglob('*'):
        if f.is_file() and f.suffix in ('.java', '.json', '.mcmeta', '.toml', '.txt'):
            t = f.read_text(errors='ignore')
            if 'createaddition' in t or 'com.mrh0' in t:
                fail(f'upstream namespace in {f}')
        if 'createaddition' in str(f):
            fail(f'upstream namespace in path {f}')

# 5. Packaged JAR.
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

if failures:
    print('\n'.join(failures))
    sys.exit(f'{len(failures)} failure(s)')
print(f'OK: {len(BLOCKS)} blocks, {len(ITEMS)} items, {len(keys)} lang keys, {ponder_scenes} ponder scenes, '
      f'{len(SOUNDS)} sounds')
