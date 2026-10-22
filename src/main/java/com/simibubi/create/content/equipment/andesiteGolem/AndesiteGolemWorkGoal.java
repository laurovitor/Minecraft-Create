package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.kinetics.saw.TreeCutter;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * The Andesite Golem's job: find a grown tree, crop or tall sugar cane/cactus near its chest,
 * walk there, harvest it, return and store what the chest filter accepts. Trees and crops are
 * replanted; sugar cane and cactus keep their rooted bottom block and regrow naturally.
 * Everything else is discarded.
 */
public class AndesiteGolemWorkGoal extends Goal {

	private enum Phase { GO_TO_TARGET, WORK, RETURN, DEPOSIT, GO_TO_PATH, DONE }

	private enum Kind { TREE, CROP, TALL_PLANT, FLOWER }

	/** Ticks spent chopping/harvesting and storing, roughly the copper golem chest animations. */
	private static final int WORK_TICKS = 30;
	private static final int DEPOSIT_TICKS = 20;
	/**
	 * How often the golem looks for new work while it has none, in GAME ticks (2 s). Vanilla calls
	 * Goal#canUse only every second tick; the old call-counted 40 really waited ~80 ticks.
	 */
	private static final int SEARCH_INTERVAL = 40;
	/** A wrench-skipped job is ignored this long, game ticks (30 s). */
	public static final int SKIP_TICKS = 600;
	/** Gives up on a target it cannot reach after this long, and skips it for a while. */
	private static final int GIVE_UP_TICKS = 300;
	private static final int BLACKLIST_TICKS = 1200;
	private static final double REACH = 2.5;
	/** How often a golem walking to a tree checks whether leaves are blocking it. */
	private static final int LEAF_CHECK_INTERVAL = 20;
	/** Standing spots around a trunk are tried from this far above its lowest log (buried giant-tree
	 * roots) down to {@link #APPROACH_DOWN} below it (mangrove roots), in blocks. */
	private static final int APPROACH_UP = 8;
	private static final int APPROACH_DOWN = 7;
	/** Only the nearest standing spots get a path computed per refresh: bounded pathfinding cost. */
	private static final int MAX_APPROACH_PATHS = 16;
	/** A cut can start beside the trunk up to this many blocks above its lowest log. */
	private static final int PERIMETER_UP = APPROACH_UP + 2;

	private final AndesiteGolem golem;
	private final Map<BlockPos, Long> blacklist = new HashMap<>();

	private Phase phase = Phase.DONE;
	private @Nullable BlockPos target;
	private @Nullable BlockPos pathTarget;
	private @Nullable BlockPos approach;
	private Kind kind = Kind.CROP;
	private int timer;
	private int travelTicks;
	/** Game time of the next idle search. */
	private long nextSearch = Long.MIN_VALUE;
	/** The claim key of the current target (normalized trunk footprint corner for 2x2 trees). */
	private @Nullable BlockPos claimed;
	private double lastDistance = Double.MAX_VALUE;

	public AndesiteGolemWorkGoal(AndesiteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		AndesiteGolemChestBlockEntity chest = golem.getChest();
		if (chest == null)
			return false;
		// canUse may run while another goal owns MOVE. Do not retain an idle
		// candidate that could override a newly found primary job in start().
		target = null;
		pathTarget = null;
		blacklist.values().removeIf(until -> until < golem.level().getGameTime());
		// things to store, and room for them
		if (!golem.getCarried().isEmpty())
			return chest.hasEmptySlot();
		long now = golem.level().getGameTime();
		if (now < nextSearch && nextSearch - now <= SEARCH_INTERVAL)
			return false;
		nextSearch = now + SEARCH_INTERVAL;
		if (!chest.hasEmptySlot())
			golem.setTreeStatus(golem.getTreeStatusPos(), AndesiteGolem.TreeStatus.CHEST_FULL);
		else if (findTarget(chest))
			return true;
		// An exact canReach() check can veto every chest-edge block before the
		// idle phase even starts. A partial route is worth trying; completion
		// still requires the golem to occupy the selected ground column.
		pathTarget = AndesiteGolemArea.nextPaveSpot(golem.level(), chest.getBlockPos(), feet ->
			!blacklist.containsKey(feet.below()) && (golem.blockPosition().equals(feet)
				|| golem.getNavigation().createPath(feet, 1) != null));
		return pathTarget != null;
	}

