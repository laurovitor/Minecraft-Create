package com.simibubi.create.compat.jei.category;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.simibubi.create.AllItems;
import com.simibubi.create.compat.jei.category.animations.AnimatedBlazeBurner;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.electricity.recipe.LiquidBurningRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Liquid Blaze Burner fuels. Ported from Create Crafts &amp; Additions (MIT, by MRH0). */
@NullMarked
public class LiquidBurningCategory extends CreateRecipeCategory<LiquidBurningRecipe> {

	private final AnimatedBlazeBurner heater = new AnimatedBlazeBurner();

	public LiquidBurningCategory(Info<LiquidBurningRecipe> info) {
		super(info);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, LiquidBurningRecipe recipe, IFocusGroup focuses) {
		int center = getWidth() / 2;
		List<ItemStack> buckets = fluidsOf(recipe.getFluidInput()).stream()
			.map(fluid -> new ItemStack(fluid.getFluid()
				.getBucket()))
			.filter(stack -> !stack.isEmpty() && !stack.is(Items.AIR))
			.toList();
		builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, center - 56, 3)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStack(AllItems.STRAW.asStack());
		builder.addSlot(RecipeIngredientRole.INPUT, center - 36, 3)
			.setBackground(getRenderedSlot(), -1, -1)
			.addItemStacks(buckets);
		addFluidSlot(builder, center - 16, 3, recipe.getFluidInput());
	}

	@Override
	public void draw(LiquidBurningRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor graphics,
		double mouseX, double mouseY) {
		int center = getWidth() / 2;
		HeatCondition heat = recipe.isSuperheated() ? HeatCondition.SUPERHEATED : HeatCondition.HEATED;
		graphics.text(Minecraft.getInstance().font, ElectricityUtil.formatTime(recipe.getBurnTime()), center + 48, 36,
			0xFFFFFFFF, false);
		AllGuiTextures.JEI_LIGHT.render(graphics, 81, 38);
		AllGuiTextures.JEI_HEAT_BAR.render(graphics, 4, 30);
		graphics.text(Minecraft.getInstance().font, CreateLang.translateDirect(heat.getTranslationKey()), 9, 36,
			heat.getColor(), false);
		heater.withHeat(heat.visualizeAsBlazeBurner())
			.draw(graphics, center + 3, 5);
		AllGuiTextures.JEI_DOWN_ARROW.render(graphics, center + 3, 8);
	}
}
