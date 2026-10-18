package com.simibubi.create.content.fluids.potion;

import com.simibubi.create.foundation.item.ItemHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class PotionMixingRecipes {

	public static final List<Item> SUPPORTED_CONTAINERS = List.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION);

	private static List<RecipeHolder<MixingRecipe>> RECIPES;
	private static Map<Item, List<MixingRecipe>> SORTED;

	private static boolean alreadyGenerated = false;
	private static boolean alreadySorted = false;

	public static List<RecipeHolder<MixingRecipe>> createRecipes(Level level) {
		if (!alreadyGenerated) {
			RECIPES = createRecipesImpl(level);
			alreadyGenerated = true;
		}

		return RECIPES;
	}

	public static Map<Item, List<MixingRecipe>> sortRecipesByItem(Level level) {
		if (!alreadySorted) {
			SORTED = sortRecipesByItem(createRecipes(level));
			alreadySorted = true;
		}

		return SORTED;
	}

	private static List<RecipeHolder<MixingRecipe>> createRecipesImpl(Level level) {
		List<RecipeHolder<MixingRecipe>> mixingRecipes = new ArrayList<>();
		List<Reference<Potion>> potions = level.registryAccess()
			.lookupOrThrow(Registries.POTION)
			.listElements()
			.toList();
		int recipeIndex = 0;

		// Brewing is data-driven in 26.3. Include vanilla and modded brewing recipes alike.
		for (Reference<Recipe<?>> holder : level.registryAccess()
			.lookupOrThrow(Registries.RECIPE)
			.listElements().toList()) {
			if (!(holder.value() instanceof BrewingRecipe recipe))
				continue;
			ItemStack output = recipe.getOutput().create();
			if (!SUPPORTED_CONTAINERS.contains(output.getItem()))
				continue;
			Ingredient reagent = recipe.getReagent().ingredient();
			if (reagent.isEmpty() || recipe.getReagent().potions().isPresent())
				continue;

			for (Item container : SUPPORTED_CONTAINERS) {
				for (Reference<Potion> potion : potions) {
					ItemStack input = PotionContents.createItemStack(container, potion);
					if (!recipe.getInput().test(input))
						continue;
					FluidStack inputFluid = PotionFluidHandler.getFluidFromPotionItem(input);
					FluidStack outputFluid = PotionFluidHandler.getFluidFromPotionItem(output);
					inputFluid.setAmount(1000);
					outputFluid.setAmount(1000);
					mixingRecipes.add(createRecipe("potion_mixing_" + recipeIndex++, reagent, inputFluid, outputFluid));
				}
			}
		}

		return mixingRecipes;
	}

	private static RecipeHolder<MixingRecipe> createRecipe(String id, Ingredient ingredient, FluidStack fromFluid, FluidStack toFluid) {
		Identifier recipeId = Create.asResource(id);
		MixingRecipe recipe = new Builder<>(MixingRecipe::new, recipeId)
				.require(ingredient)
			.require(new SizedFluidIngredient(DataComponentFluidIngredient.of(false, fromFluid), fromFluid.getAmount()))
				.output(toFluid)
				.requiresHeat(HeatCondition.HEATED)
				.build();

		return new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, recipeId), recipe);
	}

	private static Map<Item, List<MixingRecipe>> sortRecipesByItem(List<RecipeHolder<MixingRecipe>> all) {
		Map<Item, List<MixingRecipe>> byItem = new HashMap<>();
		Set<Item> processedItems = new HashSet<>();
		for (RecipeHolder<MixingRecipe> recipe : all) {
			for (Ingredient ingredient : recipe.value().getIngredients()) {
				for (ItemStack itemStack : ItemHelper.getItems(ingredient)) {
					Item item = itemStack.getItem();
					if (processedItems.add(item)) {
						byItem.computeIfAbsent(item, i -> new ArrayList<>())
							.add(recipe.value());
					}
				}
			}
			processedItems.clear();
		}
		return byItem;
	}

}