	@Override
	public boolean canContinueToUse() {
		return phase != Phase.DONE && golem.getChest() != null;
	}

	@Override
	public void start() {
		travelTicks = 0;
		lastDistance = Double.MAX_VALUE;
		phase = !golem.getCarried().isEmpty() ? Phase.RETURN
			: pathTarget != null ? Phase.GO_TO_PATH : Phase.GO_TO_TARGET;
	}

	@Override
	public void stop() {
		phase = Phase.DONE;
		release();
		target = null;
		pathTarget = null;
		approach = null;
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		AndesiteGolemChestBlockEntity chest = golem.getChest();
		if (chest == null) {
			phase = Phase.DONE;
			return;
		}

		switch (phase) {
		case GO_TO_TARGET -> {
			renewClaim();
			if (target == null || !isValidTarget(chest, target)) {
				phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
				return;
			}
			if (kind == Kind.TREE ? walkToTree(target) : walkTo(target)) {
				phase = Phase.WORK;
				timer = WORK_TICKS;
				golem.setState(CopperGolemState.GETTING_ITEM);
				return;
			}
			if (kind == Kind.TREE && phase == Phase.GO_TO_TARGET)
				clearLeavesIfStuck(chest);
		}
		case WORK -> {
			renewClaim();
			golem.getLookControl().setLookAt(Vec3.atCenterOf(target));
			if (--timer > 0)
				return;
			golem.setState(CopperGolemState.IDLE);
			if (isValidTarget(chest, target))
				harvest(chest, target);
			release();
			target = null;
			travelTicks = 0;
			phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
		}
		case GO_TO_PATH -> {
			if (pathTarget == null || !AndesiteGolemArea.canPave(golem.level(), pathTarget)) {
				phase = Phase.DONE;
				return;
			}
			if (walkToPath(pathTarget.above())) {
				AndesiteGolemArea.pave(golem.level(), pathTarget);
				phase = Phase.DONE;
			}
		}
		case RETURN -> {
			if (walkTo(chest.getBlockPos())) {
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
			phase = Phase.DONE;
		}
		default -> {
		}
		}
	}

	/** @return true once close enough to work on the block */
	private boolean walkTo(BlockPos pos) {
		Vec3 center = Vec3.atCenterOf(pos);
		if (golem.position().distanceTo(center) <= REACH) {
			golem.getNavigation().stop();
			return true;
		}
		if (travelTicks++ > GIVE_UP_TICKS) {
			if (phase == Phase.GO_TO_TARGET && target != null) {
				blacklist.put(target, golem.level().getGameTime() + BLACKLIST_TICKS);
				release();
			}
			phase = Phase.DONE;
			return false;
		}
		if (travelTicks % 20 == 1 || golem.getNavigation().isDone()) {
			Path path = golem.getNavigation().createPath(pos, 1);
			golem.getNavigation().moveTo(path, 1.0);
		}
		return false;
	}

	/** Path paving requires arrival at the chest perimeter, not the general work reach. */
	private boolean walkToPath(BlockPos feet) {
		// Path.canReach() can be false at the exact node beside the chest.
		// Paving is allowed only once the golem is actually on that column.
		if (GolemWorkGeometry.onPaveColumn(golem.getX(), golem.getY(), golem.getZ(),
			feet.getX(), feet.getY(), feet.getZ())) {
			golem.getNavigation().stop();
			return true;
		}
		if (travelTicks++ > GIVE_UP_TICKS) {
			blacklist.put(feet.below(), golem.level().getGameTime() + BLACKLIST_TICKS);
			phase = Phase.DONE;
			return false;
		}
		if (travelTicks % 20 == 1 || golem.getNavigation().isDone())
			golem.getNavigation().moveTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 1.0);
		return false;
	}

