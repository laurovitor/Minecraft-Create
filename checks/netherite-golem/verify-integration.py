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
# Giant/mod trees: NaturalDarkOakCheck ports these rules; fail if the real code drifts.
for line in ['below.is(BlockTags.GRASS_BLOCKS)', 'below.is(BlockTags.MOSS_BLOCKS)', 'below.is(BlockTags.SUBSTRATE_OVERWORLD)',
             'below.is(BlockTags.BASE_STONE_OVERWORLD)', 'if (below.is(BlockTags.LOGS) || !isTreeGround(below))',
             'APPROACH_UP = 8', 'sameWood(log, golem.level().getBlockState(p))', 'MAX_APPROACH_PATHS',
             'feetY - base.getY() <= PERIMETER_UP', 'String wood = GolemWorkGeometry.woodOf(path);']:
    assert line in work, line
assert 'below.is(BlockTags.DIRT) || below.is(Blocks.MUD) || isMangroveRoot(below)))' not in work
# NaturalDarkOakCheck ports these TreeCutter lines; fail if they drift.
for line in ['visited.add(pos);', 'BlockPos.betweenClosedStream(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))',
             'forNeighbours(currentPos, visited, SearchDirection.UP', 'getLeafDistance(state) > prevLeafDistance']:
    assert line in cutter, line
rest = (root/'content/equipment/netheriteGolem/NetheriteGolemRestGoal.java').read_text()
renderer = (root/'content/equipment/netheriteGolem/NetheriteGolemRenderer.java').read_text()
assert 'output.putLong("AwayTicks", awayTicks)' in entity and 'input.getLongOr("AwayTicks", 0L)' in entity
assert 'builder.define(COMBAT, false)' in entity and 'entityData.set(COMBAT' in entity
assert 'new NetheriteGolemRestGoal(this)' in entity and 'setSleeping(day && getTarget() == null)' not in entity
# Sleep regen and chest repair live in AndesiteGolem.tickCare for both golems (no double regen).
assert 'sleepRegen(true, getMaxHealth())' in andesite and 'sleepRegen' not in entity
assert 'tickCare(serverLevel)' in andesite and 'chest.takeOne(this::isRepairItem)' in andesite
assert 'isShiftKeyDown() && AllItems.ANDESITE_ALLOY' not in andesite and 'held.consume(1, player)' in andesite
assert andesite.index('isCopperCareTool(held)') < andesite.index('isRepairItem(held)')
assert 'stack.is(Items.NETHERITE_INGOT)' in entity and 'return isInCombat();' in entity
assert 'getSensing().hasLineOfSight(m)' in entity and 'keepUnseenTarget' in entity
assert 'WAKE_AFTER_HURT_TICKS' in entity and 'teleportTo(at.x, at.y, at.z)' in entity
assert 'golem.setSleeping(true)' in rest and 'getNavigation().isDone()' in rest
assert 'netherite_golem_eyes_white.png' in renderer and 'Eyes.CLOSED ? 0.0F' in renderer
assert Path('src/main/resources/assets/create/textures/entity/netherite_golem/netherite_golem_eyes_white.png').exists()
for f in ['src/main/resources/assets/create/lang/pt_br.json', 'src/generated/resources/assets/create/lang/en_us.json']:
    lang = json.loads(Path(f).read_text())
    for k in ['', '.sleeping', '.patrol', '.combat']:
        assert 'create.clipboard.golem_info.state' + k in lang, (f, k)
assert 'clipboard.golem_info.state' in clipboard
# Not copper: no vanilla CopperGolem inheritance/weathering, honeycomb/axe refused, chests not tool-modifiable.
golem_dir = [root/'content/equipment/andesiteGolem', root/'content/equipment/netheriteGolem']
for d in golem_dir:
    for f in d.glob('*.java'):
        src = f.read_text()
        assert 'extends CopperGolem ' not in src and 'extends CopperGolem{' not in src, f
        for bad in ['WeatheringCopper', 'nextWeatheringTick', 'updateWeathering', 'DATA_WEATHER_STATE', 'CopperGolemOxidation']:
            assert bad not in src, (f, bad)
assert 'extends AbstractGolem' in andesite and 'class NetheriteGolem extends AndesiteGolem' in entity
assert 'if (isCopperCareTool(held))\n\t\t\treturn InteractionResult.FAIL;' in andesite
assert 'Items.HONEYCOMB' in andesite and 'ItemTags.AXES' in andesite
assert entity.index('return super.mobInteract(player, hand)') > 0  # Netherite falls through to the guard
for f in ['content/equipment/andesiteGolem/AndesiteGolemChestBlock.java', 'content/equipment/netheriteGolem/NetheriteGolemChestBlock.java']:
    assert 'getToolModifiedState' in (root/f).read_text() and 'boolean simulate) {\n\t\treturn null;' in (root/f).read_text(), f
for dm in Path('src').rglob('data_maps/block/*.json'):
    assert 'golem_chest' not in dm.read_text(), dm
# Creeper vs Netherite golem: blocks spared only for creepers in golem combat; fear; knockback.
assert 'ExplosionEvent.Detonate' in loot and 'getDirectSourceEntity() instanceof Creeper creeper && inGolemCombat(creeper)' in loot
assert 'event.getAffectedBlocks().clear()' in loot and 'getAffectedEntities' not in loot
assert 'getPersistentData().putLong(NetheriteGolemRules.CREEPER_COMBAT_KEY' in loot
assert entity.count('NetheriteGolemEvents.markCombat(creeper)') == 2  # while targeted + on every hit
# Round 7 scope: no creeper fear, no extra knockback/fuse change in the Netherite golem package
# (AndesiteGolemEvents has its own, older EntityJoinLevelEvent use and is not touched).
for d in [root/'content/equipment/netheriteGolem']:
    for f in d.glob('*.java'):
        src = f.read_text()
        for gone in ['AvoidEntityGoal', 'AvoidGolemGoal', 'EntityJoinLevelEvent', '.knockback(', 'CREEPER_KNOCKBACK',
                     'setSwellDir', 'knockbackDistance', 'goalSelector.addGoal(3']:
            assert gone not in src, (f.name, gone)
recipe = json.loads(Path('src/generated/resources/data/create/recipe/pressing/netherite_ingot_from_golem_chest.json').read_text())
assert recipe['ingredients'] == ['create:netherite_golem_chest']
assert recipe['results'] == [{'count': 6, 'id': 'minecraft:netherite_ingot'}]
print('Offline integration/serialization wiring checks passed (runtime unverified)')
