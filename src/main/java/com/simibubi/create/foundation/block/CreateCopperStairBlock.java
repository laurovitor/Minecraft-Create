package com.simibubi.create.foundation.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

public class CreateCopperStairBlock extends StairBlock {
	public CreateCopperStairBlock(Properties properties) {
		super(Blocks.AIR.defaultBlockState(), properties);
	}

	@Override
	public float getExplosionResistance() {
		return explosionResistance;
	}
}
