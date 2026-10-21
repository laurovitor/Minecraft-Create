package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * The Redstone Golem's only job: run to the fullest chest on its network, take up to
 * {@link RedstoneGolemRules#CARRY_SLOTS} stacks, walk home and put them in its own chest. Idle while
 * its own chest has no empty slot; what does not fit stays carried until there is room.
 */
public class RedstoneGolemCollectGoal extends Goal {

	private enum Phase { GO, TAKE, RETURN, DEPOSIT, DONE }

	/** Ticks spent taking from / putting into a chest, like the other golems' chest animations. */
	private static final int TAKE_TICKS = 20;
	private static final int DEPOSIT_TICKS = 20;
	/** Distance from a chest's center that counts as at the chest, blocks (as the Andesite golem). */
	private static final double REACH = 2.5;

	private final RedstoneGolem golem;
	private final Map<BlockPos, Long> blacklist = new HashMap<>();
	private Phase phase = Phase.DONE;
	private @Nullable BlockPos source;
	private int timer;
	private int travelTicks;
	private int searchCooldown;

	public RedstoneGolemCollectGoal(RedstoneGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		RedstoneGolemChestBlockEntity chest = golem.getChest();
		if (chest == null)
			return false;
		// Loaded from a save or left over from a full chest: bring it home first.
		if (!golem.getCarried().isEmpty()) {
			golem.setTask(chest.hasEmptySlot() ? RedstoneGolem.Task.RETURNING : RedstoneGolem.Task.CHEST_FULL);
			return chest.hasEmptySlot();
		}
		if (searchCooldown-- > 0)
			return false;
		searchCooldown = RedstoneGolemRules.SEARCH_INTERVAL;
		if (!RedstoneGolemRules.shouldCollect(chest.hasEmptySlot(), golem.getCarried().size())) {
			golem.setTask(RedstoneGolem.Task.CHEST_FULL);
			return false;
		}
		source = chooseSource(chest);
		golem.setTask(source == null ? RedstoneGolem.Task.NO_SOURCE : RedstoneGolem.Task.COLLECTING);
		return source != null;
	}

	private @Nullable BlockPos chooseSource(RedstoneGolemChestBlockEntity chest) {
		long now = golem.level().getGameTime();
		blacklist.values().removeIf(until -> until < now);
		List<RedstoneGolemNetwork.Source> sources = RedstoneGolemNetwork.sources(golem.level(), chest);
		golem.setNetworkChests(sources.size());
		List<RedstoneGolemRules.Candidate> candidates = new ArrayList<>();
		for (RedstoneGolemNetwork.Source s : sources)
			candidates.add(new RedstoneGolemRules.Candidate(RedstoneGolemNetwork.occupiedSlots(s.container()),
				s.container().getContainerSize(), golem.distanceToSqr(Vec3.atCenterOf(s.pos())), blacklist.containsKey(s.pos())));
		int best = RedstoneGolemRules.chooseSource(candidates);
		return best < 0 ? null : sources.get(best).pos();
	}

	@Override
	public boolean canContinueToUse() {
		return phase != Phase.DONE && golem.getChest() != null;
	}

	@Override
	public void start() {
		travelTicks = 0;
		phase = golem.getCarried().isEmpty() ? Phase.GO : Phase.RETURN;
	}

	@Override
	public void stop() {
		phase = Phase.DONE;
		source = null;
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		if (golem.getTask() == RedstoneGolem.Task.COLLECTING || golem.getTask() == RedstoneGolem.Task.RETURNING)
			golem.setTask(RedstoneGolem.Task.IDLE);
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		RedstoneGolemChestBlockEntity chest = golem.getChest();
		if (chest == null) {
			phase = Phase.DONE;
			return;
		}
		switch (phase) {
		case GO -> {
			if (source == null || sourceContainer(chest) == null) {
				phase = Phase.DONE;
				return;
			}
			if (walkTo(source, RedstoneGolemRules.RUN_SPEED)) {
				phase = Phase.TAKE;
				timer = TAKE_TICKS;
				golem.setState(CopperGolemState.GETTING_ITEM);
			}
		}
		case TAKE -> {
			golem.getLookControl().setLookAt(Vec3.atCenterOf(source));
			if (--timer > 0)
				return;
			golem.setState(CopperGolemState.IDLE);
			Container container = sourceContainer(chest);
			if (container != null)
				take(container);
			travelTicks = 0;
			phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
			golem.setTask(phase == Phase.RETURN ? RedstoneGolem.Task.RETURNING : RedstoneGolem.Task.IDLE);
		}
		case RETURN -> {
			if (walkTo(chest.getBlockPos(), RedstoneGolemRules.WALK_SPEED)) {
				phase = Phase.DEPOSIT;
				timer = DEPOSIT_TICKS;
				golem.setState(CopperGolemState.DROPPING_ITEM);
			}
		}
		case DEPOSIT -> {
			golem.getLookControl().setLookAt(Vec3.atCenterOf(chest.getBlockPos()));
			if (--timer > 0)
				return;
			deposit(chest);
			golem.setState(CopperGolemState.IDLE);
			golem.setTask(golem.getCarried().isEmpty() ? RedstoneGolem.Task.IDLE : RedstoneGolem.Task.CHEST_FULL);
			phase = Phase.DONE;
		}
		default -> {
		}
		}
	}

	/** The source's inventory if it is still on the network (link not removed, still loaded). */
	private @Nullable Container sourceContainer(RedstoneGolemChestBlockEntity chest) {
		for (RedstoneGolemNetwork.Source s : RedstoneGolemNetwork.sources(golem.level(), chest))
			if (s.pos().equals(source))
				return s.container();
		return null;
	}

	/** @return true once within reach of the chest */
	private boolean walkTo(BlockPos pos, double speed) {
		if (golem.position().distanceTo(Vec3.atCenterOf(pos)) <= REACH) {
			golem.getNavigation().stop();
			return true;
		}
		if (travelTicks++ > RedstoneGolemRules.GIVE_UP_TICKS) {
			if (phase == Phase.GO)
				blacklist.put(pos, golem.level().getGameTime() + RedstoneGolemRules.BLACKLIST_TICKS);
			phase = Phase.DONE;
			return false;
		}
		if (travelTicks % 20 == 1 || golem.getNavigation().isDone()) {
			Path path = golem.getNavigation().createPath(pos, 1);
			golem.getNavigation().moveTo(path, speed);
		}
		return false;
	}

	private void take(Container container) {
		List<Boolean> occupied = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++)
			occupied.add(!container.getItem(i).isEmpty());
		for (int slot : RedstoneGolemRules.pickupSlots(occupied, golem.getCarried().size()))
			golem.carry(container.removeItemNoUpdate(slot));
		container.setChanged();
	}

	private void deposit(RedstoneGolemChestBlockEntity chest) {
		Iterator<ItemStack> iterator = golem.getCarried().iterator();
		List<ItemStack> leftovers = new ArrayList<>();
		while (iterator.hasNext()) {
			ItemStack remainder = chest.insert(iterator.next());
			iterator.remove();
			if (!remainder.isEmpty())
				leftovers.add(remainder);
		}
		golem.getCarried().addAll(leftovers);
		golem.updateHeldItem();
	}
}
