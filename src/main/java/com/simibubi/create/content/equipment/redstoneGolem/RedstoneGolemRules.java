package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.ArrayList;
import java.util.List;

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
	 * Network chests farther than this from the golem's chest are ignored, blocks (3D). Matches the
	 * 48-block navigation path length the golems use; farther targets could never be pathed to.
	 */
	public static final int MAX_SOURCE_DISTANCE = 48;
	/** How often an idle golem looks at its network again, ticks (2 s). */
	public static final int SEARCH_INTERVAL = 40;
	/** Gives up walking to a source after this long, ticks (60 s), and skips it for BLACKLIST_TICKS. */
	public static final int GIVE_UP_TICKS = 1200;
	public static final int BLACKLIST_TICKS = 1200;

	/**
	 * Fullness of a chest: occupied slots / total slots, 0..1. Slots, not item counts: the golem
	 * empties slots, so the chest with the most occupied slots gives the most per trip.
	 */
	public static double fullness(int occupiedSlots, int totalSlots) {
		return totalSlots <= 0 ? 0 : (double) Math.max(0, occupiedSlots) / totalSlots;
	}

	/** One network chest as seen by the golem. */
	public record Candidate(int occupiedSlots, int totalSlots, double distanceSqr, boolean skipped) {}

	/**
	 * Index of the chest to visit: the fullest non-empty, not-skipped one; ties go to the nearer.
	 * -1 when every chest is empty or skipped.
	 */
	public static int chooseSource(List<Candidate> candidates) {
		int best = -1;
		for (int i = 0; i < candidates.size(); i++) {
			Candidate c = candidates.get(i);
			if (c.skipped() || c.occupiedSlots() <= 0)
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

	/** Within the network range of the golem's chest (3D). */
	public static boolean inRange(int dx, int dy, int dz) {
		return (long) dx * dx + (long) dy * dy + (long) dz * dz <= (long) MAX_SOURCE_DISTANCE * MAX_SOURCE_DISTANCE;
	}
}
