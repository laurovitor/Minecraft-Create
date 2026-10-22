package com.simibubi.create.content.electricity.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

/** CC&A fluid fuel recipe for the Liquid Blaze Burner. */
public class LiquidBurningRecipe extends ProcessingRecipe<FluidRecipeInput, LiquidBurningRecipeParams> {
    public LiquidBurningRecipe(LiquidBurningRecipeParams params) { super(AllRecipeTypes.LIQUID_BURNING, params); }
    @Override public boolean matches(FluidRecipeInput input, Level level) {
        return !input.fluid().isEmpty() && !fluidIngredients.isEmpty() && fluidIngredients.getFirst().test(input.fluid());
    }
    @Override protected int getMaxInputCount() { return 0; }
    @Override protected int getMaxOutputCount() { return 0; }
    @Override protected int getMaxFluidInputCount() { return 1; }
    public SizedFluidIngredient getFluidInput() { return fluidIngredients.getFirst(); }
    public int getBurnTime() { return params.getBurnTime(); }
    public boolean isSuperheated() { return params.isSuperheated(); }
}
