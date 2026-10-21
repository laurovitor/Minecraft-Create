package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lauro edition: a carved pumpkin (or jack o'lantern) placed on an andesite alloy block makes an
 * Andesite Golem, the same way a copper block makes a copper golem. The alloy block becomes the
 * golem's chest. Hooked into CarvedPumpkinBlock by CarvedPumpkinBlockMixin.
 */
public class AndesiteGolemSpawning {

	public static boolean matches(LevelReader level, BlockPos pumpkinPos) {
		BlockState pumpkin = level.getBlockState(pumpkinPos);
		return (pumpkin.is(Blocks.CARVED_PUMPKIN) || pumpkin.is(Blocks.JACK_O_LANTERN))
			&& AllBlocks.ANDESITE_ALLOY_BLOCK.has(level.getBlockState(pumpkinPos.below()));
	}

	/** @return true if a golem was made */
	public static boolean trySpawn(Level level, BlockPos pumpkinPos) {
		if (level.isClientSide() || !matches(level, pumpkinPos))
			return false;
		AndesiteGolem golem = AllEntityTypes.ANDESITE_GOLEM.get().create(level, EntitySpawnReason.TRIGGERED);
		if (golem == null)
			return false;

		BlockState pumpkin = level.getBlockState(pumpkinPos);
		Direction facing = pumpkin.getValue(CarvedPumpkinBlock.FACING);
		BlockPos chestPos = pumpkinPos.below();

		level.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.levelEvent(2001, pumpkinPos, Block.getId(pumpkin));
		level.setBlock(chestPos, AllBlocks.ANDESITE_GOLEM_CHEST.getDefaultState()
			.setValue(AndesiteGolemChestBlock.FACING, facing)
			.setValue(AndesiteGolemChestBlock.ACTIVE, true), Block.UPDATE_ALL);

		golem.snapTo(pumpkinPos.getX() + 0.5, pumpkinPos.getY() + 0.05, pumpkinPos.getZ() + 0.5, facing.toYRot(), 0);
		golem.setChest(chestPos);
		if (level.getBlockEntity(chestPos) instanceof AndesiteGolemChestBlockEntity chest)
			chest.linkGolem(golem.getUUID());
		AndesiteGolemArea.markBoundary(level, chestPos);
		level.addFreshEntity(golem);
		golem.playSpawnSound();

		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0)))
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
		level.updateNeighborsAt(pumpkinPos, Blocks.AIR);
		return true;
	}
}
