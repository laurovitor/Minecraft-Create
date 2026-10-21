package com.simibubi.create.content.equipment.andesiteGolem;

/** Lauro edition: side-effect-free repair and sleep-healing policy shared by both golems. */
public final class GolemCareRules {
	private GolemCareRules() {}

	/**
	 * Health restored by one repair item, as a fraction of max health; like the vanilla iron golem
	 * (an iron ingot heals 25 of 100). Andesite alloy: 3 of 12 HP. Netherite ingot: 20 of 80 HP.
	 */
	public static final float REPAIR_FRACTION = 0.25f;
	/** Ticks between two repairs from the chest's reserve (5 s): visible, never instant. */
	public static final int AUTO_REPAIR_COOLDOWN = 100;
	/** No repair from the chest until this many ticks after the last damage taken (5 s). */
	public static final int AUTO_REPAIR_CALM_TICKS = 100;
	/**
	 * Ticks of sleep for a 0 -> 100% heal. A sleep window is at most 11000 (Andesite: night
	 * 12500-23500) to 13000 ticks (Netherite: day 23500-12500); 22000 = two realistic windows.
	 * Netherite, 80 HP: 0.00364 HP/tick, ~3.6 HP per in-game hour. Andesite, 12 HP: 0.000545 HP/tick,
	 * ~0.55 HP per in-game hour, 6 HP per full night. Raise to slow regen, lower to speed it up.
	 */
	public static final int FULL_HEAL_SLEEP_TICKS = 22000;
	/** How often sleep regeneration is applied, ticks (20 = once a second). */
	public static final int REGEN_INTERVAL = 20;

	public static float repairAmount(float maxHealth) {
		return maxHealth * REPAIR_FRACTION;
	}

	/** HP healed per {@link #REGEN_INTERVAL}; nothing while awake. */
	public static float sleepRegen(boolean asleep, float maxHealth) {
		return asleep ? maxHealth * REGEN_INTERVAL / FULL_HEAL_SLEEP_TICKS : 0;
	}

	/**
	 * Takes one repair item from the chest only when it would not be wasted (a full heal step is
	 * missing), the golem is out of combat, calm since its last hit and off cooldown.
	 */
	public static boolean shouldAutoRepair(float health, float maxHealth, boolean inCombat,
		int ticksSinceHurt, int cooldown) {
		return cooldown <= 0 && !inCombat && ticksSinceHurt >= AUTO_REPAIR_CALM_TICKS
			&& maxHealth - health >= repairAmount(maxHealth) - 1e-4f;
	}
}
