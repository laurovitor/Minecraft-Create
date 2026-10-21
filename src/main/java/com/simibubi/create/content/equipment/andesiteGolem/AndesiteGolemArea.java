package com.simibubi.create.content.equipment.andesiteGolem;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Paves only the eight immediately adjacent soil blocks whenever the chest appears. */
public class AndesiteGolemArea {

	public static void paveAroundChest(LevelAccessor level, BlockPos chest) {
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++) {
				if (dx == 0 && dz == 0)
					continue;
				BlockPos beside = chest.offset(dx, 0, dz);
				if (canPave(level, beside))
					pave(level, beside);
				else if (canPave(level, beside.below()))
					pave(level, beside.below());
			}
	}

	private static boolean canPave(LevelAccessor level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT_PATH) && !state.is(Blocks.FARMLAND)
			&& level.getBlockState(pos.above()).isAir();
	}

	private static void pave(LevelAccessor level, BlockPos pos) {
		level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
	}
}
