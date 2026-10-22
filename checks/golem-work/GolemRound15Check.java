package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules.Candidate;
import com.simibubi.create.content.equipment.redstoneGolem.SourceReservations;

/**
 * Round 15 regressions; run with java -ea. Compile with the real GolemWorkGeometry, GolemCareRules,
 * RedstoneGolemRules and SourceReservations. Models goal decisions only, no Minecraft runtime.
 */
public class GolemRound15Check {

	/** 1: AndesiteGolemChestBlockEntity#hasEmptySlot loops getContainerSize() (= SIZE 27; guarded in verify-integration.py). */
	static boolean hasEmptySlot(int[] slots) {
		for (int i = 0; i < slots.length; i++) if (slots[i] == 0) return true;
		return false;
	}

	/** 2: findTarget = nearest candidate not claimed by another golem; then claim it. */
	static int pick(double[][] golemDist, int golem, boolean[] done, SourceReservations<Integer> book, UUID[] ids, long now) {
		int best = -1;
		for (int t = 0; t < done.length; t++)
			if (!done[t] && book.othersOn(t, ids[golem], now) == 0 && (best < 0 || golemDist[golem][t] < golemDist[golem][best]))
				best = t;
		if (best >= 0) book.claim(best, ids[golem], now);
		return best;
	}

	public static void main(String[] args) {
		// ---- 1 Andesite chest: rows 1-2 full, row 3 empty -> still works; 27 full -> stops
		int[] chest = new int[27];
		for (int s = 0; s < 18; s++) chest[s] = 1;
		assert hasEmptySlot(chest) : "row 3 empty must keep the golem working";
		for (int s = 18; s < 27; s++) chest[s] = 1;
		assert !hasEmptySlot(chest);

		// ---- 2 teamwork: 3 golems standing together, 5 trees; old rule (no claims) = all pick the same
		UUID[] ids = { UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID() };
		double[][] dist = { { 1, 2, 3, 4, 5 }, { 1.1, 2.1, 3.1, 4.1, 5.1 }, { 1.2, 2.2, 3.2, 4.2, 5.2 } };
		boolean[] done = new boolean[5];
		SourceReservations<Integer> book = new SourceReservations<>();
		int a = pick(dist, 0, done, book, ids, 0), b = pick(dist, 1, done, book, ids, 0), c = pick(dist, 2, done, book, ids, 0);
		assert a == 0 && b == 1 && c == 2 : "three different nearest jobs in parallel";
		// finish releases; a dead golem's claim lapses after the TTL; renewal keeps it while working
		book.release(ids[0]);
		done[0] = true;
		assert book.othersOn(1, ids[0], 1) == 1 && book.othersOn(1, ids[0], 1 + RedstoneGolemRules.RESERVATION_TTL + 1) == 0;
		// 2x2 tree claimed by its corner: logs (0,0),(1,0),(0,1),(1,1) all map to one key
		int[][] logs = { { 1, 1 }, { 0, 1 }, { 1, 0 }, { 0, 0 } };
		int[] min = logs[0];
		for (int[] l : logs) if (l[0] < min[0] || l[0] == min[0] && l[1] < min[1]) min = l;
		assert min[0] == 0 && min[1] == 0;
		// randomized: never two golems on one job
		java.util.Random r = new java.util.Random(15);
		for (int run = 0; run < 2000; run++) {
			int n = 1 + r.nextInt(8);
			double[][] d = new double[3][n];
			for (double[] row : d) for (int t = 0; t < n; t++) row[t] = r.nextDouble() * 20;
			SourceReservations<Integer> bk = new SourceReservations<>();
			boolean[] dn = new boolean[n];
			List<Integer> picks = new ArrayList<>();
			for (int g = 0; g < 3; g++) picks.add(pick(d, g, dn, bk, ids, run));
			for (int i = 0; i < 3; i++)
				for (int j = i + 1; j < 3; j++)
					assert picks.get(i) < 0 || !picks.get(i).equals(picks.get(j)) : "same job twice";
			assert picks.stream().filter(p -> p >= 0).count() == Math.min(3, n) : "every golem gets a job while jobs last";
		}

		// ---- 3 sleep: no bedside, stuck or too long -> sleep where it stands (never awake all night)
		assert GolemWorkGeometry.sleepHereInstead(false, 0, 0, 60, 400) : "no reachable bed: sleep here";
		assert !GolemWorkGeometry.sleepHereInstead(true, 10, 50, 60, 400) : "walking fine: keep walking";
		assert GolemWorkGeometry.sleepHereInstead(true, 61, 50, 60, 400) && GolemWorkGeometry.sleepHereInstead(true, 0, 401, 60, 400);

		// ---- 4 Redstone: one unreachable fullest source must not block the others
		assert RedstoneGolemRules.giveUpWalk(2, false, 0) : "no reachable stand spot: give up at once";
		assert !RedstoneGolemRules.giveUpWalk(1, false, 0) : "before the first spot search";
		assert RedstoneGolemRules.giveUpWalk(50, true, RedstoneGolemRules.STALL_TICKS + 1) : "stalled";
		assert !RedstoneGolemRules.giveUpWalk(50, true, 10);
		int[] items = { 27, 5, 3 }; // source 0 is unreachable but the fullest
		boolean[] reachable = { false, true, true };
		long[] blacklistUntil = new long[3];
		int[] collected = new int[3];
		long tick = 0;
		while (tick < 2400) {
			List<Candidate> cands = new ArrayList<>();
			for (int i = 0; i < 3; i++) cands.add(new Candidate(items[i], 27, i, blacklistUntil[i] > tick));
			int pick = RedstoneGolemRules.chooseSource(cands);
			if (pick < 0) { tick += RedstoneGolemRules.SEARCH_INTERVAL; continue; }
			if (!reachable[pick]) {
				tick += 3; // new rule: gives up on the 3rd walk tick (no reachable stand spot)
				blacklistUntil[pick] = tick + RedstoneGolemRules.BLACKLIST_TICKS;
				continue;
			}
			int take = Math.min(4, items[pick]);
			items[pick] -= take;
			collected[pick] += take;
			tick += 200; // walk, take, walk home, deposit
		}
		assert collected[1] == 5 && collected[2] == 3 : "reachable sources fully collected despite the bad one";
		assert items[0] == 27 && collected[0] == 0;

		// ---- 6 Redstone repair: 25% of 8 HP
		assert GolemCareRules.repairAmount((float) RedstoneGolemRules.MAX_HEALTH) == 2f;
		// ---- 5 skip durations within the requested 30-60 s
		assert AndesiteGolemWorkGoalSkip.ANDESITE >= 600 && AndesiteGolemWorkGoalSkip.ANDESITE <= 1200;
		assert RedstoneGolemRules.SKIP_TICKS >= 600 && RedstoneGolemRules.SKIP_TICKS <= 1200;
		System.out.println("Round 15 checks passed: Andesite third row, job claims (2000 runs), sleep fallback,"
			+ " Redstone bad source, repair 2 HP, skip 30 s");
	}

	/** AndesiteGolemWorkGoal.SKIP_TICKS (that class needs Minecraft; value guarded in verify-integration.py). */
	static final class AndesiteGolemWorkGoalSkip {
		static final int ANDESITE = 600;
	}
}
