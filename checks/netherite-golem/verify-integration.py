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

# ---- Redstone golem (structural; runtime unverified)
rg = root/'content/equipment/redstoneGolem'
rg_entity = (rg/'RedstoneGolem.java').read_text()
rg_goal = (rg/'RedstoneGolemCollectGoal.java').read_text()
rg_net = (rg/'RedstoneGolemNetwork.java').read_text()
rg_be = (rg/'RedstoneGolemChestBlockEntity.java').read_text()
rg_block = (rg/'RedstoneGolemChestBlock.java').read_text()
# only its own job: no sleep, regen, repair, combat, filter or work goals of the other golems
for gone in ['SleepGoal', 'MeleeAttackGoal', 'GuardGoal', 'WorkGoal', 'StrollGoal', 'FilteringBehaviour', 'filtering']:
    for src in (rg_entity, rg_goal, rg_be, rg_block):
        assert gone not in src, gone
assert 'public boolean isRepairItem(ItemStack stack) {\n\t\treturn stack.is(Items.REDSTONE);' in rg_entity  # round 15
assert 'new RedstoneGolemCollectGoal(this)' in rg_entity and 'RedstoneGolemRules.MAX_HEALTH' in rg_entity
# network: Create logistics frequency, Stock Links only, attached block, own chest excluded, chests/golem chests
assert 'new LogisticallyLinkedBehaviour(this, false)' in rg_be and 'return true;' in rg_be
assert 'LogisticallyLinkedBehaviour.getAllPresent(home.link.freqId, false)' in rg_net
assert 'link.blockEntity instanceof PackagerLinkBlockEntity stockLink' in rg_net
assert '.relative(PackagerLinkBlock.getConnectedDirection(stockLink.getBlockState()).getOpposite())' in rg_net
assert 'seen.add(homePos); // its own chest is never a source' in rg_net
assert 'instanceof AndesiteGolemChestBlockEntity golemChest' in rg_net and 'ChestBlock.getContainer(chest, state, level, pos, true)' in rg_net
assert 'RedstoneGolemRules.inRange(' in rg_net
# behaviour: fullest-first, 4 slots, run there, stop when full
assert 'RedstoneGolemRules.chooseSource(candidates)' in rg_goal and 'RedstoneGolemRules.pickupSlots(occupied' in rg_goal
assert 'walkTo(source, RedstoneGolemRules.RUN_SPEED)' in rg_goal and 'transferableSlots(s.container(), chest)' in rg_goal
# Round 11: both legs target reachable standing spots with 3D arrival and no partial roof paths.
rg_nav = (rg/'RedstoneGolemNavigation.java').read_text()
rg_barriers = (rg/'RedstoneGolemBarriers.java').read_text()
for required in ['standingSpots(chestPos)', 'path.canReach()', 'BlockHitResult hit',
                 'validStandSpot(', 'position().distanceTo(Vec3.atBottomCenterOf(standSpot))',
                 'position().distanceTo(Vec3.atCenterOf(chestPos))', 'returnCooldown = 100']:
    assert required in rg_goal, required
assert 'walkTo(chest.getBlockPos(), RedstoneGolemRules.WALK_SPEED)' in rg_goal
assert 'new RedstoneGolemNavigation(this, level)' in rg_entity and 'barriers.tick()' in rg_entity
assert 'Attributes.FOLLOW_RANGE, 96.0' in rg_entity and 'setMaxVisitedNodesMultiplier(4)' in rg_nav
for required in ['DoorBlock', 'FenceGateBlock', 'TrapDoorBlock', 'PathType.WALKABLE_DOOR', 'PathType.BLOCKED']:
    assert required in rg_nav and required in rg_barriers or required in rg_nav, required
