package com.simibubi.create.compat.jei.category;

import org.jspecify.annotations.NullMarked;

import com.simibubi.create.compat.jei.category.animations.AnimatedTeslaCoil;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.electricity.recipe.ChargingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Tesla Coil charging recipes. Ported from Create Crafts &amp; Additions (MIT, by MRH0). */
@NullMarked
public class ChargingCategory extends CreateRecipeCategory<ChargingRecipe> {

	private final AnimatedTeslaCoil teslaCoil = new AnimatedTeslaCoil();

	public ChargingCategory(Info<ChargingRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, ChargingRecipe recipe, IFocusGroup focuses) {
		builder.addSlot(RecipeIngredientRole.INPUT, 15, 9)
			.setBackground(getRenderedSlot(), -1, -1)
			.addIngredients(recipe.getIngredients()
				.getFirst());

		ProcessingOutput output = recipe.getRollableResults()
			.getFirst();
		builder.addSlot(RecipeIngredientRole.OUTPUT, 139, 27)
			.setBackground(getRenderedSlot(output), -1, -1)
			.addItemStack(output.getStack())
			.addRichTooltipCallback(addStochasticTooltip(output));
	}

	@Override
	public void draw(ChargingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
		double mouseX, double mouseY) {
		AllGuiTextures.JEI_ARROW.render(graphics, 85, 32);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, 43, 4);
		teslaCoil.draw(graphics, 48, 27);
		graphics.text(Minecraft.getInstance().font, ElectricityUtil.format(recipe.getEnergy()) + "⚡", 86, 9,
			0xFFFFFFFF, false);
	}
}
