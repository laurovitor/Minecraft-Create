package com.simibubi.create.infrastructure.data;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.AllPotatoProjectileTypes;
import com.simibubi.create.infrastructure.worldgen.AllBiomeModifiers;
import com.simibubi.create.infrastructure.worldgen.AllConfiguredFeatures;
import com.simibubi.create.infrastructure.worldgen.AllPlacedFeatures;

import net.minecraft.core.Cloner;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;


import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class GeneratedEntriesProvider extends DatapackBuiltinEntriesProvider {
	private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
		.add(Registries.ENCHANTMENT, AllEnchantments::bootstrap)
		.add(Registries.DAMAGE_TYPE, AllDamageTypes::bootstrap)
		.add(Registries.FEATURE, AllConfiguredFeatures::bootstrap)
		.add(Registries.PLACED_FEATURE, AllPlacedFeatures::bootstrap)
		.add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, AllBiomeModifiers::bootstrap)
		.add(CreateRegistries.POTATO_PROJECTILE_TYPE, AllPotatoProjectileTypes::bootstrap);

	private static CompletableFuture<RegistrySetBuilder.PatchedRegistries> createWorldLookup(CompletableFuture<HolderLookup.Provider> registries) {
		// Vanilla's RegistryPatchGenerator only registers vanilla world registry codecs with
		// the cloner. NeoForge adds data-pack registries such as context_int_provider.
		return registries.thenApply(parent -> {
			Cloner.Factory cloner = new Cloner.Factory();
			DataPackRegistriesHooks.getWorldRegistriesWithDimensions()
				.forEach(registry -> registry.runWithArguments(cloner::addCodec));
			RegistryDataLoader.RELOADABLE_REGISTRIES.stream()
				.forEach(registry -> registry.runWithArguments(cloner::addCodec));
			return BUILDER.buildPatch(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), parent, cloner);
		});
	}

	public GeneratedEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, "Create's Generated Registry Entries", DataPackRegistriesHooks.getWorldRegistriesWithDimensions().toList(),
			createWorldLookup(registries), Set.of(Create.ID));
	}
}
