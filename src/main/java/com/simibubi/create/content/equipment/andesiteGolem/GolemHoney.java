package com.simibubi.create.content.equipment.andesiteGolem;

/** Lauro edition: what the Andesite golem takes from a full hive. Pure, checked offline. */
public final class GolemHoney {
	private GolemHoney() {}

	/** Honeycomb from a full hive, as vanilla shears give (loot table harvest_beehive: 3). */
	public static final int COMBS = 3;

	public enum Product { BOTTLE, COMBS, NONE }

	/** A glass bottle in its personal slots makes a honey bottle; otherwise combs. The filter decides. */
	public static Product product(boolean bottleInPersonal, boolean acceptsHoneyBottle, boolean acceptsCombs) {
		if (bottleInPersonal && acceptsHoneyBottle)
			return Product.BOTTLE;
		return acceptsCombs ? Product.COMBS : Product.NONE;
	}
}
