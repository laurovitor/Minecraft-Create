package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lauro edition: the items each golem kind keeps in its personal slots, by registry id. Pure, so the
 * offline checks use the same table. Keys are GolemKind ids.
 */
public final class GolemMaintenance {
	private GolemMaintenance() {}

	public static final Map<String, List<String>> ITEMS = Map.of(
		"andesite", List.of("create:andesite_alloy", "minecraft:glass_bottle"),
		"netherite", List.of("minecraft:netherite_ingot", "minecraft:ancient_debris"),
		"redstone", List.of("minecraft:redstone"));

	public static boolean uses(String kind, String itemId) {
		return ITEMS.getOrDefault(kind, List.of()).contains(itemId);
	}

	/** Anything some golem kind uses: what the Redstone distribution column accepts. */
	public static boolean anyUses(String itemId) {
		return ITEMS.values().stream().anyMatch(l -> l.contains(itemId));
	}

	public static Set<String> kinds() {
		return ITEMS.keySet();
	}
}
