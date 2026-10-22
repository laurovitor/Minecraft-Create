package com.simibubi.create.content.electricity.accumulator;

import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.connectivity.ConnectivityHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ModularAccumulatorCTBehaviour extends HorizontalCTBehaviour {

	public ModularAccumulatorCTBehaviour() {
		super(AllSpriteShifts.ACCUMULATOR, AllSpriteShifts.ACCUMULATOR_TOP);
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return state.getBlock() == other.getBlock() && ConnectivityHandler.isConnected(reader, pos, otherPos);
	}
}