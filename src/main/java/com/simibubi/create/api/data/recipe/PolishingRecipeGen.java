package com.simibubi.create.api.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;


import com.simibubi.create.AllRecipeTypes;

import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;

/**
 * The base class for Polishing recipe generation.
 * Addons should extend this and use the {@link ProcessingRecipeGen#create} methods
 * to make recipes.
 * For an example of how you might do this, see Create's implementation: {@link com.simibubi.create.foundation.data.recipe.CreatePolishingRecipeGen}.
 * Needs to be added to a registered recipe provider to do anything, see {@link com.simibubi.create.foundation.data.recipe.CreateRecipeProvider}
 */
public abstract class PolishingRecipeGen extends StandardProcessingRecipeGen<SandPaperPolishingRecipe> {

	public PolishingRecipeGen(BootstrapContext<Recipe<?>> recipeContext, BootstrapContext<Advancement> advancementContext, String defaultNamespace) {
		super(recipeContext, advancementContext, defaultNamespace);
	}

	@Override
	protected AllRecipeTypes getRecipeType() {
		return AllRecipeTypes.SANDPAPER_POLISHING;
	}

}
