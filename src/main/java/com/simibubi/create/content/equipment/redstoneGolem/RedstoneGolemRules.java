package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/** Lauro edition: side-effect-free policy of the Redstone Golem, checked offline. */
public final class RedstoneGolemRules {
	private RedstoneGolemRules() {}

	/** Max health: the most fragile golem (Andesite 12, Netherite 80). */
	public static final double MAX_HEALTH = 8.0;
	/** Base movement speed: the fastest golem (Andesite 0.20, Netherite 0.22). */
	public static final double BASE_SPEED = 0.25;
	/** Navigation speed modifier while running to a source chest: 0.25 * 1.5 = 0.375. */
	public static final double RUN_SPEED = 1.5;
	/** Navigation speed modifier while walking home with the load. */
	public static final double WALK_SPEED = 1.0;
	/** Stacks (inventory slots) the golem carries per trip. */
	public static final int CARRY_SLOTS = 4;
	/**
	 * Network chests farther than this from the golem's chest are ignored, blocks (3D; was 48). Used
	 * for collecting, distribution targets and the latch tint (all via RedstoneGolemNetwork#sources).
	 */
	public static final int MAX_SOURCE_DISTANCE = 128;
	/**
	 * Navigation reach, blocks: the path length budget (and FOLLOW_RANGE) for 128-block sources with
	 * detours. Vanilla searches up to length * 16 * RedstoneGolemNavigation multiplier (4) = 10240 nodes.
	 */
	public static final float PATH_LENGTH = 160;
	/** Paths computed per stand-spot refresh (nearest spots first): bounds the cost of long searches. */
	public static final int MAX_STAND_SPOT_PATHS = 4;
	/**
	 * How often an idle golem looks at its network again, in GAME ticks (1 s). Vanilla only calls
	 * Goal#canUse every second tick, so the goal schedules by game time, not by canUse calls (the
	 * old call-counted 40 meant ~80 game ticks = 4 s). A change in its own chest wakes it at once.
	 */
	public static final int SEARCH_INTERVAL = 20;
	/** A chest-change wake re-plans at most this often, game ticks (0.25 s): a hopper feeding the chest
	 * cannot force a network scan every goal update. */
	public static final int WAKE_MIN_GAP = 5;
	/** Gives up walking to a source after this long, ticks (60 s), and skips it for BLACKLIST_TICKS. */
	public static final int GIVE_UP_TICKS = 1200;
	/** Walking with no 0.5-block progress for this long gives up early, ticks (10 s). */
	public static final int STALL_TICKS = 200;
	/** A wrench-skipped source is ignored this long, game ticks (30 s). */
	public static final int SKIP_TICKS = 600;
	/** Unreachable source skipped this long, game ticks (10 s; was 60 s), then retried. */
	public static final int BLACKLIST_TICKS = 200;
	/** Network source list reused this long while walking, game ticks (1 s); rescanned at once on a wake. */
	public static final int SOURCE_CACHE_TICKS = 20;
	/**
	 * A golem's claim on its source chest lasts this long without renewal, game ticks (2 s). Renewed
	 * every tick while walking there; released on arrival, give-up and goal stop, so a dead or
	 * unloaded golem's claim lapses on its own.
	 */
	public static final int RESERVATION_TTL = 40;

	/**
	 * Fullness of a chest: occupied slots / total slots, 0..1. Slots, not item counts: the golem
	 * empties slots, so the chest with the most occupied slots gives the most per trip.
	 */
	public static double fullness(int occupiedSlots, int totalSlots) {
		return totalSlots <= 0 ? 0 : (double) Math.max(0, occupiedSlots) / totalSlots;
	}

	/**
	 * One network chest as seen by the golem. {@code reservedByOthers}: other golems already heading
	 * there. {@code occupiedSlots} counts stacks this golem could take (fit in its own chest).
	 */
	public record Candidate(int occupiedSlots, int totalSlots, double distanceSqr, boolean skipped, int reservedByOthers) {
		public Candidate(int occupiedSlots, int totalSlots, double distanceSqr, boolean skipped) {
			this(occupiedSlots, totalSlots, distanceSqr, skipped, 0);
		}
	}

