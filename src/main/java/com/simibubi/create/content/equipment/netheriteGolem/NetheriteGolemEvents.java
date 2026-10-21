package com.simibubi.create.content.equipment.netheriteGolem;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

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

	/**
	 * A creeper fighting a Netherite golem blows up without breaking blocks (so no block drops);
	 * sound, particles, entity damage and knockback stay. Every other explosion is untouched.
	 */
	@SubscribeEvent public static void creeperBlastSparesBlocks(ExplosionEvent.Detonate event) {
		if (event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper && inGolemCombat(creeper))
			event.getAffectedBlocks().clear();
	}

	static boolean inGolemCombat(Creeper creeper) {
		long marked = creeper.getPersistentData().getLongOr(NetheriteGolemRules.CREEPER_COMBAT_KEY, Long.MIN_VALUE);
		long since = marked == Long.MIN_VALUE ? -1 : creeper.level().getGameTime() - marked;
		double r = NetheriteGolemRules.CREEPER_NEAR_GOLEM;
		boolean near = !creeper.level().getEntitiesOfClass(NetheriteGolem.class, creeper.getBoundingBox().inflate(r),
			golem -> golem.getTarget() == creeper && golem.distanceTo(creeper) <= r).isEmpty();
		return NetheriteGolemRules.creeperInGolemCombat(creeper.getTarget() instanceof NetheriteGolem, since, near);
	}

	/** Called by the golem while it targets or hits a creeper. */
	static void markCombat(Creeper creeper) {
		creeper.getPersistentData().putLong(NetheriteGolemRules.CREEPER_COMBAT_KEY, creeper.level().getGameTime());
	}
}
