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
 * The Andesite Golem's job: find a grown tree or crop near its chest, walk there, take it down,
 * replant, walk back and store what the chest filter accepts. Everything else is discarded.
 */
public class AndesiteGolemWorkGoal extends Goal {

	private enum Phase { GO_TO_TARGET, WORK, RETURN, DEPOSIT, DONE }

	private enum Kind { TREE, CROP }

	/** Ticks spent chopping/harvesting and storing, roughly the copper golem chest animations. */
	private static final int WORK_TICKS = 30;
	private static final int DEPOSIT_TICKS = 20;
	/** How often the golem looks for new work while it has none. */
	private static final int SEARCH_INTERVAL = 40;
	/** Gives up on a target it cannot reach after this long, and skips it for a while. */
	private static final int GIVE_UP_TICKS = 300;
	private static final int BLACKLIST_TICKS = 1200;
	private static final double REACH = 2.5;
	/** How often a golem walking to a tree checks whether leaves are blocking it. */
	private static final int LEAF_CHECK_INTERVAL = 20;

	private final AndesiteGolem golem;
	private final Map<BlockPos, Long> blacklist = new HashMap<>();

	private Phase phase = Phase.DONE;
	private @Nullable BlockPos target;
	private Kind kind = Kind.CROP;
	private int timer;
	private int travelTicks;
	private int searchCooldown;
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
		// things to store, and room for them
		if (!golem.getCarried().isEmpty())
			return chest.hasEmptySlot();
		if (searchCooldown-- > 0)
			return false;
		searchCooldown = SEARCH_INTERVAL;
		if (!chest.hasEmptySlot())
			return false;
		return findTarget(chest);
	}

	@Override
	public boolean canContinueToUse() {
		return phase != Phase.DONE && golem.getChest() != null;
	}

	@Override
	public void start() {
		travelTicks = 0;
		lastDistance = Double.MAX_VALUE;
		phase = golem.getCarried().isEmpty() ? Phase.GO_TO_TARGET : Phase.RETURN;
	}

	@Override
	public void stop() {
		phase = Phase.DONE;
		target = null;
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
			if (target == null || !isValidTarget(chest, target)) {
				phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
				return;
			}
			if (walkTo(target)) {
				phase = Phase.WORK;
				timer = WORK_TICKS;
				golem.setState(CopperGolemState.GETTING_ITEM);
				return;
			}
			if (kind == Kind.TREE && phase == Phase.GO_TO_TARGET)
				clearLeavesIfStuck(chest);
		}
		case WORK -> {
			golem.getLookControl().setLookAt(Vec3.atCenterOf(target));
			if (--timer > 0)
				return;
			golem.setState(CopperGolemState.IDLE);
			if (isValidTarget(chest, target))
				harvest(chest, target);
			target = null;
			travelTicks = 0;
			phase = golem.getCarried().isEmpty() ? Phase.DONE : Phase.RETURN;
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
			if (phase == Phase.GO_TO_TARGET && target != null)
				blacklist.put(target, golem.level().getGameTime() + BLACKLIST_TICKS);
			phase = Phase.DONE;
			return false;
		}
		if (travelTicks % 20 == 1 || golem.getNavigation().isDone()) {
			Path path = golem.getNavigation().createPath(pos, 1);
			golem.getNavigation().moveTo(path, 1.0);
		}
		return false;
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

		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				if (dx * dx + dz * dz > r * r)
					continue;
				for (int dy = -r; dy <= r; dy++) {
					pos.setWithOffset(home, dx, dy, dz);
					Kind found = kindAt(chest, pos);
					if (found == null || blacklist.containsKey(pos))
						continue;
					double distance = golem.distanceToSqr(Vec3.atCenterOf(pos));
					if (distance >= bestDistance)
						continue;
					best = pos.immutable();
					bestKind = found;
					bestDistance = distance;
				}
			}
		}

		if (best == null)
			return false;
		if (bestKind == Kind.TREE && !isTree(best)) {
			blacklist.put(best, now + BLACKLIST_TICKS);
			return false;
		}
		target = best;
		kind = bestKind;
		return true;
	}

	/** Logs with leaves on them and no bee nest or hive; log walls and bee trees are left alone. */
	private boolean isTree(BlockPos base) {
		TreeCutter.Tree tree = findTree(base, baseCluster(base));
		return tree != TreeCutter.NO_TREE && tree.hasLeaves() && !tree.hasBeeHome(golem.level());
	}

	/**
	 * The trunk's bottom layer: the base log plus the logs next to it at the same height. One block
	 * for most trees, 2x2 for dark oak, pale oak, giant spruce and giant jungle trees.
	 */
	private List<BlockPos> baseCluster(BlockPos base) {
		BlockState log = golem.level().getBlockState(base);
		String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(log.getBlock()).getPath();
		if (!List.of("dark_oak_log", "pale_oak_log", "spruce_log", "jungle_log").contains(path))
			return List.of(base);
		// Accept only a complete 2x2 of the same wood at the bottom of the trunk.
		// A flood-fill here can inadvertently take down adjacent trees or log builds.
		for (int x = -1; x <= 0; x++)
			for (int z = -1; z <= 0; z++) {
				List<BlockPos> square = List.of(base.offset(x, 0, z), base.offset(x + 1, 0, z),
					base.offset(x, 0, z + 1), base.offset(x + 1, 0, z + 1));
				if (square.stream().allMatch(p -> golem.level().getBlockState(p).is(log.getBlock())
					&& !golem.level().getBlockState(p.below()).is(BlockTags.LOGS)))
					return square;
			}
		return List.of(base);
	}

	/**
	 * Create's tree cutter only takes a tree from the last log standing under it, which is how a saw
	 * fells a 2x2 trunk one log at a time. The golem cuts the whole bottom layer at once, so the
	 * rest of that layer is hidden from the cutter as if already sawn.
	 */
	private TreeCutter.Tree findTree(BlockPos base, List<BlockPos> cluster) {
		net.minecraft.world.level.Level level = golem.level();
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
		return TreeCutter.findTree(masked, base, level.getBlockState(base));
	}

	private boolean isValidTarget(AndesiteGolemChestBlockEntity chest, BlockPos pos) {
		return kindAt(chest, pos) == kind;
	}

	/** A tree is targeted at its lowest log, standing on soil. */
	private @Nullable Kind kindAt(AndesiteGolemChestBlockEntity chest, BlockPos pos) {
		if (!golem.level().isLoaded(pos))
			return null;
		BlockState state = golem.level().getBlockState(pos);
		if (isGrownCrop(state))
			return cropAccepted(chest, state, pos) ? Kind.CROP : null;
		if (state.is(BlockTags.LOGS)) {
			BlockState below = golem.level().getBlockState(pos.below());
			// on soil, or on roots like the mangrove
			if (below.is(BlockTags.LOGS) || !(below.is(BlockTags.DIRT) || TreeCutter.isRoot(below)))
				return null;
			return chest.accepts(new ItemStack(state.getBlock())) ? Kind.TREE : null;
		}
		return null;
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

		// replant the whole footprint with one kind of sapling, taken from the tree first, then the chest;
		// the same kind as the tree when there is one, a 2x2 tree only grows back when all four are planted
		for (BlockPos trunk : footprint) {
			BlockPos spot = groundSpot(trunk);
			if (spot == null)
				continue;
			ItemStack sapling = takeSapling(chest, gathered, spot, kind);
			if (sapling.isEmpty())
				continue;
			kind = sapling.getItem();
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
		String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4)
			: path.endsWith("_stem") ? path.substring(0, path.length() - 5) : null;
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
		return stack.is(ItemTags.SAPLINGS) && stack.getItem() instanceof BlockItem blockItem
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
