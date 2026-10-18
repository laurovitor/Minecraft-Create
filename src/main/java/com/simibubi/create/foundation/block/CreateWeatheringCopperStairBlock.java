package com.simibubi.create.foundation.block;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopperStairBlock;

public class CreateWeatheringCopperStairBlock extends WeatheringCopperStairBlock {
	public CreateWeatheringCopperStairBlock(WeatherState weatherState, Properties properties) {
		super(weatherState, Blocks.AIR.defaultBlockState(), properties);
	}

	@Override
	public float getExplosionResistance() {
		return explosionResistance;
	}
}
