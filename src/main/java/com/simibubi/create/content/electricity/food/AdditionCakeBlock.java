package com.simibubi.create.content.electricity.food;

import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Seven-slice cake with Crafts & Additions' richer three-food, 0.3-saturation bites. */
public class AdditionCakeBlock extends CakeBlock {
	public AdditionCakeBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.isClientSide())
			return InteractionResult.CONSUME;
		if (!player.canEat(false))
			return InteractionResult.PASS;
		player.awardStat(Stats.EAT_CAKE_SLICE);
		player.getFoodData().eat(3, .3f);
		int bites = state.getValue(BITES);
		if (bites < 6)
			level.setBlock(pos, state.setValue(BITES, bites + 1), 3);
		else
			level.removeBlock(pos, false);
		return InteractionResult.SUCCESS;
	}
}
