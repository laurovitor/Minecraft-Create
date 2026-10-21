"""Every player-facing golem string must ship in both en_us and pt_br.

Run from the repository root: python3 checks/golem-lang/verify-golem-lang.py [built.jar]
Collects golem lang keys (source default/*.json, generated en_us, pt_br, keys referenced by golem
code and JEI), then fails if any is missing or untranslated (pt_br identical to en_us) in the
sources and, when a JAR is given, inside the packaged assets/create/lang/*.json.
Also fails on Component.literal in the golem packages (hardcoded, not localizable).
"""
import json
import re
import sys
import zipfile
from pathlib import Path

GOLEM = re.compile(r'golem')
src = Path('src/main/java/com/simibubi/create')
default = {}
for f in Path('src/main/resources/assets/create/lang/default').glob('*.json'):
    default.update(json.loads(f.read_text()))
en = json.loads(Path('src/generated/resources/assets/create/lang/en_us.json').read_text())
pt = json.loads(Path('src/main/resources/assets/create/lang/pt_br.json').read_text())

keys = {k for d in (default, en, pt) for k in d if GOLEM.search(k)}
code = [f for d in ('andesiteGolem', 'netheriteGolem', 'redstoneGolem') for f in (src / 'content/equipment' / d).glob('*.java')]
for f in code + [src / 'compat/jei/CreateJEI.java']:
    text = f.read_text()
    if f.parent.name.endswith('Golem'):
        assert 'Component.literal' not in text, f'hardcoded text in {f}'
    for key in re.findall(r'translat(?:e|able)\("([^"]+)"', text):
        if f.name == 'CreateJEI.java' and not GOLEM.search(key):
            continue
        if not key.endswith('.') and '"' not in key:
            keys.add(key if key.startswith(('create.', 'entity.', 'block.', 'item.')) else 'create.' + key)
# Clipboard state names are built as "clipboard.golem_info.state." + state.
keys |= {f'create.clipboard.golem_info.state.{s}' for s in ('sleeping', 'patrol', 'combat')}
# Tree diagnosis names: "clipboard.golem_info.tree." + AndesiteGolem.TreeStatus.key.
statuses = re.search(r'enum TreeStatus \{(.*?);', (src / 'content/equipment/andesiteGolem/AndesiteGolem.java').read_text(), re.S)
keys |= {f'create.clipboard.golem_info.tree.{k}' for k in re.findall(r'\("([a-z_]+)"\)', statuses.group(1))}
assert len([k for k in keys if '.tree.' in k]) == 9
# Redstone golem task names: "clipboard.golem_info.task." + RedstoneGolem.Task.key.
tasks = re.search(r'enum Task \{(.*?);', (src / 'content/equipment/redstoneGolem/RedstoneGolem.java').read_text(), re.S)
keys |= {f'create.clipboard.golem_info.task.{k}' for k in re.findall(r'\("([a-z_]+)"\)', tasks.group(1))}
assert len([k for k in keys if '.task.' in k]) == 5

def check(label, en_map, pt_map):
    bad = [k for k in sorted(keys) if k not in en_map or k not in pt_map or en_map[k] == pt_map[k]]
    assert not bad, f'{label}: missing/untranslated {bad}'

for k in keys:
    if k in default:
        assert default[k] == en[k], f'generated en_us drifted from default source: {k}'
check('sources', en, pt)
if len(sys.argv) > 1:
    with zipfile.ZipFile(sys.argv[1]) as jar:
        check('jar', json.loads(jar.read('assets/create/lang/en_us.json')),
              json.loads(jar.read('assets/create/lang/pt_br.json')))
for k in sorted(keys):
    print(k)
print(f'{len(keys)} golem keys present in en_us and pt_br' + (' (sources + JAR)' if len(sys.argv) > 1 else ' (sources)'))
