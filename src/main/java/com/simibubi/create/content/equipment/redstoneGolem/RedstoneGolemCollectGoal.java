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
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
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
	/** Game time of the last idle network search. */
	private long lastPlan = Long.MIN_VALUE / 2;
	/** Own-chest contentVersion last planned for; a different value wakes the golem. */
	private int seenChestVersion = Integer.MIN_VALUE;
	private @Nullable List<RedstoneGolemNetwork.Source> cachedSources;
	private long cachedSourcesAt;
	private boolean sourceUnreachable;
	/** Closest the golem got to its stand spot this walk, and ticks since it last got closer. */
	private double bestDistance = Double.MAX_VALUE;
	private int stallTicks;
	private boolean sourceBusy;
	private int returnCooldown;
	private @Nullable BlockPos standSpot;
	private boolean sourceBlockedByCapacity;

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
		if (golem.hasLoad()) {
			if (returnCooldown > 0) {
				returnCooldown--;
				golem.setTask(RedstoneGolem.Task.RETURNING);
				return false;
			}
			boolean room = golem.getCarried().stream().anyMatch(chest::hasRoomFor) || canUnloadDelivery(chest);
			golem.setTask(room ? RedstoneGolem.Task.RETURNING : RedstoneGolem.Task.CHEST_FULL);
			return room;
		}
		long now = golem.level().getGameTime();
		boolean woken = chest.getContentVersion() != seenChestVersion;
		if (!RedstoneGolemRules.shouldPlan(now, lastPlan, woken))
			return false;
		lastPlan = now;
		seenChestVersion = chest.getContentVersion();
		// Choose from actual transferable stacks in one bounded network scan. An empty
		// destination slot is not a source, and all 27 home slots are considered.
		source = chooseSource(chest, true);
		golem.setTask(source == null ? idleTask() : RedstoneGolem.Task.COLLECTING);
		return source != null;
	}

	private RedstoneGolem.Task idleTask() {
		return sourceBusy ? RedstoneGolem.Task.NETWORK_BUSY : sourceUnreachable ? RedstoneGolem.Task.UNREACHABLE
			: sourceBlockedByCapacity ? RedstoneGolem.Task.CHEST_FULL : RedstoneGolem.Task.NO_SOURCE;
	}

	/** Network sources, rescanned at most every SOURCE_CACHE_TICKS unless {@code fresh}. */
	private List<RedstoneGolemNetwork.Source> sources(RedstoneGolemChestBlockEntity chest, boolean fresh) {
		long now = golem.level().getGameTime();
		if (fresh || cachedSources == null || now - cachedSourcesAt >= RedstoneGolemRules.SOURCE_CACHE_TICKS
			|| now < cachedSourcesAt) {
			cachedSources = RedstoneGolemNetwork.sources(golem.level(), chest);
			cachedSourcesAt = now;
		}
		return cachedSources;
	}

	private net.minecraft.core.GlobalPos key(BlockPos pos) {
		return net.minecraft.core.GlobalPos.of(golem.level().dimension(), pos);
	}

	/** Picks and claims the next source; null (and the reason flags set) when there is none. */
	private @Nullable BlockPos chooseSource(RedstoneGolemChestBlockEntity chest, boolean fresh) {
		long now = golem.level().getGameTime();
		blacklist.values().removeIf(until -> until < now);
		RedstoneGolemReservations.SERVER.release(golem.getUUID());
		List<RedstoneGolemNetwork.Source> sources = sources(chest, fresh);
		golem.setNetworkChests(sources.size());
		List<RedstoneGolemRules.Candidate> candidates = new ArrayList<>();
		sourceBlockedByCapacity = sourceUnreachable = sourceBusy = false;
		for (RedstoneGolemNetwork.Source s : sources) {
			int transferable = transferableSlots(s.container(), chest);
			if (transferable == 0 && RedstoneGolemNetwork.occupiedSlots(s.container()) > 0)
				sourceBlockedByCapacity = true;
			boolean skipped = blacklist.containsKey(s.pos());
			int others = RedstoneGolemReservations.SERVER.othersOn(key(s.pos()), golem.getUUID(), now);
			if (transferable > 0 && skipped)
				sourceUnreachable = true;
			if (transferable > 0 && !skipped && others > 0 && !RedstoneGolemRules.canShare(transferable, others))
				sourceBusy = true;
			candidates.add(new RedstoneGolemRules.Candidate(transferable, s.container().getContainerSize(),
				golem.distanceToSqr(Vec3.atCenterOf(s.pos())), skipped, others));
		}
		int best = RedstoneGolemRules.chooseSource(candidates);
		// A network golem holding none of an item it uses (and that the distribution column has)
		// gets a dedicated trip first, even if it has nothing to collect: it cannot repair otherwise.
		BlockPos urgent = null;
		double urgentDistance = Double.MAX_VALUE;
		for (int i = 0; i < sources.size(); i++) {
			RedstoneGolemNetwork.Source s = sources.get(i);
			if (candidates.get(i).skipped() || candidates.get(i).reservedByOthers() > 0
				|| !(golem.level().getBlockEntity(s.pos()) instanceof AndesiteGolemChestBlockEntity target))
				continue;
			for (int slot = 0; slot < chest.getDistribution().getContainerSize(); slot++) {
				ItemStack item = chest.getDistribution().getItem(slot);
				if (!item.isEmpty() && GolemDistribution.urgent(targetOf(target, item))
					&& candidates.get(i).distanceSqr() < urgentDistance) {
					urgent = s.pos();
					urgentDistance = candidates.get(i).distanceSqr();
				}
			}
		}
		if (best < 0 && urgent == null)
			return null;
		BlockPos chosen = urgent != null ? urgent : sources.get(best).pos();
		RedstoneGolemReservations.SERVER.claim(key(chosen), golem.getUUID(), now);
		loadDelivery(chest, sources, chosen);
		return chosen;
	}

	private static GolemDistribution.Target targetOf(AndesiteGolemChestBlockEntity target, ItemStack item) {
		return new GolemDistribution.Target(target.personalCount(item.getItem()), target.personalRoom(item));
	}

	/**
	 * Takes this trip's share for {@code chosen} out of the distribution column: one carry slot, one
	 * item type (the first the target uses and gets a share of). Shares follow GolemDistribution#plan
	 * over every golem chest of the network that uses the item.
	 */
	private void loadDelivery(RedstoneGolemChestBlockEntity chest, List<RedstoneGolemNetwork.Source> sources, BlockPos chosen) {
		if (!golem.getDelivery().isEmpty() || !(golem.level().getBlockEntity(chosen) instanceof AndesiteGolemChestBlockEntity dest))
			return;
		var dist = chest.getDistribution();
		for (int slot = 0; slot < dist.getContainerSize(); slot++) {
			ItemStack item = dist.getItem(slot);
			if (item.isEmpty() || !dest.usesPersonally(item))
				continue;
			List<GolemDistribution.Target> targets = new ArrayList<>();
			int destIndex = -1;
			for (RedstoneGolemNetwork.Source s : sources)
				if (golem.level().getBlockEntity(s.pos()) instanceof AndesiteGolemChestBlockEntity t && t.usesPersonally(item)) {
					if (s.pos().equals(chosen))
						destIndex = targets.size();
					targets.add(targetOf(t, item));
				}
			int stock = 0;
			for (int i = 0; i < dist.getContainerSize(); i++)
				if (ItemStack.isSameItemSameComponents(dist.getItem(i), item))
					stock += dist.getItem(i).getCount();
			int share = destIndex < 0 ? 0 : Math.min(GolemDistribution.plan(stock, targets)[destIndex], item.getMaxStackSize());
			if (share <= 0)
				continue;
			ItemStack load = item.copyWithCount(0);
			for (int i = 0; i < dist.getContainerSize() && load.getCount() < share; i++)
				if (ItemStack.isSameItemSameComponents(dist.getItem(i), item))
					load.grow(dist.removeItem(i, share - load.getCount()).getCount());
			golem.getDelivery().add(load);
			golem.updateHeldItem();
			return;
		}
	}

	private boolean canUnloadDelivery(RedstoneGolemChestBlockEntity chest) {
		for (ItemStack d : golem.getDelivery())
			if (chest.getDistribution().canAddItem(d) || chest.hasRoomFor(d))
				return true;
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
		phase = source != null && golem.getCarried().isEmpty() ? Phase.GO : Phase.RETURN;
	}

	@Override
	public void stop() {
		phase = Phase.DONE;
		standSpot = null;
		source = null;
		RedstoneGolemReservations.SERVER.release(golem.getUUID());
		golem.closeOpenedBarriers();
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		if (golem.hasLoad())
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
			RedstoneGolemReservations.SERVER.claim(key(source), golem.getUUID(), golem.level().getGameTime());
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
			if (container != null && golem.level().getBlockEntity(source) instanceof AndesiteGolemChestBlockEntity target) {
				List<ItemStack> undelivered = new ArrayList<>();
				for (ItemStack share : golem.getDelivery()) {
					ItemStack rest = target.insertPersonal(share);
					if (!rest.isEmpty())
						undelivered.add(rest); // full or holding other items: back to the distribution column
				}
				golem.getDelivery().clear();
				golem.getDelivery().addAll(undelivered);
			}
			if (container != null)
				take(container, chest);
			RedstoneGolemReservations.SERVER.release(golem.getUUID()); // arrived and took: free for others
			travelTicks = 0;
			standSpot = null;
			golem.updateHeldItem();
			phase = golem.hasLoad() ? Phase.RETURN : Phase.DONE;
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
			if (golem.hasLoad()) {
				golem.setTask(RedstoneGolem.Task.CHEST_FULL);
				phase = Phase.DONE;
			} else {
				// Select the next source as part of this goal's transition. Otherwise the goal
				// stops at home and relies on an idle cooldown to restart the entire run.
				source = chooseSource(chest, true);
				seenChestVersion = chest.getContentVersion(); // own deposit is not a wake
				if (source != null) {
					travelTicks = 0;
					standSpot = null;
					golem.setTask(RedstoneGolem.Task.COLLECTING);
					phase = Phase.GO;
				} else {
					golem.setTask(idleTask());
					lastPlan = golem.level().getGameTime();
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
		for (RedstoneGolemNetwork.Source s : sources(chest, false))
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
		if (travelTicks == 0) {
			bestDistance = Double.MAX_VALUE;
			stallTicks = 0;
		}
		if (standSpot != null) {
			double d = golem.position().distanceTo(Vec3.atBottomCenterOf(standSpot));
			if (d < bestDistance - 0.5) {
				bestDistance = d;
				stallTicks = 0;
			} else
				stallTicks++;
		}
		// One bad source must not hold the golem: no reachable stand spot after the first search, or
		// no progress for STALL_TICKS, gives up right away instead of after the full GIVE_UP_TICKS.
		if (RedstoneGolemRules.giveUpWalk(travelTicks++, standSpot != null, stallTicks)) {
			if (phase == Phase.GO) {
				blacklist.put(chestPos, golem.level().getGameTime() + RedstoneGolemRules.BLACKLIST_TICKS);
				RedstoneGolemReservations.SERVER.release(golem.getUUID());
			}
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
				// The Stock Link that makes a chest a source sits on one of its faces: seeing it counts.
				boolean seesChest = hit.getBlockPos().equals(chestPos)
					|| golem.level().getBlockState(hit.getBlockPos()).getBlock()
						instanceof com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock
						&& hit.getBlockPos().distManhattan(chestPos) == 1;
				if (!RedstoneGolemRules.validStandSpot(pos.getX() - chestPos.getX(), dy,
					pos.getZ() - chestPos.getZ(), feet.getCollisionShape(golem.level(), pos).isEmpty(),
					head.getCollisionShape(golem.level(), pos.above()).isEmpty(),
					!floor.getCollisionShape(golem.level(), pos.below()).isEmpty(),
					seesChest)) continue;
				spots.add(pos);
			}
		spots.sort(Comparator.comparingDouble(p -> golem.distanceToSqr(Vec3.atBottomCenterOf(p))));
		return spots;
	}

	/**
	 * Wrench: drop the current source and ignore it for SKIP_TICKS; carried items still go home.
	 * @return whether there was a source to skip
	 */
	public boolean skip() {
		if (source == null || phase != Phase.GO && phase != Phase.TAKE)
			return false;
		blacklist.put(source, golem.level().getGameTime() + RedstoneGolemRules.SKIP_TICKS);
		RedstoneGolemReservations.SERVER.release(golem.getUUID());
		source = null;
		standSpot = null;
		travelTicks = 0;
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		phase = golem.hasLoad() ? Phase.RETURN : Phase.DONE;
		lastPlan = Long.MIN_VALUE / 2; // choose the next source right away
		return true;
	}

	private void take(Container container, RedstoneGolemChestBlockEntity chest) {
		List<Boolean> occupied = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++)
			occupied.add(chest.hasRoomFor(container.getItem(i)));
		for (int slot : RedstoneGolemRules.pickupSlots(occupied, golem.getCarried().size() + golem.getDelivery().size()))
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
		List<ItemStack> kept = new ArrayList<>();
		for (ItemStack share : golem.getDelivery()) {
			ItemStack rest = chest.returnToDistribution(share);
			if (!rest.isEmpty())
				rest = chest.insert(rest);
			if (!rest.isEmpty())
				kept.add(rest);
		}
		golem.getDelivery().clear();
		golem.getDelivery().addAll(kept);
		golem.updateHeldItem();
	}
}
