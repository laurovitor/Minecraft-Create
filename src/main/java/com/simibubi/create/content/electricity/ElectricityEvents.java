package com.simibubi.create.content.electricity;

import com.simibubi.create.content.electricity.network.WireGraph;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Server-side hooks of the wire graph that backs electric networks. */
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
}
