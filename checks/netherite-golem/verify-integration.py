"""Offline structural checks for wiring that requires a Minecraft runtime to exercise.
Run from repository root. These are not in-game or NBT round-trip tests.
"""
from pathlib import Path
import json

root = Path('src/main/java/com/simibubi/create')
entity = (root/'content/equipment/netheriteGolem/NetheriteGolem.java').read_text()
chest = (root/'content/equipment/netheriteGolem/NetheriteGolemChestBlock.java').read_text()
be = (root/'content/equipment/netheriteGolem/NetheriteGolemChestBlockEntity.java').read_text()
loot = (root/'content/equipment/netheriteGolem/NetheriteGolemEvents.java').read_text()
clipboard = (root/'content/equipment/netheriteGolem/NetheriteGolemClipboardInfo.java').read_text()
andesite = (root/'content/equipment/andesiteGolem/AndesiteGolem.java').read_text()
handler = (root/'content/equipment/clipboard/ClipboardValueSettingsHandler.java').read_text()
work = (root/'content/equipment/andesiteGolem/AndesiteGolemWorkGoal.java').read_text()

assert 'putLong("CreatedGameTime", createdGameTime)' in andesite
assert 'getLongOr("CreatedGameTime", level().getGameTime())' in andesite
assert 'data.putLong("CreatedGameTime", golem.getCreatedGameTime())' in chest
assert 'golem.setCreatedGameTime' in chest
assert 'output.putInt("MonsterKills", monsterKills)' in entity
assert 'input.getIntOr("MonsterKills", 0)' in entity
assert 'data.store("Home", BlockPos.CODEC, pos)' in chest
assert 'NetheriteGolemRules.killsAfterMove' in chest
assert 'NetheriteGolemRules.shouldTarget(m instanceof Enemy' in entity
assert 'chest.accepts(drop.getItem())' in loot
assert 'filtering.getFilter().isEmpty()' in be
for key in ['health', 'attack', 'armor', 'kills', 'days', 'capacity', 'used', 'free']:
    assert f'clipboard.golem_info.{key}' in clipboard
assert 'NetheriteGolemClipboardInfo.write' in handler
assert 'Blocks.DARK_OAK_LOG' in work and 'TreeCutter.findTreeForGolem(level, base' in work
recipe = json.loads(Path('src/generated/resources/data/create/recipe/pressing/netherite_ingot_from_golem_chest.json').read_text())
assert recipe['ingredients'] == ['create:netherite_golem_chest']
assert recipe['results'] == [{'count': 6, 'id': 'minecraft:netherite_ingot'}]
print('Offline integration/serialization wiring checks passed (runtime unverified)')