	/** Reach an accessible outside edge, not the solid trunk's center. Mangrove logs may be
	 * several blocks above the ground on roots: the working position is at ground level. */
	private boolean walkToTree(BlockPos base) {
		List<BlockPos> footprint = baseCluster(base);
		// On a complete 2x2 trunk, navigation can terminate alongside its solid
		// face without ever reporting the exact requested node as reached.
		if (footprint.size() == 4 && atTreePerimeter(base, footprint)) {
			golem.getNavigation().stop();
			return true;
		}
		if (approach == null || travelTicks % 20 == 0 || golem.getNavigation().isDone()) {
			BlockPos best = null;
			Path bestPath = null;
			double bestDistance = Double.MAX_VALUE;
			List<BlockPos> spots = new ArrayList<>();
			for (BlockPos trunk : footprint)
				for (Direction side : Direction.Plane.HORIZONTAL)
					for (int outward = 1; outward <= 3; outward++) {
					BlockPos edge = trunk.relative(side, outward);
					for (int dy = APPROACH_UP; dy >= -APPROACH_DOWN; dy--) {
						BlockPos feet = edge.offset(0, dy, 0);
						if (spots.contains(feet) || footprint.contains(feet) || !canStandAt(feet))
							continue;
						if (golem.position().distanceToSqr(Vec3.atBottomCenterOf(feet)) <= 2.0) {
							approach = feet;
							golem.getNavigation().stop();
							return true;
						}
						spots.add(feet);
					}
				}
			spots.sort(java.util.Comparator.comparingDouble(feet -> golem.distanceToSqr(Vec3.atBottomCenterOf(feet))));
			for (BlockPos feet : spots.subList(0, Math.min(MAX_APPROACH_PATHS, spots.size()))) {
				Path path = golem.getNavigation().createPath(feet, footprint.size() == 4 ? 1 : 0);
				if (path == null || (footprint.size() != 4 && !path.canReach()))
					continue;
				// A partial route is useful for a wide trunk if it ends at
				// its face. Keep complete routes ahead of partial ones.
				double score = golem.distanceToSqr(Vec3.atBottomCenterOf(feet))
					+ (path.canReach() ? 0 : 1000);
				if (score < bestDistance) {
					best = feet;
					bestPath = path;
					bestDistance = score;
				}
			}
			approach = best;
			if (bestPath != null)
				golem.getNavigation().moveTo(bestPath, 1.0);
		}
		if (footprint.size() == 4 && atTreePerimeter(base, footprint)
			|| approach != null && golem.position().distanceToSqr(Vec3.atBottomCenterOf(approach)) <= 2.0) {
			golem.getNavigation().stop();
			return true;
		}
		if (travelTicks++ > GIVE_UP_TICKS) {
			blacklist.put(base, golem.level().getGameTime() + BLACKLIST_TICKS);
			release();
			phase = Phase.DONE;
		}
		return false;
	}

	private boolean atTreePerimeter(BlockPos base, List<BlockPos> footprint) {
		// A giant tree's lowest logs can be buried below the surface the golem walks on: compare
		// with the trunk at the golem's own height while that height is still part of the trunk.
		int feetY = golem.getBlockY();
		int trunkY = feetY > base.getY() && feetY - base.getY() <= PERIMETER_UP
			&& golem.level().getBlockState(base.atY(feetY)).is(BlockTags.LOGS) ? feetY : base.getY();
		for (BlockPos trunk : footprint)
			if (GolemWorkGeometry.atOuterTreeFace(golem.getX(), golem.getY(), golem.getZ(),
				trunk.getX(), trunkY, trunk.getZ()))
				return true;
		return false;
	}

	private boolean canStandAt(BlockPos feet) {
		return golem.level().isLoaded(feet) && golem.level().isLoaded(feet.below())
			&& golem.level().getBlockState(feet).getCollisionShape(golem.level(), feet).isEmpty()
			&& golem.level().getBlockState(feet.above()).getCollisionShape(golem.level(), feet.above()).isEmpty()
			&& golem.level().getBlockState(feet.below()).isFaceSturdy(golem.level(), feet.below(), Direction.UP);
	}

