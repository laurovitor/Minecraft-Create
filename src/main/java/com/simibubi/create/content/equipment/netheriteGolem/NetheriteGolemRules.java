package com.simibubi.create.content.equipment.netheriteGolem;

import com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules;

/** Side-effect-free policy used by combat, chest and packed-movement paths. */
public final class NetheriteGolemRules {
	private NetheriteGolemRules() {}

	/** Daytime sleep window per day cycle, ticks: isDay spans 23500 -> 24000 -> 12500 = 13000. */
	public static final int DAY_WINDOW_TICKS = 13000;
	/**
	 * Ticks of sleep for a 0 -> 100% heal (shared with the Andesite golem, see GolemCareRules).
	 * Walking home eats into each 13000-tick day window, so a realistic day gives ~11000 ticks of
	 * sleep; two of them = 22000. With 80 max HP: 80 / 22000 = 0.00364 HP/tick = 0.0727 HP/s =
	 * ~3.6 HP per in-game hour (1000 ticks). Even a perfect day (13000 ticks asleep) restores only
	 * 47.3 HP (59%), so a full heal always needs two days of undisturbed sleep.
	 */
	public static final int FULL_HEAL_SLEEP_TICKS = GolemCareRules.FULL_HEAL_SLEEP_TICKS;
	/** How often sleep regeneration is applied, ticks (20 = once a second, 0.0727 HP at 80 max). */
	public static final int REGEN_INTERVAL = GolemCareRules.REGEN_INTERVAL;
	/** A target out of sight this long, ticks, is dropped (5 s): purple eyes turn white again. */
	public static final int TARGET_UNSEEN_TICKS = 100;
	/** One full Minecraft day, ticks: longer than this away from the chest teleports the golem home. */
	public static final long AWAY_TELEPORT_TICKS = 24000;
	/** Retry delay, ticks, when the chest has no free spot to teleport onto (10 s). */
	public static final long TELEPORT_RETRY_TICKS = 200;
	/** After taking damage the golem stays awake (no sleep, no regen) this many ticks (10 s). */
	public static final int WAKE_AFTER_HURT_TICKS = 200;
	/** Eyes stay purple this many ticks after the golem's last hit on a mob (5 s). */
	public static final int COMBAT_LINGER_TICKS = 100;

	/** A creeper marked by a Netherite golem (targeted or hit) stays "in combat" this long, ticks (10 s). */
	public static final long CREEPER_COMBAT_TICKS = 200;
	/** A creeper this close to a Netherite golem that targets it counts as in combat, blocks. */
	public static final double CREEPER_NEAR_GOLEM = 4.0;
	/** Persistent-data key (game time of the last mark); saved with the creeper. */
	public static final String CREEPER_COMBAT_KEY = "CreateNetheriteGolemCombat";

	public enum Eyes { CLOSED, WHITE, PURPLE }
	public static boolean inTerritory(int dx, int dy, int dz) {
		return (long)dx * dx + (long)dy * dy + (long)dz * dz <= 25L * 25;
	}
	public static boolean isDay(long overworldClock) {
		long time = Math.floorMod(overworldClock, 24000);
		return time < 12500 || time >= 23500;
	}
	public static boolean shouldTarget(boolean hostile, boolean alive, boolean inTerritory) {
		return hostile && alive && inTerritory; // Chest filter has no say in targeting.
	}
	public static boolean acceptsLoot(boolean filterEmpty, boolean filterMatches) {
		return filterEmpty || filterMatches;
	}
	public static double movementSpeed(double health, double maxHealth) {
		int steps = Math.min(10, Math.max(0, (int)Math.floor((1 - health / maxHealth) * 10 + 1e-6)));
		return .22 * (1 - .05 * steps);
	}
	public static int killsAfterMove(int previousKills, boolean sameLocation) {
		return sameLocation ? Math.max(0, previousKills) : 0;
	}
	public static int deathIngots(boolean chance, boolean secondIngot) {
		return chance ? (secondIngot ? 2 : 1) : 0;
	}

	/** Day, nothing hostile targeted and not recently hit: walk home and sleep. */
	public static boolean wantsRest(boolean day, boolean hasTarget, int wakeCooldown) {
		return day && !hasTarget && wakeCooldown <= 0;
	}
	/** HP healed per {@link #REGEN_INTERVAL}; nothing while awake. */
	public static float sleepRegen(boolean asleep, float maxHealth) {
		return GolemCareRules.sleepRegen(asleep, maxHealth);
	}
	/** No visible hostile left: keep chasing the current target only while it was seen recently. */
	public static boolean keepUnseenTarget(boolean hasTarget, int unseenTicks) {
		return hasTarget && unseenTicks < TARGET_UNSEEN_TICKS;
	}
	/** On the chest or in one of the eight columns around it, feet from one below to two above. */
	public static boolean nearChest(int dx, int dy, int dz) {
		return Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && dy >= -1 && dy <= 2;
	}
	public static long nextAwayTicks(long awayTicks, boolean nearChest) {
		return nearChest ? 0 : awayTicks + 1;
	}
	public static boolean shouldTeleportHome(long awayTicks) {
		return awayTicks > AWAY_TELEPORT_TICKS;
	}
	/** Live target, or hit a mob within the last {@link #COMBAT_LINGER_TICKS}; -1 = never hit one. */
	public static boolean inCombat(boolean liveTarget, int ticksSinceAttack) {
		return liveTarget || ticksSinceAttack >= 0 && ticksSinceAttack < COMBAT_LINGER_TICKS;
	}
	/** Explosion of a creeper fighting a Netherite golem: no blocks broken. -1 = never marked. */
	public static boolean creeperInGolemCombat(boolean targetsGolem, long ticksSinceMark, boolean nearGolemTargetingIt) {
		return targetsGolem || nearGolemTargetingIt || ticksSinceMark >= 0 && ticksSinceMark <= CREEPER_COMBAT_TICKS;
	}
	public static Eyes eyes(boolean sleeping, boolean combat) {
		return sleeping ? Eyes.CLOSED : combat ? Eyes.PURPLE : Eyes.WHITE;
	}
}
