package com.simibubi.create.content.electricity.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** CC&A charging processing recipe, namespaced natively under create. */
public class ChargingRecipe extends ProcessingRecipe<SingleRecipeInput, ChargingRecipeParams> {
    public ChargingRecipe(ChargingRecipeParams params) { super(AllRecipeTypes.CHARGING, params); }
    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && !ingredients.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }
    @Override protected int getMaxInputCount() { return 1; }
    @Override protected int getMaxOutputCount() { return 1; }
    public int getEnergy() { return params.getEnergy(); }
    public int getMaxChargeRate() { return params.getMaxChargeRate(); }
    public ItemStack getResultStack() { return getRollableResults().getFirst().getStack(); }
}
