package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;

/**
 * The Redstone Golem's only job: run to the fullest chest on its network, take up to
 * {@link RedstoneGolemRules#CARRY_SLOTS} stacks, walk home and put them in its own chest. Idle while
 * its own chest cannot accept any more source items; what does not fit stays carried until there is room.
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
	private int returnCooldown;
	private @Nullable BlockPos standSpot;

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
			if (returnCooldown > 0) {
				returnCooldown--;
				golem.setTask(RedstoneGolem.Task.RETURNING);
				return false;
			}
			boolean room = golem.getCarried().stream().anyMatch(chest::hasRoomFor);
			golem.setTask(room ? RedstoneGolem.Task.RETURNING : RedstoneGolem.Task.CHEST_FULL);
			return room;
		}
		if (searchCooldown-- > 0)
			return false;
		searchCooldown = RedstoneGolemRules.SEARCH_INTERVAL;
		if (!RedstoneGolemRules.shouldCollect(hasTransferableSource(chest), 0)) {
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
			candidates.add(new RedstoneGolemRules.Candidate(transferableSlots(s.container(), chest),
				s.container().getContainerSize(), golem.distanceToSqr(Vec3.atCenterOf(s.pos())), blacklist.containsKey(s.pos())));
		int best = RedstoneGolemRules.chooseSource(candidates);
		return best < 0 ? null : sources.get(best).pos();
	}

	private boolean hasTransferableSource(RedstoneGolemChestBlockEntity chest) {
		// No empty slot is not equivalent to no capacity: existing partial stacks may still merge.
		for (int i = 0; i < chest.getContainer().getContainerSize(); i++)
			if (chest.getContainer().getItem(i).isEmpty()) return true;
		for (RedstoneGolemNetwork.Source source : RedstoneGolemNetwork.sources(golem.level(), chest))
			if (transferableSlots(source.container(), chest) > 0) return true;
		return false;
	}

	private int transferableSlots(Container source, RedstoneGolemChestBlockEntity chest) {
		int count = 0;
		for (int i = 0; i < source.getContainerSize(); i++)
			if (chest.hasRoomFor(source.getItem(i))) count++;
		return count;
	}

	@Override
	public boolean canContinueToUse() {
		return phase != Phase.DONE && golem.getChest() != null;
	}

	@Override
	public void start() {
		travelTicks = 0;
		standSpot = null;
		phase = golem.getCarried().isEmpty() ? Phase.GO : Phase.RETURN;
	}

	@Override
	public void stop() {
		phase = Phase.DONE;
		standSpot = null;
		source = null;
		golem.closeOpenedBarriers();
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		if (!golem.getCarried().isEmpty())
			golem.setTask(RedstoneGolem.Task.RETURNING);
		else if (golem.getTask() == RedstoneGolem.Task.COLLECTING || golem.getTask() == RedstoneGolem.Task.RETURNING)
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
				standSpot = null;
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
				take(container, chest);
			travelTicks = 0;
			standSpot = null;
			phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
			golem.setTask(phase == Phase.RETURN ? RedstoneGolem.Task.RETURNING : RedstoneGolem.Task.IDLE);
		}
		case RETURN -> {
			if (walkTo(chest.getBlockPos(), RedstoneGolemRules.WALK_SPEED)) {
				phase = Phase.DEPOSIT;
				standSpot = null;
				timer = DEPOSIT_TICKS;
				golem.setState(CopperGolemState.DROPPING_ITEM);
			}
		}
		case DEPOSIT -> {
			golem.getLookControl().setLookAt(Vec3.atCenterOf(chest.getBlockPos()));
			if (--timer > 0)
				return;
			deposit(chest);
			golem.closeOpenedBarriers(); // continuous goals still finish each trip's doors
			golem.setState(CopperGolemState.IDLE);
			if (!golem.getCarried().isEmpty()) {
				golem.setTask(RedstoneGolem.Task.CHEST_FULL);
				phase = Phase.DONE;
			} else {
				// Select the next source as part of this goal's transition. Otherwise the goal
				// stops at home and relies on an idle cooldown to restart the entire run.
				source = chooseSource(chest);
				if (source != null) {
					travelTicks = 0;
					standSpot = null;
					golem.setTask(RedstoneGolem.Task.COLLECTING);
					phase = Phase.GO;
				} else {
					golem.setTask(hasTransferableSource(chest) ? RedstoneGolem.Task.NO_SOURCE : RedstoneGolem.Task.CHEST_FULL);
					phase = Phase.DONE;
				}
			}
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

	/**
	 * Choose a reachable standing position next to the actual chest. Never accept a partial path
	 * ending on the roof above it. Both source and home use the same 3D arrival rule.
	 */
	private boolean walkTo(BlockPos chestPos, double speed) {
		if (standSpot != null && golem.position().distanceTo(Vec3.atBottomCenterOf(standSpot)) < 1.0
			&& golem.position().distanceTo(Vec3.atCenterOf(chestPos)) <= REACH) {
			golem.getNavigation().stop();
			return true;
		}
		if (travelTicks++ > RedstoneGolemRules.GIVE_UP_TICKS) {
			if (phase == Phase.GO)
				blacklist.put(chestPos, golem.level().getGameTime() + RedstoneGolemRules.BLACKLIST_TICKS);
			else returnCooldown = 100; // retry home after 5 s; never discard carried items
			phase = Phase.DONE;
			golem.getNavigation().stop();
			return false;
		}
		if (travelTicks % 40 != 1)
			return false;
		Path best = null;
		BlockPos bestSpot = null;
		for (BlockPos candidate : standingSpots(chestPos)) {
			Path path = golem.getNavigation().createPath(candidate, 0);
			if (path == null || !path.canReach()) continue;
			if (best == null || path.getNodeCount() < best.getNodeCount()) {
				best = path;
				bestSpot = candidate;
			}
		}
		standSpot = bestSpot;
		if (best != null) golem.getNavigation().moveTo(best, speed);
		return false;
	}

	/** Horizontal neighbors at chest Y and Y±1, including enclosed rooms; no heightmap lookup. */
	private List<BlockPos> standingSpots(BlockPos chestPos) {
		List<BlockPos> spots = new ArrayList<>();
		for (int dy = -1; dy <= 1; dy++)
			for (Direction direction : Direction.Plane.HORIZONTAL) {
				BlockPos pos = chestPos.relative(direction).offset(0, dy, 0);
				if (!golem.level().isLoaded(pos)) continue;
				BlockState feet = golem.level().getBlockState(pos);
				BlockState head = golem.level().getBlockState(pos.above());
				BlockState floor = golem.level().getBlockState(pos.below());
				// Reject a roof-side spot: it must have an unobstructed interaction ray to the chest.
				Vec3 eye = Vec3.atBottomCenterOf(pos).add(0, golem.getEyeHeight(), 0);
				BlockHitResult hit = golem.level().clip(new ClipContext(eye, Vec3.atCenterOf(chestPos),
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, golem));
				if (!RedstoneGolemRules.validStandSpot(pos.getX() - chestPos.getX(), dy,
					pos.getZ() - chestPos.getZ(), feet.getCollisionShape(golem.level(), pos).isEmpty(),
					head.getCollisionShape(golem.level(), pos.above()).isEmpty(),
					!floor.getCollisionShape(golem.level(), pos.below()).isEmpty(),
					hit.getBlockPos().equals(chestPos))) continue;
				spots.add(pos);
			}
		spots.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atBottomCenterOf(p))));
		return spots;
	}

	private void take(Container container, RedstoneGolemChestBlockEntity chest) {
		List<Boolean> occupied = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++)
			occupied.add(chest.hasRoomFor(container.getItem(i)));
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
