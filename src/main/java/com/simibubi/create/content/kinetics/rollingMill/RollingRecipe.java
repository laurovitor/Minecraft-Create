package com.simibubi.create.content.kinetics.rollingMill;

import org.jspecify.annotations.NullMarked;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

/**
 * Rolling Mill recipe: one input item rolled into one output stack (sheets into rods, rods into wires).
 * Ported from Create Crafts &amp; Additions (MIT, by MRH0).
 */
@NullMarked
public class RollingRecipe extends StandardProcessingRecipe<RecipeInput> {

	public RollingRecipe(ProcessingRecipeParams params) {
		super(AllRecipeTypes.ROLLING, params);
	}

	public Ingredient getIngredient() {
		return ingredients.get(0);
	}

	@Override
	public boolean matches(RecipeInput inv, Level level) {
		if (inv.isEmpty())
			return false;
		return ingredients.get(0)
			.test(inv.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	public ItemStack getResultStack() {
		return getRollableResults().getFirst()
			.getStack();
	}
}
