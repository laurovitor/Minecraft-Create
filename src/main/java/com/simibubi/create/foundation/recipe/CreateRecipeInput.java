package com.simibubi.create.foundation.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;

/** Adapts Create's item resource inventory to Minecraft's recipe input API. */
public record CreateRecipeInput(ItemStacksResourceHandler inventory) implements RecipeInput {
	@Override
	public ItemStack getItem(int index) {
		return ItemUtil.getStack(inventory, index);
	}

	@Override
	public int size() {
		return inventory.size();
	}
}
