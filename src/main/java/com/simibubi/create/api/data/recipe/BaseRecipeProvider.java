package com.simibubi.create.api.data.recipe;

import net.minecraft.advancements.Advancement;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.simibubi.create.Create;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * A class containing some basic setup for other recipe generators to use.
 * Addons should extend this if they add a custom recipe type that is not
 * a processing recipe type and want to use Create's helpers.
 * For processing recipes extend {@link StandardProcessingRecipeGen}.
 */
public abstract class BaseRecipeProvider extends RecipeProvider {
	private static boolean boundComponents;
	protected final String modid;
	protected final BootstrapContext<Recipe<?>> recipeContext;
	protected final HolderGetter<Item> items;
	protected final List<GeneratedRecipe> all = new ArrayList<>();

	public BaseRecipeProvider(BootstrapContext<Recipe<?>> recipeContext, BootstrapContext<Advancement> advancementContext, String defaultNamespace) {
		super(recipeContext, advancementContext);
		this.modid = defaultNamespace;
		this.recipeContext = recipeContext;
		this.items = recipeContext.lookup(Registries.ITEM);
		bindItemComponents(recipeContext);
	}

	protected Identifier asResource(String path) {
		return Identifier.fromNamespaceAndPath(modid, path);
	}

	/**
	 * Recipe bootstraps run before GatherDataEvent supplies a lookup future. Rebuild the
	 * provider view from the static and already-bootstrapped world registries exposed by
	 * this recipe context, so item defaults are bound before a generator makes ItemStacks.
	 */
	private static void bindItemComponents(BootstrapContext<Recipe<?>> context) {
		if (boundComponents)
			return;

		List<ResourceKey<? extends Registry<?>>> keys = Stream.concat(
			RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY).listRegistryKeys(),
			DataPackRegistriesHooks.getWorldRegistriesWithDimensions().map(data -> data.key()))
			.distinct()
			.filter(key -> context.holderLookup(key).isPresent())
			.toList();

		HolderLookup.Provider registries = new HolderLookup.Provider() {
			@Override
			public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
				return keys.stream();
			}

			@Override
			public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
				return context.holderLookup(key).map(lookup -> {
					if (lookup instanceof HolderLookup.RegistryLookup<?> registryLookup)
						return (HolderLookup.RegistryLookup<T>) registryLookup;
					throw new IllegalStateException("Recipe bootstrap lookup is not a registry lookup: " + key);
				});
			}
		};
		bindItemComponents(registries);
	}

	/**
	 * An item's default components are bound when a server loads its datapacks, which never happens
	 * during datagen - so anything that asks an item about itself fails on "Components not bound yet".
	 * Bind them from the bootstrap context's static and world registry lookups.
	 * <p>
	 * One wrinkle: those registries answer a tag lookup with an unbound placeholder, an anonymous
	 * subclass of HolderSet.Named. NeoForge only whitelists Named itself as a component value, so say
	 * up front that the placeholder is fine too.
	 */
	public static void bindItemComponents(HolderLookup.Provider registries) {
		if (boundComponents)
			return;

		CommonHooks.markComponentClassAsValid(HolderSet.emptyNamed(new HolderOwner<>() {
		}, TagKey.create(Registries.ITEM, Create.asResource("datagen_placeholder")))
			.getClass());

		BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registries)
			.forEach(DataComponentInitializers.PendingComponents::apply);
		boundComponents = true;
	}

	/**
	 * 26.2's Ingredient wraps an already-resolved set of items, so a tag has to be looked up on a
	 * getter that has tags bound - the provider's own - rather than named on the ingredient.
	 */
	protected Ingredient ingredient(TagKey<Item> tag) {
		return Ingredient.of(items.getOrThrow(tag));
	}

	protected GeneratedRecipe register(GeneratedRecipe recipe) {
		all.add(recipe);
		return recipe;
	}

	@Override
	public void buildRecipes() {
		all.forEach(c -> c.register(output));
		Create.LOGGER.info("{} registered {} recipe{}", getName(), all.size(), all.size() == 1 ? "" : "s");
	}

	/**
	 * A name for the recipe registration log.
	 */
	public String getName() {
		return getClass().getSimpleName();
	}

	@FunctionalInterface
	public interface GeneratedRecipe {
		void register(RecipeOutput recipeOutput);
	}
}
