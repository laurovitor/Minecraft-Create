package com.simibubi.create.content.equipment.andesiteGolem;

import net.minecraft.core.BlockPos;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Idle work: convert only soil immediately beside the home chest. */
public class AndesiteGolemArea {

	public static @Nullable BlockPos nextPaveSpot(LevelAccessor level, BlockPos chest, Predicate<BlockPos> canReach) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0)
					continue;
				BlockPos beside = chest.offset(dx, 0, dz);
				if (canPave(level, beside) && canReach.test(beside.above()))
					return beside;
				if (canPave(level, beside.below()) && canReach.test(beside))
					return beside.below();
			}
		return null;
	}

	public static boolean canPave(LevelAccessor level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT_PATH) && !state.is(Blocks.FARMLAND)
			&& level.getBlockState(pos.above()).isAir();
	}

	public static void pave(LevelAccessor level, BlockPos pos) {
		if (canPave(level, pos))
			level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
	}
}
