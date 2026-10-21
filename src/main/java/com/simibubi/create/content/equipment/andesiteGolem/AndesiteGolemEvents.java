package com.simibubi.create.content.equipment.andesiteGolem;

import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Hostile mobs hunt Andesite Golems, so their work area has to be lit and guarded. */
@EventBusSubscriber
public class AndesiteGolemEvents {

	@SubscribeEvent
	public static void huntGolems(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide())
			return;
		if (!(event.getEntity() instanceof Monster monster) || monster instanceof NeutralMob)
			return;
		monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, AndesiteGolem.class, true));
	}
}
