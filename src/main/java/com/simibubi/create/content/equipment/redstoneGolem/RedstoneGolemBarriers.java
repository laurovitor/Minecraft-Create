package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;

/** Opens path barriers just ahead, then closes only those opened by this golem after passing. */
public class RedstoneGolemBarriers {
	private final RedstoneGolem golem;
	private final Set<BlockPos> opened = new HashSet<>();
	private final Set<BlockPos> passed = new HashSet<>();

	public RedstoneGolemBarriers(RedstoneGolem golem) { this.golem = golem; }

	public void tick() {
		Level level = golem.level();
		Path path = golem.getNavigation().getPath();
		if (path != null && !path.isDone()) {
			for (int i = path.getNextNodeIndex(); i < Math.min(path.getNodeCount(), path.getNextNodeIndex() + 3); i++) {
				BlockPos pos = path.getNodePos(i);
				if (golem.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 6.25)
					continue;
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof DoorBlock) {
					BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
					open(lower);
				} else if (state.getBlock() instanceof FenceGateBlock || state.getBlock() instanceof TrapDoorBlock)
					open(pos);
			}
		}
		for (BlockPos pos : Set.copyOf(opened)) {
			if (!clearOf(pos))
				passed.add(pos);
			if (passed.contains(pos) && clearOf(pos)) {
				close(pos);
				opened.remove(pos);
				passed.remove(pos);
			}
		}
	}

	/** Do not close a barrier on the golem, including the upper half of a door. */
	private boolean clearOf(BlockPos pos) {
		BlockState state = golem.level().getBlockState(pos);
		AABB space = new AABB(pos);
		if (state.getBlock() instanceof DoorBlock) space = space.minmax(new AABB(pos.above()));
		return !golem.getBoundingBox().inflate(.1).intersects(space);
	}

	private void open(BlockPos pos) {
		if (opened.contains(pos)) return;
		BlockState state = golem.level().getBlockState(pos);
		if (state.getBlock() instanceof DoorBlock door && !state.getValue(DoorBlock.OPEN)) {
			door.setOpen(golem, golem.level(), state, pos, true);
			opened.add(pos);
		} else if (state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN)
			&& golem.level().getBlockState(pos.below()).isFaceSturdy(golem.level(), pos.below(), net.minecraft.core.Direction.UP)) {
			golem.level().setBlock(pos, state.setValue(TrapDoorBlock.OPEN, true), Block.UPDATE_ALL);
			opened.add(pos);
		} else if (state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN)) {
			golem.level().setBlock(pos, state.setValue(FenceGateBlock.OPEN, true), Block.UPDATE_ALL);
			opened.add(pos);
		}
	}

	private void close(BlockPos pos) {
		BlockState state = golem.level().getBlockState(pos);
		if (state.getBlock() instanceof DoorBlock door && state.getValue(DoorBlock.OPEN))
			door.setOpen(golem, golem.level(), state, pos, false);
		else if (state.getBlock() instanceof TrapDoorBlock && state.getValue(TrapDoorBlock.OPEN))
			golem.level().setBlock(pos, state.setValue(TrapDoorBlock.OPEN, false), Block.UPDATE_ALL);
		else if (state.getBlock() instanceof FenceGateBlock && state.getValue(FenceGateBlock.OPEN))
			golem.level().setBlock(pos, state.setValue(FenceGateBlock.OPEN, false), Block.UPDATE_ALL);
	}

	public void stop() {
		for (BlockPos pos : Set.copyOf(opened))
			if (clearOf(pos)) {
				close(pos);
				opened.remove(pos);
				passed.remove(pos);
			}
	}
}
