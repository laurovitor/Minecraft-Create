#!/usr/bin/env python3
"""Offline contract guards for the Redstone Golem lifecycle; no Minecraft client is launched."""
from pathlib import Path

root = Path(__file__).resolve().parents[2]
redstone = root / 'src/main/java/com/simibubi/create/content/equipment/redstoneGolem'
andesite = root / 'src/main/java/com/simibubi/create/content/equipment/andesiteGolem'
read = lambda path: path.read_text()
goal = read(redstone / 'RedstoneGolemCollectGoal.java')
golem = read(redstone / 'RedstoneGolem.java')
chest = read(redstone / 'RedstoneGolemChestBlock.java')
capacity = read(redstone / 'RedstoneGolemChestBlockEntity.java')
parent = read(andesite / 'AndesiteGolem.java')
network = read(redstone / 'RedstoneGolemNetwork.java')
barriers = read(redstone / 'RedstoneGolemBarriers.java')

# Actual goal state transition, not merely a model of the old selection policy.
assert 'source = chooseSource(chest, true);' in goal
# Round 12: game-time scheduling with own-chest wake, cached sources, reservations, network join.
assert 'RedstoneGolemRules.shouldPlan(now, lastPlan, woken)' in goal and 'searchCooldown' not in goal
assert 'chest.getContentVersion() != seenChestVersion' in goal and 'contentVersion++' in capacity
assert goal.count('RedstoneGolemNetwork.sources(') == 1, 'network scanned only through the cache'
assert 'RedstoneGolemReservations.SERVER.claim(key(chosen)' in goal and 'othersOn(key(s.pos())' in goal
for where in ['public void stop()', 'case TAKE -> {', 'if (phase == Phase.GO) {']:
    assert 'RedstoneGolemReservations.SERVER.release(golem.getUUID())' in goal.split(where, 1)[1][:900], where
assert 'RedstoneGolemReservations.SERVER.release(getUUID())' in golem
assert 'AllItems.PACKED_REDSTONE_GOLEM_CHEST.isIn(stack)' in chest and 'joinNetwork(stack, chest.link.freqId, player)' in chest
assert 'chest.link.mayInteractMessage(player)' in chest
assert 'phase = Phase.GO;' in goal.split('case DEPOSIT -> {', 1)[1]
assert 'travelTicks = 0;' in goal.split('case DEPOSIT -> {', 1)[1]
assert 'transferableSlots(s.container(), chest)' in goal
assert 'take(container, chest)' in goal
assert 'chest.hasRoomFor(container.getItem(i))' in goal
assert 'stored.getCount() <' in capacity and 'isSameItemSameComponents' in capacity
assert 'walkTo(source, RedstoneGolemRules.RUN_SPEED)' in goal
assert 'walkTo(chest.getBlockPos(), RedstoneGolemRules.WALK_SPEED)' in goal
assert 'golem.closeOpenedBarriers();' in goal and 'getAllPresent(home.link.freqId' in network
assert 'close' in barriers.lower() and 'open' in barriers.lower()

# Single cargo inventory: save/reload and packed-chest hand-off retain list; visual has no contents.
assert 'output.store("Carried", ItemStack.CODEC.listOf(), carried)' in parent
assert 'input.read("Carried", ItemStack.CODEC.listOf()).ifPresent(carried::addAll)' in parent
assert 'super.readAdditionalSaveData(input);\n\t\tdelivery.clear();' in golem and 'ifPresent(delivery::addAll);\n\t\tupdateHeldItem();' in golem
assert 'data.store("Carried", ItemStack.CODEC.listOf()' in chest
assert 'data.read("Carried", ItemStack.CODEC.listOf()' in chest
assert 'golem.getCarried().clear();\n\t\tgolem.updateHeldItem();' in chest
assert 'PackageStyles.getDefaultBox()' in golem
assert 'ItemContainerContents.fromItems(contents.subList' in golem
assert 'i += PackageItem.SLOTS' in golem
assert 'box.set(AllDataComponents.PACKAGE_CONTENTS' in golem
assert 'getCarried().clear();\n\t\t\tupdateHeldItem();' in golem
assert 'setDropChance(EquipmentSlot.MAINHAND, 0)' in parent
loot = golem.split('protected void dropCustomDeathLoot', 1)[1]
assert loot.count('spawnAtLocation(level, box)') == 1
assert 'spawnAtLocation(level, stack)' not in loot
print('Redstone lifecycle integration guards passed: repeat, capacity, cargo, reload, death box, route/barriers')
