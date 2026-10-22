package com.simibubi.create.content.electricity;

import com.simibubi.create.Create;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AllElectricEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Create.ID);
    public static final DeferredHolder<MobEffect, MobEffect> SHOCKING = EFFECTS.register("shocking", () ->
        new ShockingEffect().addAttributeModifier(Attributes.MOVEMENT_SPEED, Create.asResource("shocking"),
            -1.0d, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }
}