assert 'setCanOpenDoors(true)' in rg_nav and 'setCanPassDoors(true)' in rg_nav
assert 'opened' in rg_barriers and 'passed' in rg_barriers and 'close(pos)' in rg_barriers
# tuning a Stock Link on the chest instead of opening it; network survives packing
assert 'instanceof com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem)\n\t\t\treturn InteractionResult.PASS;' in rg_block
assert 'data.store("Freq"' in rg_block and 'chest.link.freqId = freq' in rg_block
mixin = Path('src/main/java/com/simibubi/create/foundation/mixin/CarvedPumpkinBlockMixin.java').read_text()
assert 'RedstoneGolemSpawning.trySpawn(level, topPos)' in mixin and 'Blocks.REDSTONE_BLOCK' in mixin
assert 'Blocks.REDSTONE_BLOCK' in (rg/'RedstoneGolemSpawning.java').read_text()
for reg, needle in [('AllBlocks.java', 'REDSTONE_GOLEM_CHEST ='), ('AllItems.java', 'PACKED_REDSTONE_GOLEM_CHEST ='),
                    ('AllEntityTypes.java', 'REDSTONE_GOLEM ='), ('AllBlockEntityTypes.java', 'REDSTONE_GOLEM_CHEST = REGISTRATE'),
                    ('compat/jei/CreateJEI.java', 'create.jei.redstone_golem.info'),
                    ('foundation/events/CommonEvents.java', 'RedstoneGolemChestBlockEntity.registerCapabilities(event)'),
                    ('content/logistics/packager/AllInventoryIdentifiers.java', 'AllBlocks.REDSTONE_GOLEM_CHEST.get()'),
                    ('content/equipment/clipboard/ClipboardValueSettingsHandler.java', 'RedstoneGolemClipboardInfo.write')]:
    assert needle in (root/reg).read_text(), (reg, needle)
handler_src = (root/'content/equipment/clipboard/ClipboardValueSettingsHandler.java').read_text()
assert handler_src.index('RedstoneGolemChestBlockEntity redstoneChest') < handler_src.index('AndesiteGolemChestBlockEntity chest'), 'subclass branch first'
rrecipe = json.loads(Path('src/generated/resources/data/create/recipe/pressing/redstone_from_golem_chest.json').read_text())
assert rrecipe['ingredients'] == ['create:redstone_golem_chest'] and rrecipe['results'] == [{'count': 6, 'id': 'minecraft:redstone'}]
assert 'create:redstone_golem_chest' in Path('src/generated/resources/data/minecraft/tags/block/mineable/pickaxe.json').read_text()
for f in ['assets/create/blockstates/redstone_golem_chest.json', 'assets/create/items/redstone_golem_chest.json',
          'assets/create/items/redstone_golem_spawn_egg.json', 'assets/create/items/packed_redstone_golem_chest.json',
          'data/create/loot_table/blocks/redstone_golem_chest.json']:
    json.loads(Path('src/generated/resources/' + f).read_text())
for f in ['models/block/redstone_golem_chest/block_active.json', 'models/block/redstone_golem_chest/block_inactive.json',
          'models/item/redstone_golem_spawn_egg.json', 'textures/block/redstone_golem_chest/active.png',
          'textures/block/redstone_golem_chest/inactive.png', 'textures/entity/redstone_golem/redstone_golem.png',
          'textures/entity/redstone_golem/redstone_golem_eyes.png', 'textures/item/redstone_golem_spawn_egg.png']:
    assert Path('src/main/resources/assets/create/' + f).exists(), f

# ---- Revival pumpkins (structural; runtime unverified)
gr = root/'content/equipment/golemRevival'
kinds = (gr/'GolemKind.java').read_text()
pumpkin = (gr/'RevivalPumpkinBlock.java').read_text()
for k in ['ANDESITE("andesite"', 'NETHERITE("netherite"', 'REDSTONE("redstone"']:
    assert k in kinds, k
assert 'return null; // not placed: the pumpkin stays in hand' in pumpkin
assert 'GolemRevivalRules.decide(chestKind != null, chestKind == kind, bound != null, knownDead)' in pumpkin
assert 'chest.getGolemId()' in pumpkin and 'GolemGraveyard.get(level.getServer()).forget(previous)' in pumpkin
assert 'chestState.setValue(kind.active, true)' in pumpkin and 'chest.linkGolem(golem.getUUID())' in pumpkin
assert 'extends CarvedPumpkinBlock' not in pumpkin  # vanilla/Create golem-building mixin must not see it
assert '!level.isLoaded(chest)' in (gr/'GolemRevivalEvents.java').read_text()
blocks_src = (root/'AllBlocks.java').read_text()
for m in ['andesite', 'netherite', 'redstone']:
    assert f'{m.upper()}_REVIVAL_PUMPKIN =\n\t\trevivalPumpkin(GolemKind.{m.upper()}' in blocks_src, m
    r = json.loads(Path(f'src/generated/resources/data/create/recipe/crafting/curiosities/{m}_revival_pumpkin.json').read_text())
    assert r['pattern'] == [' P ', 'MMM'] and r['key']['P'] == 'minecraft:carved_pumpkin', m
    assert r['key']['M'] == {'andesite': 'create:andesite_alloy', 'netherite': 'minecraft:netherite_ingot', 'redstone': 'minecraft:redstone'}[m]
    for f in [f'assets/create/blockstates/{m}_revival_pumpkin.json', f'assets/create/items/{m}_revival_pumpkin.json',
              f'data/create/loot_table/blocks/{m}_revival_pumpkin.json']:
        json.loads(Path('src/generated/resources/' + f).read_text())
    for t in ['front', 'side', 'top']:
        assert Path(f'src/main/resources/assets/create/textures/block/{m}_revival_pumpkin/{t}.png').exists()
    assert f'create:{m}_revival_pumpkin' in Path('src/generated/resources/data/minecraft/tags/block/mineable/axe.json').read_text()
