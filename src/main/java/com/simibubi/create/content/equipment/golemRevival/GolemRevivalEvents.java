package com.simibubi.create.content.equipment.golemRevival;

import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber
public class GolemRevivalEvents {

	/** Every golem kind extends AndesiteGolem; remember deaths its chest could not hear about. */
	@SubscribeEvent
	public static void recordUnheardDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof AndesiteGolem golem) || !(golem.level() instanceof ServerLevel level))
			return;
		BlockPos chest = golem.getChestPos();
		if (chest != null && !level.isLoaded(chest))
			GolemGraveyard.get(level.getServer()).bury(golem.getUUID());
	}
}