	/**
	 * Leaves hanging down to the ground can wall the trunk off. When the golem stops getting closer
	 * to a tree it is about to cut anyway, it clears the leaves right in front of it, towards the
	 * trunk. Their drops go through the chest filter like the rest of the tree.
	 */
	private void clearLeavesIfStuck(AndesiteGolemChestBlockEntity chest) {
		if (travelTicks % LEAF_CHECK_INTERVAL != 0)
			return;
		double distance = golem.position().distanceTo(Vec3.atCenterOf(target));
		boolean stuck = distance > lastDistance - 0.3;
		lastDistance = distance;
		if (!stuck)
			return;

		Vec3 toTrunk = Vec3.atCenterOf(target).subtract(golem.position());
		// one block towards the trunk; diagonal when both axes matter
		double major = Math.max(Math.abs(toTrunk.x), Math.abs(toTrunk.z));
		int stepX = (int) Math.round(toTrunk.x / major);
		int stepZ = (int) Math.round(toTrunk.z / major);
		BlockPos feet = golem.blockPosition();
		List<ItemStack> gathered = new ArrayList<>();
		boolean cleared = false;
		for (BlockPos ahead : List.of(feet.offset(stepX, 0, stepZ), feet.offset(stepX, 0, 0), feet.offset(0, 0, stepZ))) {
			for (int dy = 0; dy <= 1; dy++) {
				BlockPos pos = ahead.above(dy);
				if (!golem.level().getBlockState(pos).is(BlockTags.LEAVES))
					continue;
				BlockHelper.destroyBlockAs(golem.level(), pos, null, ItemStack.EMPTY, 1, gathered::add);
				cleared = true;
			}
		}
		if (!cleared)
			return;
		for (ItemStack stack : gathered)
			if (!stack.isEmpty() && chest.accepts(stack))
				golem.carry(stack);
		// clearing is progress, give the walk more time and a fresh path
		travelTicks = Math.max(0, travelTicks - LEAF_CHECK_INTERVAL);
		golem.getNavigation().stop();
	}

	// ---- finding work

