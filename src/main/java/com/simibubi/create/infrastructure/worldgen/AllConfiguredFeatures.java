package com.simibubi.create.infrastructure.worldgen;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

public class AllConfiguredFeatures {
	public static final ResourceKey<Feature>
				ZINC_ORE = key("zinc_ore"),
				STRIATED_ORES_OVERWORLD = key("striated_ores_overworld"),
				STRIATED_ORES_NETHER = key("striated_ores_nether");

	private static ResourceKey<Feature> key(String name) {
		return ResourceKey.create(Registries.FEATURE, Create.asResource(name));
	}

	public static void bootstrap(BootstrapContext<Feature> ctx) {
		RuleTest stoneOreReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
		RuleTest deepslateOreReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

		List<BlockReplacement> zincTargetStates = List.of(
			BlockReplacement.replace(stoneOreReplaceables, AllBlocks.ZINC_ORE.get()
				.defaultBlockState()),
			BlockReplacement.replace(deepslateOreReplaceables, AllBlocks.DEEPSLATE_ZINC_ORE.get()
				.defaultBlockState())
		);

		ctx.register(ZINC_ORE, new OreFeature(zincTargetStates, 12));

		List<LayerPattern> overworldLayerPatterns = List.of(
			AllLayerPatterns.SCORIA.get(),
			AllLayerPatterns.CINNABAR.get(),
			AllLayerPatterns.MAGNETITE.get(),
			AllLayerPatterns.MALACHITE.get(),
			AllLayerPatterns.LIMESTONE.get(),
			AllLayerPatterns.OCHRESTONE.get()
		);

		ctx.register(STRIATED_ORES_OVERWORLD, new LayeredOreFeature(new LayeredOreConfiguration(overworldLayerPatterns, 32, 0)));

		List<LayerPattern> netherLayerPatterns = List.of(
			AllLayerPatterns.SCORIA_NETHER.get(),
			AllLayerPatterns.SCORCHIA_NETHER.get()
		);

		ctx.register(STRIATED_ORES_NETHER, new LayeredOreFeature(new LayeredOreConfiguration(netherLayerPatterns, 32, 0)));
	}
}
