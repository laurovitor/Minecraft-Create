package com.simibubi.create.content.equipment.armor;

import java.util.Map;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.generators.RegistrateItemModelGenerator;

import com.simibubi.create.Create;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Item models for armour that can carry a trim.
 * <p>
 * 26.3 generates a model for each trim material with a slot-specific trim sprite prefix.
 * Create supplies its own cardboard trim sprites below; vanilla uses the same generator for its armour.
 */
public class TrimmableArmorModelGenerator {

	public static <T extends BaseArmorItem> void generate(DataGenContext<Item, T> c, RegistrateItemModelGenerator p) {
		T item = c.get();
		p.generateTrimmableItem(item, slotTrimPrefix(item.getArmorType()), false, Map.of());
	}

	/**
	 * Create draws its cardboard trims from its own sprites rather than the vanilla ones, so the prefix
	 * the trim layer is built from points into Create's namespace.
	 */
	private static Identifier slotTrimPrefix(ArmorType type) {
		return Create.asResource("trims/items/card_" + switch (type) {
			case HELMET -> "helmet";
			case CHESTPLATE, BODY -> "chestplate";
			case LEGGINGS -> "leggings";
			case BOOTS -> "boots";
		} + "_trim");
	}

}
