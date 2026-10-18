package com.simibubi.create.infrastructure.worldgen;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.Create;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.jetbrains.annotations.ApiStatus.Internal;

public class AllFeatures {
	private static final DeferredRegister<MapCodec<? extends Feature>> REGISTER = DeferredRegister.create(BuiltInRegistries.FEATURE_TYPE, Create.ID);

	public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<LayeredOreFeature>> LAYERED_ORE = REGISTER.register("layered_ore", () -> LayeredOreFeature.CODEC);

	@Internal
	public static void register(IEventBus modEventBus) {
		REGISTER.register(modEventBus);
	}
}
