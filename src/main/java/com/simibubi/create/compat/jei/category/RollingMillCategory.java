package com.simibubi.create.compat.jei.category;

import org.jspecify.annotations.NullMarked;

import com.simibubi.create.compat.jei.category.animations.AnimatedRollingMill;
import com.simibubi.create.content.kinetics.rollingMill.RollingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@NullMarked
public class RollingMillCategory extends CreateRecipeCategory<RollingRecipe> {

	private final AnimatedRollingMill rollingMill = new AnimatedRollingMill();

	public RollingMillCategory(Info<RollingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, RollingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 15, 9)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredient());

		ProcessingOutput output = recipe.getRollableResults()
			.getFirst();
		builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 27)
			.setBackground(getRenderedSlot(output), -1, -1)
			.addItemStack(output.getStack())
			.addRichTooltipCallback(addStochasticTooltip(output));
	}

	@Override
	public void draw(RollingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
		rollingMill.draw(graphics, 48, 27);
	}
}
