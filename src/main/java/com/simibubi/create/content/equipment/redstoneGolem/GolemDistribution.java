package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.List;

/**
 * Lauro edition: how a Redstone golem shares one kind of maintenance item among the golems of its
 * network. Pure, checked offline. One item at a time goes to the target holding the least of it
 * that still has room (ties: lower index = nearer), so stocks even out and nothing is overfilled.
 */
public final class GolemDistribution {
	private GolemDistribution() {}

	/** {@code count}: what the target's personal slots hold; {@code room}: what they can still take. */
	public record Target(int count, int room) {}

	/** Shares of {@code stock} per target, index-aligned. */
	public static int[] plan(int stock, List<Target> targets) {
		int[] share = new int[targets.size()];
		for (int left = stock; left > 0; left--) {
			int best = -1;
			for (int i = 0; i < targets.size(); i++) {
				Target t = targets.get(i);
				if (share[i] >= t.room())
					continue;
				if (best < 0 || t.count() + share[i] < targets.get(best).count() + share[best])
					best = i;
			}
			if (best < 0)
				break;
			share[best]++;
		}
		return share;
	}

	/** A target needing a dedicated delivery trip: it holds none of an item it uses and can take some. */
	public static boolean urgent(Target t) {
		return t.count() == 0 && t.room() > 0;
	}
}
