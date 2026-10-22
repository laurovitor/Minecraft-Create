package com.simibubi.create.content.electricity;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** The CC&A electrical stun effect. The movement modifier is registered in AllElectricEffects. */
public class ShockingEffect extends MobEffect {
    public ShockingEffect() {
        super(MobEffectCategory.HARMFUL, 15453236);
    }
}
