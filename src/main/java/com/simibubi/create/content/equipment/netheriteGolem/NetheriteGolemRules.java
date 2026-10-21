package com.simibubi.create.content.equipment.netheriteGolem;

/** Side-effect-free policy used by combat, chest and packed-movement paths. */
public final class NetheriteGolemRules {
	private NetheriteGolemRules() {}
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
}
