package com.simibubi.create.content.electricity.tesla;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.processing.AssemblyOperatorBlockItem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Places the Tesla Coil like a Mechanical Press: clicking the top of a belt, depot or ejector puts the
 * coil two blocks above it, hanging down over the spot it charges.
 */
public class TeslaCoilBlockItem extends AssemblyOperatorBlockItem {

	public TeslaCoilBlockItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	protected boolean operatesOn(LevelReader world, BlockPos pos, BlockState placedOnState) {
		if (AllBlocks.BELT.has(placedOnState))
			return placedOnState.getValue(BeltBlock.SLOPE) == BeltSlope.HORIZONTAL;
		return AllBlocks.DEPOT.has(placedOnState) || AllBlocks.WEIGHTED_EJECTOR.has(placedOnState);
	}

}
