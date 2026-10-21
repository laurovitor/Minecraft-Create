package com.simibubi.create.content.equipment.netheriteGolem;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber
public class NetheriteGolemEvents {
	@SubscribeEvent public static void countKill(LivingDeathEvent event) {
		if (event.getEntity() instanceof Enemy && event.getSource().getEntity() instanceof NetheriteGolem golem)
			golem.countKill();
	}
	@SubscribeEvent public static void collectDrops(LivingDropsEvent event) {
		if (!(event.getEntity() instanceof Enemy) || !(event.getSource().getEntity() instanceof NetheriteGolem golem)) return;
		NetheriteGolemChestBlockEntity chest = golem.getChest();
		if (chest == null) return;
		var it = event.getDrops().iterator();
		while (it.hasNext()) {
			ItemEntity drop = it.next();
			if (!chest.accepts(drop.getItem())) continue; // no filter accepts all; targeting never checks this.
			var remainder = chest.insert(drop.getItem().copy());
			if (remainder.isEmpty()) it.remove();
			else drop.setItem(remainder); // Preserve overflow as a normal world drop.
		}
	}
}
