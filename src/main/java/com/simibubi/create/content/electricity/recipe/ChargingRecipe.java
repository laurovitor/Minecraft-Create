package com.simibubi.create.content.electricity.recipe;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.sequencedAssembly.ChargingAssemblySubCategory;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;

/** Tesla Coil charging recipe; also usable as a sequenced assembly step. Ported from Create Crafts &amp; Additions. */
public class ChargingRecipe extends ProcessingRecipe<SingleRecipeInput, ChargingRecipeParams> implements IAssemblyRecipe {

	public ChargingRecipe(ChargingRecipeParams params) {
		super(AllRecipeTypes.CHARGING, params);
	}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		return !input.isEmpty() && !ingredients.isEmpty() && ingredients.getFirst()
			.test(input.getItem(0));
	}

	@Override
	protected int getMaxInputCount() {
		return 1;
	}

	@Override
	protected int getMaxOutputCount() {
		return 1;
	}

	public int getEnergy() {
		return params.getEnergy();
	}

	public int getMaxChargeRate() {
		return params.getMaxChargeRate();
	}

	public ItemStack getResultStack() {
		return getRollableResults().getFirst()
			.getStack();
	}

	@Override
	public Component getDescriptionForAssembly() {
		return CreateLang.translateDirect("recipe.assembly.charging", ElectricityUtil.format(getEnergy()));
	}

	@Override
	public void addRequiredMachines(Set<ItemLike> list) {
		list.add(AllBlocks.TESLA_COIL.get());
	}

	@Override
	public void addAssemblyIngredients(List<Ingredient> list) {}

	@Override
	public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
		return () -> ChargingAssemblySubCategory::new;
	}
}
