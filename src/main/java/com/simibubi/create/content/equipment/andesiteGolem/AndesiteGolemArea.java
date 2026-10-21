package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.LinkedHashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lauro edition: when a golem is made, the ground on the edge of its work area and right around its
 * chest turns into dirt path, so the player can see where the golem works. Only soil (grass, dirt,
 * podzol, mycelium...) with open air above changes; nothing else is touched. Picking the chest up and
 * putting it back does not do this again.
 */
public class AndesiteGolemArea {

	/** How far up and down from the chest the edge looks for the ground. */
	private static final int SEARCH_HEIGHT = 10;

	public static void markBoundary(LevelAccessor level, BlockPos chest) {
		Set<BlockPos> columns = new LinkedHashSet<>();
		int r = AndesiteGolem.WORK_RADIUS;
		// a closed one-block ring at the work radius
		for (int a = -r; a <= r; a++) {
			int b = (int) Math.round(Math.sqrt(r * r - a * a));
			columns.add(chest.offset(a, 0, b));
			columns.add(chest.offset(a, 0, -b));
			columns.add(chest.offset(b, 0, a));
			columns.add(chest.offset(-b, 0, a));
		}
		for (BlockPos column : columns)
			pave(level, surface(level, column));

		// the eight ground blocks beside the alloy block (now the chest). The alloy
		// may be flush with the soil or placed on top of it.
		for (int dx = -1; dx <= 1; dx++)
			for (int dz = -1; dz <= 1; dz++)
				if (dx != 0 || dz != 0)
					paveAdjacent(level, chest.offset(dx, 0, dz));
	}

	private static void paveAdjacent(LevelAccessor level, BlockPos beside) {
		if (canPave(level, beside))
			pave(level, beside);
		else
			pave(level, beside.below());
	}

	private static boolean canPave(LevelAccessor level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT_PATH) && !state.is(Blocks.FARMLAND)
			&& level.getBlockState(pos.above()).isAir();
	}

	private static BlockPos surface(LevelAccessor level, BlockPos column) {
		// Canopies and other non-air blocks may be above the ground. Find the first
		// eligible soil with open air above, not merely the first non-air block.
		for (int dy = SEARCH_HEIGHT; dy >= -SEARCH_HEIGHT; dy--) {
			BlockPos pos = column.offset(0, dy, 0);
			BlockState state = level.getBlockState(pos);
			if (state.is(BlockTags.DIRT) && !state.is(Blocks.FARMLAND)
				&& level.getBlockState(pos.above()).isAir())
				return pos;
		}
		return column;
	}

	private static void pave(LevelAccessor level, BlockPos pos) {
		if (canPave(level, pos))
			level.setBlock(pos, Blocks.DIRT_PATH.defaultBlockState(), Block.UPDATE_ALL);
	}
}
