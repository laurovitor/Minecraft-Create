"""Offline guard for the Create Crafts & Additions content imported natively into Create (phase 1).

Run from the repository root: python3 -I checks/cca-native/verify-cca-native.py [built.jar]

Sources (always):
  - every phase-1 block/item id is registered under REGISTRATE in AllBlocks/AllItems;
  - every lang key the electricity code, JEI, ponder scenes and tooltips need is present in the generated
    en_us and in pt_br, translated (pt_br differs from en_us, small allowlist), and en_us matches
    lang/default where the key is defined there;
  - no hardcoded Component.literal text in the imported packages;
  - no `createaddition` / `com.mrh0` left in Java, assets or data of the fork.
JAR (when given):
  - item definitions, block states and block loot tables exist for every id;
  - packaged en_us/pt_br carry the same keys;
  - every `create:` item referenced by a recipe of the imported recipe folders exists in the JAR;
  - no entry name or text entry of the JAR mentions `createaddition`.
"""
import json
import re
import sys
import zipfile
from pathlib import Path

BLOCKS = ['rolling_mill', 'electric_motor', 'alternator', 'connector', 'small_light_connector',
          'large_connector', 'modular_accumulator', 'creative_energy', 'electrum_block']
ITEMS = ['electrum_ingot', 'electrum_nugget', 'electrum_sheet', 'zinc_sheet', 'copper_rod', 'iron_rod',
         'gold_rod', 'electrum_rod', 'brass_rod', 'copper_wire', 'iron_wire', 'gold_wire', 'electrum_wire',
         'diamond_grit', 'spool', 'copper_spool', 'gold_spool', 'electrum_spool', 'festive_spool',
         'capacitor', 'diamond_grit_sandpaper']
PONDER_SCENES = ['electric_motor', 'alternator', 'rolling_mill', 'automated_rolling_mill',
                 'modular_accumulator', 'connector', 'connector_remove']
# Same word in both languages.
SAME_OK = {'item.create.capacitor'}

root = Path('src/main/java/com/simibubi/create')
code_dirs = [root / 'content/electricity', root / 'content/kinetics/rollingMill']
code_files = [f for d in code_dirs for f in d.rglob('*.java')] + [
    root / 'compat/jei/category/RollingMillCategory.java',
    root / 'compat/jei/category/animations/AnimatedRollingMill.java',
    root / 'content/equipment/sandPaper/DiamondGritSandpaperItem.java',
    root / 'infrastructure/ponder/scenes/ElectricityScenes.java',
    root / 'infrastructure/config/CElectricity.java',
]
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
for s in PONDER_SCENES:
    scene = {k for k in en if k.startswith(f'create.ponder.{s}.')}
    if f'create.ponder.{s}.header' not in scene:
        fail(f'ponder scene without header: {s}')
    keys |= scene
keys |= {'create.ponder.tag.electric', 'create.ponder.tag.electric.description', 'create.recipe.rolling',
         'create.subtitle.electric_motor_buzz', 'create.subtitle.electric_charge'}
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

# 3. No upstream namespace left behind.
for base in ('src/main/java', 'src/main/resources', 'src/generated/resources'):
    for f in Path(base).rglob('*'):
        if f.is_file() and f.suffix in ('.java', '.json', '.mcmeta', '.toml', '.txt'):
            t = f.read_text(errors='ignore')
            if 'createaddition' in t or 'com.mrh0' in t:
                fail(f'upstream namespace in {f}')
        if 'createaddition' in str(f):
            fail(f'upstream namespace in path {f}')

# 4. Packaged JAR.
if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        names = set(jar.namelist())
        for i in BLOCKS + ITEMS:
            if f'assets/create/items/{i}.json' not in names:
                fail(f'jar: no item definition for {i}')
        for b in BLOCKS:
            for n in (f'assets/create/blockstates/{b}.json', f'data/create/loot_table/blocks/{b}.json'):
                if n not in names:
                    fail(f'jar: missing {n}')
        check_lang('jar', json.loads(jar.read('assets/create/lang/en_us.json')),
                   json.loads(jar.read('assets/create/lang/pt_br.json')))
        items_in_jar = {n[len('assets/create/items/'):-5] for n in names
                        if n.startswith('assets/create/items/') and n.endswith('.json')}
        recipes = [n for n in names if n.endswith('.json') and (
            n.startswith('data/create/recipe/rolling/') or n.startswith('data/create/recipe/crafting/electricity/')
            or re.search(r'/(electrum|zinc_sheet|diamond_grit|capacitor|spool|connector|accumulator|'
                         r'alternator|electric_motor|rolling_mill|creative_energy)', n) and n.startswith('data/create/recipe/'))]
        if not any(n.startswith('data/create/recipe/rolling/') for n in recipes):
            fail('jar: no rolling recipes')
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
print(f'OK: {len(BLOCKS)} blocks, {len(ITEMS)} items, {len(keys)} lang keys, {len(PONDER_SCENES)} ponder scenes')