	/**
	 * A chest already claimed by {@code reservers} golems has room for one more only when its stacks
	 * exceed what they will take: more than CARRY_SLOTS per golem already going.
	 */
	/** Idle decision timing: plan when the interval passed, or on a wake once WAKE_MIN_GAP passed. */
	public static boolean shouldPlan(long now, long lastPlan, boolean woken) {
		return now - lastPlan >= SEARCH_INTERVAL || woken && now - lastPlan >= WAKE_MIN_GAP || now < lastPlan;
	}

	public static boolean canShare(int transferableStacks, int reservers) {
		return transferableStacks > (long) CARRY_SLOTS * reservers;
	}

	/**
	 * Index of the chest to visit. Fullest-first among unclaimed chests with items; only when none is
	 * left, fullest-first among claimed chests with enough items to share. Ties go to the nearer.
	 * -1 when nothing qualifies.
	 */
	public static int chooseSource(List<Candidate> candidates) {
		int free = pickFullest(candidates, true);
		return free >= 0 ? free : pickFullest(candidates, false);
	}

	private static int pickFullest(List<Candidate> candidates, boolean unclaimedOnly) {
		int best = -1;
		for (int i = 0; i < candidates.size(); i++) {
			Candidate c = candidates.get(i);
			if (c.skipped() || c.occupiedSlots() <= 0)
				continue;
			if (unclaimedOnly ? c.reservedByOthers() > 0 : !canShare(c.occupiedSlots(), c.reservedByOthers()))
				continue;
			if (best < 0)
				best = i;
			else {
				Candidate b = candidates.get(best);
				int cmp = Double.compare(fullness(c.occupiedSlots(), c.totalSlots()), fullness(b.occupiedSlots(), b.totalSlots()));
				if (cmp > 0 || cmp == 0 && c.distanceSqr() < b.distanceSqr())
					best = i;
			}
		}
		return best;
	}

	/** Slot indexes taken in one visit: the first non-empty slots in order, up to the free carry slots. */
	public static List<Integer> pickupSlots(List<Boolean> slotOccupied, int carriedStacks) {
		List<Integer> slots = new ArrayList<>();
		int room = Math.max(0, CARRY_SLOTS - carriedStacks);
		for (int i = 0; i < slotOccupied.size() && slots.size() < room; i++)
			if (slotOccupied.get(i))
				slots.add(i);
		return slots;
	}

	/** Scan the entire backing chest, including slots 18-26, for an empty or mergeable slot. */
	public static boolean hasRoomFor(int slotCount, IntPredicate empty, IntPredicate mergeable) {
		for (int slot = 0; slot < slotCount; slot++)
			if (empty.test(slot) || mergeable.test(slot))
				return true;
		return false;
	}

	/** A new trip starts only with empty hands and capacity for at least one source stack. */
	public static boolean shouldCollect(boolean hasTransferableSource, int carriedStacks) {
		return hasTransferableSource && carriedStacks == 0;
	}

	/** A real standing place near the chest, with a clear interaction ray. No roof shortcuts. */
	public static boolean validStandSpot(int dx, int dy, int dz, boolean feetClear,
		boolean headClear, boolean supported, boolean chestVisible) {
		return Math.abs(dx) + Math.abs(dz) == 1 && Math.abs(dy) <= 1
			&& dx * dx + dz * dz + (dy - .5) * (dy - .5) <= 2.5 * 2.5
			&& feetClear && headClear && supported && chestVisible;
	}

	/**
	 * Give up a walk: the full timeout, or at once when the first stand-spot search found no
	 * reachable spot, or after STALL_TICKS without progress. {@code travelTicks} before this tick.
	 */
	public static boolean giveUpWalk(int travelTicks, boolean hasStandSpot, int stallTicks) {
		return travelTicks > GIVE_UP_TICKS || travelTicks > 1 && !hasStandSpot || stallTicks > STALL_TICKS;
	}

	/** Within the network range of the golem's chest (3D). */
	public static boolean inRange(int dx, int dy, int dz) {
		return (long) dx * dx + (long) dy * dy + (long) dz * dz <= (long) MAX_SOURCE_DISTANCE * MAX_SOURCE_DISTANCE;
	}
}
