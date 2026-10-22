package com.simibubi.create.content.electricity.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

public record FluidRecipeInput(FluidStack fluid) implements RecipeInput {
    @Override public ItemStack getItem(int index) { return ItemStack.EMPTY; }
    @Override public int size() { return 0; }
    @Override public boolean isEmpty() { return fluid.isEmpty(); }
}
