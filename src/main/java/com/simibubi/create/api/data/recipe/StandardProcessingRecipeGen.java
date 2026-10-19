package com.simibubi.create.api.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;


import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;

import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.Identifier;

/**
 * A base class for {@link StandardProcessingRecipe}, containing helper methods
 * for datagenning processing recipes.
 * <p>
 * Addons should extend this for custom processing recipe that extends {@link StandardProcessingRecipe},
 * and return the recipe type in {@link #getRecipeType()}.
 */
public abstract class StandardProcessingRecipeGen<R extends StandardProcessingRecipe<?>> extends ProcessingRecipeGen<ProcessingRecipeParams, R, StandardProcessingRecipe.Builder<R>> {
	public StandardProcessingRecipeGen(BootstrapContext<Recipe<?>> recipeContext, BootstrapContext<Advancement> advancementContext, String defaultNamespace) {
		super(recipeContext, advancementContext, defaultNamespace);
	}

	@SuppressWarnings("unchecked")
	protected StandardProcessingRecipe.Serializer<R> getSerializer() {
		return (StandardProcessingRecipe.Serializer<R>) getRecipeType().getProcessingSerializer();
	}

	@Override
	protected Builder<R> getBuilder(Identifier id) {
		return new StandardProcessingRecipe.Builder<>(getSerializer().factory(), id);
	}
}