	private boolean findTarget(AndesiteGolemChestBlockEntity chest) {
		long now = golem.level().getGameTime();
		blacklist.values().removeIf(until -> until < now);

		BlockPos home = chest.getBlockPos();
		int r = AndesiteGolem.WORK_RADIUS;
		BlockPos best = null;
		Kind bestKind = null;
		double bestDistance = Double.MAX_VALUE;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		// For the clipboard: the nearest standing trunk bottom, cut or not.
		BlockPos nearestTrunk = null;
		double nearestTrunkDistance = Double.MAX_VALUE;

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r)
					continue;
				for (int dy = -r; dy <= r; dy++) {
					pos.setWithOffset(home, dx, dy, dz);
					if (isTrunkBottom(pos)) {
						double d = golem.distanceToSqr(Vec3.atCenterOf(pos));
						if (d < nearestTrunkDistance) {
							nearestTrunk = pos.immutable();
							nearestTrunkDistance = d;
						}
					}
					Kind found = kindAt(chest, pos);
					if (found == null || blacklist.containsKey(pos))
						continue;
					double distance = golem.distanceToSqr(Vec3.atCenterOf(pos));
					if (distance >= bestDistance || claimedByOthers(found, pos, now) || found == Kind.TREE && !isTree(pos))
						continue;
					best = pos.immutable();
					bestKind = found;
					bestDistance = distance;
				}
			}
		}

		golem.setTreeStatus(nearestTrunk, nearestTrunk == null ? AndesiteGolem.TreeStatus.NONE
			: treeStatus(chest, nearestTrunk, best));
		if (best == null)
			return false;
		target = best;
		kind = bestKind;
		claimed = claimKey(kind, target);
		renewClaim();
		return true;
	}

	// ---- sharing work between golems

	/** Trees are claimed by the corner of their bottom layer, so no two golems pick logs of one 2x2. */
	private BlockPos claimKey(Kind kind, BlockPos pos) {
		if (kind != Kind.TREE)
			return pos.immutable();
		BlockPos min = pos.immutable();
		for (BlockPos log : baseCluster(pos))
			if (log.getX() < min.getX() || log.getX() == min.getX() && log.getZ() < min.getZ())
				min = log;
		return min;
	}

	private net.minecraft.core.GlobalPos global(BlockPos pos) {
		return net.minecraft.core.GlobalPos.of(golem.level().dimension(), pos);
	}

	private boolean claimedByOthers(Kind kind, BlockPos pos, long now) {
		return AndesiteGolemWorkReservations.SERVER.othersOn(global(claimKey(kind, pos)), golem.getUUID(), now) > 0;
	}

	private void renewClaim() {
		if (claimed != null)
			AndesiteGolemWorkReservations.SERVER.claim(global(claimed), golem.getUUID(), golem.level().getGameTime());
	}

	private void release() {
		claimed = null;
		AndesiteGolemWorkReservations.SERVER.release(golem.getUUID());
	}

	/**
	 * Wrench: drop the current tree/crop (or idle paving spot) and ignore it for SKIP_TICKS.
	 * @return whether there was a job to skip
	 */
	public boolean skip() {
		long until = golem.level().getGameTime() + SKIP_TICKS;
		boolean skipped = false;
		if (target != null && (phase == Phase.GO_TO_TARGET || phase == Phase.WORK)) {
			blacklist.put(target, until);
			if (kind == Kind.TREE)
				for (BlockPos log : baseCluster(target))
					blacklist.put(log, until);
			skipped = true;
		}
		if (pathTarget != null && phase == Phase.GO_TO_PATH) {
			blacklist.put(pathTarget, until);
			skipped = true;
		}
		if (!skipped)
			return false;
		release();
		target = null;
		pathTarget = null;
		golem.setState(CopperGolemState.IDLE);
		golem.getNavigation().stop();
		phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
		nextSearch = Long.MIN_VALUE; // pick the next job right away
		return true;
	}

	/** A log standing on something solid, not on another log: a trunk, not a hanging branch. */
	private boolean isTrunkBottom(BlockPos pos) {
		if (!golem.level().isLoaded(pos) || !golem.level().getBlockState(pos).is(BlockTags.LOGS))
			return false;
		BlockState below = golem.level().getBlockState(pos.below());
		return !below.isAir() && !below.is(BlockTags.LOGS) && !below.is(BlockTags.LEAVES);
	}

	/** Why the golem does or does not cut this trunk, for the clipboard. */
	private AndesiteGolem.TreeStatus treeStatus(AndesiteGolemChestBlockEntity chest, BlockPos trunk, @Nullable BlockPos chosen) {
		List<BlockPos> footprint = baseCluster(trunk);
		if (chosen != null && (footprint.contains(chosen) || baseCluster(chosen).contains(trunk)))
			return AndesiteGolem.TreeStatus.TARGETED;
		if (!isTreeGround(golem.level().getBlockState(trunk.below())))
			return AndesiteGolem.TreeStatus.NO_GROUND;
		if (!chest.accepts(new ItemStack(golem.level().getBlockState(trunk).getBlock())))
			return AndesiteGolem.TreeStatus.FILTER;
		if (footprint.stream().anyMatch(blacklist::containsKey))
			return AndesiteGolem.TreeStatus.UNREACHABLE;
		TreeCutter.Tree tree = findTree(trunk, footprint);
		if (tree == TreeCutter.NO_TREE || !tree.hasLeaves())
			return AndesiteGolem.TreeStatus.NO_LEAVES;
		if (tree.hasBeeHome(golem.level()))
			return AndesiteGolem.TreeStatus.BEES;
		return AndesiteGolem.TreeStatus.WAITING;
	}

	/** Logs with leaves on them and no bee nest or hive; log walls and bee trees are left alone. */
	private boolean isTree(BlockPos base) {
		TreeCutter.Tree tree = findTree(base, baseCluster(base));
		return tree != TreeCutter.NO_TREE && tree.hasLeaves() && !tree.hasBeeHome(golem.level());
	}

	/**
	 * The trunk's bottom layer: the base log plus the logs next to it at the same height. One block
	 * for most trees, 2x2 for dark/pale oak, giant spruce/jungle trees and planted giant oak.
	 */
	private List<BlockPos> baseCluster(BlockPos base) {
		BlockState log = golem.level().getBlockState(base);
		String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(log.getBlock()).getPath();
		if (!GolemWorkGeometry.canHaveWideTrunk(path))
			return List.of(base);
		// Accept only a complete 2x2 of the same wood at the bottom of the trunk.
		// A flood-fill here can inadvertently take down adjacent trees or log builds.
		for (int x = -1; x <= 0; x++)
			for (int z = -1; z <= 0; z++) {
				List<BlockPos> square = List.of(base.offset(x, 0, z), base.offset(x + 1, 0, z),
					base.offset(x, 0, z + 1), base.offset(x + 1, 0, z + 1));
				if (square.stream().allMatch(p -> sameWood(log, golem.level().getBlockState(p))
					&& !golem.level().getBlockState(p.below()).is(BlockTags.LOGS)))
					return square;
			}
		return List.of(base);
	}

	/** Same species of log or wood block (dark_oak_log next to dark_oak_wood), same mod. */
	private static boolean sameWood(BlockState a, BlockState b) {
		if (!b.is(BlockTags.LOGS))
			return false;
		net.minecraft.resources.Identifier ia = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(a.getBlock());
		net.minecraft.resources.Identifier ib = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b.getBlock());
		String wood = GolemWorkGeometry.woodOf(ia.getPath());
		return ia.equals(ib) || wood != null && ia.getNamespace().equals(ib.getNamespace())
			&& wood.equals(GolemWorkGeometry.woodOf(ib.getPath()));
	}

	/**
	 * Create's tree cutter only takes a tree from the last log standing under it, which is how a saw
	 * fells a 2x2 trunk one log at a time. The golem cuts the whole bottom layer at once, so the
	 * rest of that layer is hidden from the cutter as if already sawn.
	 */
	private TreeCutter.Tree findTree(BlockPos base, List<BlockPos> cluster) {
		net.minecraft.world.level.Level level = golem.level();
		// Every 2x2 species, natural dark oak included, uses this masked search: the cutter's
		// first frontier covers the layer above all four base logs, so the canopy stays
		// connected (checks/golem-work/NaturalDarkOakCheck). An unmasked search never
		// returns `base` itself, which left one trunk log standing after each dark oak cut.
		net.minecraft.world.level.BlockGetter masked = new net.minecraft.world.level.BlockGetter() {
			@Override
			public @Nullable net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos pos) {
				return level.getBlockEntity(pos);
			}

			@Override
			public BlockState getBlockState(BlockPos pos) {
				return !pos.equals(base) && cluster.contains(pos) ? Blocks.AIR.defaultBlockState() : level.getBlockState(pos);
			}

			@Override
			public net.minecraft.world.level.material.FluidState getFluidState(BlockPos pos) {
				return level.getFluidState(pos);
			}

			@Override
			public int getHeight() {
				return level.getHeight();
			}

			@Override
			public int getMinY() {
				return level.getMinY();
			}
		};
		return TreeCutter.findTreeForGolem(masked, base, level.getBlockState(base));
	}

	private boolean isValidTarget(AndesiteGolemChestBlockEntity chest, BlockPos pos) {
		return kindAt(chest, pos) == kind;
	}

	/** A tree is targeted at its lowest log, standing on soil. */
	private @Nullable Kind kindAt(AndesiteGolemChestBlockEntity chest, BlockPos pos) {
		if (!golem.level().isLoaded(pos))
			return null;
		BlockState state = golem.level().getBlockState(pos);
		if (isTallPlant(state))
			return isHarvestableTallPlant(pos, state) && chest.accepts(new ItemStack(state.getBlock()))
				? Kind.TALL_PLANT : null;
		// 26.3: an age-8 cactus may grow a flower on top, which stops its growth (CactusBlock#randomTick).
		if (state.is(Blocks.CACTUS_FLOWER))
			return golem.level().getBlockState(pos.below()).is(Blocks.CACTUS) && chest.accepts(new ItemStack(Blocks.CACTUS_FLOWER))
				? Kind.FLOWER : null;
		if (isGrownCrop(state))
			return cropAccepted(chest, state, pos) ? Kind.CROP : null;
		if (state.is(BlockTags.LOGS)) {
			BlockState below = golem.level().getBlockState(pos.below());
			if (below.is(BlockTags.LOGS) || !isTreeGround(below))
				return null;
			return chest.accepts(new ItemStack(state.getBlock())) ? Kind.TREE : null;
		}
		return null;
	}

	/**
	 * What the lowest log of a tree can stand on. Since 26.x #minecraft:dirt holds only dirt,
	 * coarse and rooted dirt: grass, podzol, mycelium and moss are separate tags. Vanilla growth turns
	 * grass under a trunk into dirt but keeps podzol (giant spruce/jungle) and moss; mod and
	 * structure trees often keep grass. Buried giant-tree roots can end on stone, sand or gravel.
	 * Leafless log builds are still left alone by the leaves check in isTree.
	 */
	static boolean isTreeGround(BlockState below) {
		return below.is(BlockTags.SUBSTRATE_OVERWORLD) || below.is(BlockTags.DIRT) || below.is(BlockTags.GRASS_BLOCKS)
			|| below.is(BlockTags.MOSS_BLOCKS) || below.is(BlockTags.MUD) || below.is(Blocks.MUD) || isMangroveRoot(below)
			|| below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(BlockTags.SAND) || below.is(Blocks.GRAVEL);
	}

	private static boolean isTallPlant(BlockState state) {
		return state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS);
	}

	/** Only the rooted bottom of a plant at least three blocks tall is a work target. */
	private boolean isHarvestableTallPlant(BlockPos pos, BlockState state) {
		BlockPos second = pos.above();
		BlockPos third = pos.above(2);
		return !golem.level().getBlockState(pos.below()).is(state.getBlock())
			&& golem.level().isLoaded(second) && golem.level().isLoaded(third)
			&& golem.level().getBlockState(second).is(state.getBlock())
			&& golem.level().getBlockState(third).is(state.getBlock());
	}

	private static boolean isMangroveRoot(BlockState state) {
		return TreeCutter.isRoot(state) || state.is(Blocks.MUDDY_MANGROVE_ROOTS);
	}

	private static boolean isGrownCrop(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop)
			return crop.isMaxAge(state);
		if (state.getBlock() instanceof NetherWartBlock)
			return state.getValue(NetherWartBlock.AGE) == 3;
		return false;
	}

	/** A crop is worth harvesting when the filter takes any of what it drops. */
	private boolean cropAccepted(AndesiteGolemChestBlockEntity chest, BlockState state, BlockPos pos) {
		if (!(golem.level() instanceof ServerLevel level))
			return false;
		for (ItemStack drop : Block.getDrops(state, level, pos, null))
			if (chest.accepts(drop))
				return true;
		return false;
	}

	// ---- doing the work

	private void harvest(AndesiteGolemChestBlockEntity chest, BlockPos pos) {
		List<ItemStack> gathered = new ArrayList<>();
		if (kind == Kind.TREE)
			cutTree(chest, pos, gathered);
		else if (kind == Kind.TALL_PLANT)
			harvestTallPlant(pos, gathered);
		else if (kind == Kind.FLOWER)
			BlockHelper.destroyBlockAs(golem.level(), pos, null, ItemStack.EMPTY, 1, gathered::add); // the cactus below stays
		else
			harvestCrop(chest, pos, gathered);

		// keep what the filter takes, the rest is discarded
		for (ItemStack stack : gathered)
			if (!stack.isEmpty() && chest.accepts(stack))
				golem.carry(stack);
	}

	private void cutTree(AndesiteGolemChestBlockEntity chest, BlockPos base, List<ItemStack> gathered) {
		List<BlockPos> footprint = baseCluster(base);
		TreeCutter.Tree tree = findTree(base, footprint);
		if (tree == TreeCutter.NO_TREE || !tree.hasLeaves() || tree.hasBeeHome(golem.level()))
			return;
		net.minecraft.world.item.Item kind = saplingFor(golem.level().getBlockState(base));

		// the tree found holds everything above the bottom layer; that layer goes first
		for (BlockPos log : footprint)
			BlockHelper.destroyBlockAs(golem.level(), log, null, ItemStack.EMPTY, 1, gathered::add);
		tree.destroyBlocks(golem.level(), golem, (p, stack) -> gathered.add(stack));
		// Mangroves can have a column of roots immediately below the trunk. The cutter's
		// upward search does not always include those roots; clear only this footprint.
		for (BlockPos trunk : footprint) {
			BlockPos root = trunk.below();
			for (int depth = 0; depth < 6 && isMangroveRoot(golem.level().getBlockState(root)); depth++, root = root.below())
				BlockHelper.destroyBlockAs(golem.level(), root, null, ItemStack.EMPTY, 1, gathered::add);
		}

		// replant the whole footprint with one kind of sapling, taken from the tree first, then the chest;
		// the same kind as the tree when there is one, a 2x2 tree only grows back when all four are planted
		if (kind == null)
			return; // Unknown wood: never plant a different species.
		for (BlockPos trunk : footprint) {
			BlockPos spot = groundSpot(trunk);
			if (spot == null)
				continue;
			ItemStack sapling = takeSapling(chest, gathered, spot, kind);
			if (sapling.isEmpty())
				continue;
			plantSapling(sapling, spot);
		}
	}

	/**
	 * Where a sapling goes for a trunk position: the trunk spot itself, or further down when the
	 * tree stood on roots that were cut with it (mangrove). Null if there is no free spot.
	 */
	private @Nullable BlockPos groundSpot(BlockPos trunk) {
		BlockPos pos = trunk;
		for (int i = 0; i < 6 && golem.level().getBlockState(pos).isAir(); i++) {
			if (!golem.level().getBlockState(pos.below()).isAir())
				return pos;
			pos = pos.below();
		}
		return golem.level().getBlockState(pos).isAir() ? pos : null;
	}

	/** The sapling a log grows from: oak_log -> oak_sapling, mangrove_log -> mangrove_propagule. */
	private static net.minecraft.world.item.@Nullable Item saplingFor(BlockState log) {
		net.minecraft.resources.Identifier id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(log.getBlock());
		String path = id.getPath();
		String wood = GolemWorkGeometry.woodOf(path);
		if (wood == null)
			return null;
		for (String suffix : new String[] { "_sapling", "_propagule", "_fungus" }) {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM
				.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), wood + suffix));
			if (item.isPresent())
				return item.get();
		}
		return null;
	}


	/** One sapling of the given kind (any kind when null) that can grow at the spot. */
	private ItemStack takeSapling(AndesiteGolemChestBlockEntity chest, List<ItemStack> gathered, BlockPos spot,
		@Nullable net.minecraft.world.item.Item kind) {
		for (ItemStack stack : gathered) {
			if (stack.isEmpty() || (kind != null && !stack.is(kind)) || !canPlantSapling(stack, spot))
				continue;
			return stack.split(1);
		}
		return chest.takeOne(stack -> (kind == null || stack.is(kind)) && canPlantSapling(stack, spot));
	}

	private boolean canPlantSapling(ItemStack stack, BlockPos pos) {
		return (stack.is(ItemTags.SAPLINGS) || stack.is(net.minecraft.world.item.Items.MANGROVE_PROPAGULE))
			&& stack.getItem() instanceof BlockItem blockItem
			&& blockItem.getBlock().defaultBlockState().canSurvive(golem.level(), pos);
	}

	private boolean plantSapling(ItemStack stack, BlockPos pos) {
		if (!canPlantSapling(stack, pos))
			return false;
		BlockState sapling = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
		golem.level().setBlockAndUpdate(pos, sapling);
		golem.level().playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1, 1);
		return true;
	}

	/** At normal three-block height, cut the top two from the top down so updates cannot break upper blocks as
	 * uncaptured world drops. Extra-tall columns also lose every segment above the base.
	 * Never touch the bottom block: it remains rooted to regrow. */
	private void harvestTallPlant(BlockPos root, List<ItemStack> gathered) {
		Block block = golem.level().getBlockState(root).getBlock();
		List<BlockPos> upper = new ArrayList<>();
		BlockPos pos = root.above();
		for (; golem.level().isLoaded(pos) && golem.level().getBlockState(pos).is(block); pos = pos.above())
			upper.add(pos);
		// A cactus flower on top would pop off as an uncaptured world drop once its cactus is cut.
		if (golem.level().getBlockState(pos).is(Blocks.CACTUS_FLOWER))
			upper.add(pos);
		for (int i = upper.size() - 1; i >= 0; i--)
			BlockHelper.destroyBlockAs(golem.level(), upper.get(i), null, ItemStack.EMPTY, 1, gathered::add);
	}

	private void harvestCrop(AndesiteGolemChestBlockEntity chest, BlockPos pos, List<ItemStack> gathered) {
		BlockState state = golem.level().getBlockState(pos);
		ItemStack seed = new ItemStack(state.getBlock());
		MutableBoolean seedTaken = new MutableBoolean(false);
		BlockHelper.destroyBlockAs(golem.level(), pos, null, ItemStack.EMPTY, 1, stack -> {
			if (!seedTaken.booleanValue() && ItemHelper.sameItem(stack, seed)) {
				stack.shrink(1);
				seedTaken.setTrue();
			}
			gathered.add(stack);
		});
		if (seedTaken.isFalse() && chest.takeOne(stack -> ItemHelper.sameItem(stack, seed)).isEmpty())
			return;

		BlockState replanted = state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(0)
			: state.setValue(NetherWartBlock.AGE, 0);
		if (golem.level().getBlockState(pos).isAir() && replanted.canSurvive(golem.level(), pos))
			golem.level().setBlockAndUpdate(pos, replanted);
	}

	private void deposit(AndesiteGolemChestBlockEntity chest) {
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
