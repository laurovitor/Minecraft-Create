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
# Dark oak: no unmasked special case (it never returned the base log); footprint always cut.
assert 'Blocks.DARK_OAK_LOG' not in work and 'TreeCutter.findTreeForGolem(masked, base' in work
assert 'for (BlockPos log : footprint)\n\t\t\tBlockHelper.destroyBlockAs' in work
cutter = (root/'content/kinetics/saw/TreeCutter.java').read_text()
# NaturalDarkOakCheck ports these TreeCutter lines; fail if they drift.
for line in ['visited.add(pos);', 'BlockPos.betweenClosedStream(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))',
             'forNeighbours(currentPos, visited, SearchDirection.UP', 'getLeafDistance(state) > prevLeafDistance']:
    assert line in cutter, line
rest = (root/'content/equipment/netheriteGolem/NetheriteGolemRestGoal.java').read_text()
renderer = (root/'content/equipment/netheriteGolem/NetheriteGolemRenderer.java').read_text()
assert 'output.putLong("AwayTicks", awayTicks)' in entity and 'input.getLongOr("AwayTicks", 0L)' in entity
assert 'builder.define(COMBAT, false)' in entity and 'entityData.set(COMBAT' in entity
assert 'new NetheriteGolemRestGoal(this)' in entity and 'setSleeping(day && getTarget() == null)' not in entity
assert 'sleepRegen(true, getMaxHealth())' in entity and 'if (isAsleep()) {' in entity
assert 'WAKE_AFTER_HURT_TICKS' in entity and 'teleportTo(at.x, at.y, at.z)' in entity
assert 'golem.setSleeping(true)' in rest and 'getNavigation().isDone()' in rest
assert 'netherite_golem_eyes_white.png' in renderer and 'Eyes.CLOSED ? 0.0F' in renderer
assert Path('src/main/resources/assets/create/textures/entity/netherite_golem/netherite_golem_eyes_white.png').exists()
for f in ['src/main/resources/assets/create/lang/pt_br.json', 'src/generated/resources/assets/create/lang/en_us.json']:
    lang = json.loads(Path(f).read_text())
    for k in ['', '.sleeping', '.patrol', '.combat']:
        assert 'create.clipboard.golem_info.state' + k in lang, (f, k)
assert 'clipboard.golem_info.state' in clipboard
recipe = json.loads(Path('src/generated/resources/data/create/recipe/pressing/netherite_ingot_from_golem_chest.json').read_text())
assert recipe['ingredients'] == ['create:netherite_golem_chest']
assert recipe['results'] == [{'count': 6, 'id': 'minecraft:netherite_ingot'}]
print('Offline integration/serialization wiring checks passed (runtime unverified)')