# the base-block materials the recipes use
assert '"C": "create:andesite_alloy"' in Path('src/generated/resources/data/create/recipe/crafting/materials/andesite_alloy_block.json').read_text()
assert 'public @Nullable UUID getGolemId()' in Path('src/main/java/com/simibubi/create/content/equipment/andesiteGolem/AndesiteGolemChestBlockEntity.java').read_text()
for lang_file in ['src/generated/resources/assets/create/lang/en_us.json', 'src/main/resources/assets/create/lang/pt_br.json']:
    for k, v in json.loads(Path(lang_file).read_text()).items():
        if 'revival_pumpkin' in k:
            assert ('Carved Pumpkin' in v or 'Abóbora Esculpida' in v or 'tooltip' not in k and 'jei' not in k), (k, v)

# ---- Round 15 (structural; runtime unverified)
ag = root/'content/equipment/andesiteGolem'
work = (ag/'AndesiteGolemWorkGoal.java').read_text()
sleep = (ag/'AndesiteGolemSleepGoal.java').read_text()
abe = (ag/'AndesiteGolemChestBlockEntity.java').read_text()
assert 'public static final int SIZE = 27;' in abe and 'for (int i = 0; i < container.getContainerSize(); i++)\n\t\t\tif (container.getItem(i).isEmpty())' in abe
assert 'SKIP_TICKS = 600;' in work and 'searchCooldown' not in work and 'claimedByOthers(found, pos, now)' in work
for where in ['public void stop() {', 'harvest(chest, target);']:
    assert 'release();' in work.split(where, 1)[1][:200], where
assert 'AndesiteGolemWorkReservations.SERVER.release(getUUID())' in andesite and 'golem_task.skipped' in andesite
assert 'state.is(Blocks.CACTUS_FLOWER)' in work and 'Kind.FLOWER' in work
assert 'GolemWorkGeometry.sleepHereInstead(bed != null' in sleep and 'checkCooldown' not in sleep
assert 'RedstoneGolemRules.giveUpWalk(travelTicks++, standSpot != null, stallTicks)' in rg_goal and 'PackagerLinkBlock' in rg_goal
assert 'public boolean skip()' in rg_goal and 'collectGoal.skip()' in rg_entity
assert 'RaisedItemLayer' in (rg/'RedstoneGolemRenderer.java').read_text()
links = (rg/'GolemChestLinks.java').read_text()
assert 'RedstoneGolemNetwork.sources(level, home)' in links and 'GolemChestLinks.isConnected(level, this)' in abe
for chest in ['andesite_golem_chest', 'netherite_golem_chest', 'redstone_golem_chest']:
    bs = json.loads(Path(f'src/generated/resources/assets/create/blockstates/{chest}.json').read_text())['variants']
    assert len(bs) == 16 and all(('linked=true' in k) == v['model'].endswith('_linked') for k, v in bs.items()), chest
    for v in ['active', 'inactive']:
        m = json.loads(Path(f'src/main/resources/assets/create/models/block/{chest}/block_{v}_linked.json').read_text())
        lock = [e for e in m['elements'] if e.get('name') == 'lock'][0]
        assert all(f['texture'] == '#1' for f in lock['faces'].values()) and m['textures']['1'].endswith(f'{v}_linked')
        others = [e for e in m['elements'] if e.get('name') != 'lock']
        assert all(f['texture'] == '#0' for e in others for f in e['faces'].values()), 'only the latch changes'
        assert Path(f'src/main/resources/assets/create/textures/block/{chest}/{v}_linked.png').exists()
print('Offline integration/serialization wiring checks passed (runtime unverified)')
