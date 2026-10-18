package com.simibubi.create.infrastructure.worldgen;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.Create;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jetbrains.annotations.ApiStatus.Internal;

public class AllPlacementModifiers {
	private static final DeferredRegister<MapCodec<? extends PlacementModifier>> REGISTER = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Create.ID);

	public static final DeferredHolder<MapCodec<? extends PlacementModifier>, MapCodec<ConfigPlacementFilter>> CONFIG_FILTER = REGISTER.register("config_filter", () -> ConfigPlacementFilter.CODEC);

	@Internal
	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
