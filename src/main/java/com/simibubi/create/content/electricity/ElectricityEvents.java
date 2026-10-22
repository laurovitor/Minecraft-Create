package com.simibubi.create.content.electricity;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.electricity.burner.LiquidBlazeBurnerBlock;
import com.simibubi.create.content.electricity.network.WireGraph;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server-side hooks of the wire graph that backs electric networks, and other electricity world hooks. */
@EventBusSubscriber
public class ElectricityEvents {

	@SubscribeEvent
	public static void onLevelTick(LevelTickEvent.Pre event) {
		if (event.getLevel().isClientSide())
			return;
		WireGraph.tick(event.getLevel());
	}

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk)
			WireGraph.chunkLoaded(level, chunk);
	}

	@SubscribeEvent
	public static void onLevelUnload(LevelEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level)
			WireGraph.levelUnloaded(level);
	}

	/** Right-clicking a Blaze Burner with Straw turns it into a Liquid Blaze Burner, as in Crafts &amp; Additions. */
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!AllItems.STRAW.isIn(event.getItemStack()))
			return;
		Level level = event.getLevel();
		BlockPos pos = event.getPos();
		BlockState state = level.getBlockState(pos);
		if (!AllBlocks.BLAZE_BURNER.has(state))
			return;
		if (!level.isClientSide()) {
			level.setBlockAndUpdate(pos, AllBlocks.LIQUID_BLAZE_BURNER.getDefaultState()
				.setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING)
				.setValue(LiquidBlazeBurnerBlock.FACING, state.getValue(BlazeBurnerBlock.FACING)));
			if (!event.getEntity()
				.isCreative())
				event.getItemStack()
					.shrink(1);
		}
		event.setCancellationResult(InteractionResult.SUCCESS);
		event.setCanceled(true);
	}
}
