import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules.Candidate;
import com.simibubi.create.content.equipment.redstoneGolem.SourceReservations;

/**
 * Offline checks for round 12; run with java -ea, compiled with the real RedstoneGolemRules and
 * SourceReservations. Models Minecraft's goal cadence (Mob#serverAiStep: GoalSelector#tick, and so
 * Goal#canUse, only on every second game tick). No Minecraft runtime.
 */
public class RedstoneGolemCoordinationCheck {

	// ---- 1: rows 1-2 full, row 3 empty
	static void thirdRow() {
		int[] item = new int[27];
		for (int s = 0; s < 18; s++) item[s] = s + 1; // 18 distinct full stacks (64)
		assert RedstoneGolemRules.hasRoomFor(27, s -> item[s] == 0, s -> false) : "row 3 empty must allow collecting";
		// a source with 12 new stacks: three 4-stack trips fill row 3 (9 slots), the rest waits
		int sourceStacks = 12, trips = 0;
		while (true) {
			boolean room = RedstoneGolemRules.hasRoomFor(27, s -> item[s] == 0, s -> false);
			int transferable = room ? sourceStacks : 0;
			if (RedstoneGolemRules.chooseSource(List.of(new Candidate(transferable, 27, 1, false))) < 0) break;
			int take = Math.min(4, sourceStacks);
			int placed = 0;
			for (int s = 0; s < 27 && placed < take; s++) if (item[s] == 0) { item[s] = 100 + trips * 4 + placed; placed++; }
			sourceStacks -= placed; // what did not fit stays carried, not lost
			trips++;
			assert trips < 10;
		}
		for (int s = 18; s < 27; s++) assert item[s] != 0 : "third row filled";
		assert trips == 3 && sourceStacks == 3 : "stops only when all 27 are used, 3 stacks left at source";
	}

	// ---- 2: reaction time after the player frees space (game ticks)
	/** Old: cooldown decremented once per canUse call, canUse every 2nd tick, interval 40. */
	static int oldReaction(int phase) {
		int cooldown = 40 - phase; // where in its cycle the golem was when the chest was emptied
		for (int tick = 1; tick < 1000; tick++)
			if (tick % 2 == 0 && cooldown-- <= 0) return tick;
		return -1;
	}

	/** New: game-time scheduling plus an own-chest change wake (WAKE_MIN_GAP). */
	static int newReaction(int sinceLastPlan, boolean ownChestChanged) {
		long lastPlan = -sinceLastPlan;
		for (int tick = 1; tick < 1000; tick++)
			if (tick % 2 == 0 && RedstoneGolemRules.shouldPlan(tick, lastPlan, ownChestChanged)) return tick;
		return -1;
	}

	static int[] reaction() {
		int oldWorst = 0, newWorst = 0, newSourceWorst = 0;
		for (int phase = 0; phase < 40; phase++) oldWorst = Math.max(oldWorst, oldReaction(phase));
		for (int since = 0; since < RedstoneGolemRules.SEARCH_INTERVAL; since++) {
			newWorst = Math.max(newWorst, newReaction(since, true));
			newSourceWorst = Math.max(newSourceWorst, newReaction(since, false));
		}
		assert oldWorst >= 80 : "old call-counted 40 really waited ~80 game ticks: " + oldWorst;
		assert newWorst <= RedstoneGolemRules.WAKE_MIN_GAP + 2 : "own chest change: near-immediate, got " + newWorst;
		assert newSourceWorst <= RedstoneGolemRules.SEARCH_INTERVAL + 2 : "source change: within ~1 s, got " + newSourceWorst;
		assert RedstoneGolemRules.shouldPlan(5, 1_000_000, false) : "clock went backwards (other world): plan";
		return new int[] { oldWorst, newWorst, newSourceWorst };
	}

	// ---- 4: reservations with 2-3 golems
	static int pick(List<int[]> chests, SourceReservations<String> book, UUID golem, long now, Set<Integer> blacklisted) {
		book.release(golem);
		List<Candidate> c = new ArrayList<>();
		for (int i = 0; i < chests.size(); i++)
			c.add(new Candidate(chests.get(i)[0], 27, i, blacklisted.contains(i), book.othersOn("c" + i, golem, now)));
		int best = RedstoneGolemRules.chooseSource(c);
		if (best >= 0) book.claim("c" + best, golem, now);
		return best;
	}

	static void reservations() {
		UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
		SourceReservations<String> book = new SourceReservations<>();
		List<int[]> chests = new ArrayList<>(List.of(new int[] { 20 }, new int[] { 10 }, new int[] { 5 }));
		long now = 100;
		assert pick(chests, book, a, now, Set.of()) == 0 : "first golem: fullest";
		assert pick(chests, book, b, now, Set.of()) == 1 : "second golem: next fullest, not the claimed one";
		assert pick(chests, book, c, now, Set.of()) == 2 : "third golem: the remaining chest";
		// arrival: A takes 4 from chest 0 and releases; B may then pick 0 next if it is still fullest
		chests.get(0)[0] -= 4;
		book.release(a);
		assert book.othersOn("c0", b, now) == 0 : "released on arrival";

		// sharing only when no other chest has items and the chest holds more than one load per golem
		SourceReservations<String> two = new SourceReservations<>();
		List<int[]> one = List.of(new int[] { 9 }, new int[] { 0 });
		assert pick(one, two, a, now, Set.of()) == 0;
		assert pick(one, two, b, now, Set.of()) == 0 : "9 stacks > 4: two golems may share";
		assert pick(one, two, c, now, Set.of()) == 0 : "9 > 8: a third may share too";
		SourceReservations<String> small = new SourceReservations<>();
		List<int[]> four = List.of(new int[] { 4 }, new int[] { 0 });
		assert pick(four, small, a, now, Set.of()) == 0;
		assert pick(four, small, b, now, Set.of()) == -1 : "4 stacks: one golem's load, not shared";
		SourceReservations<String> mixed = new SourceReservations<>();
		List<int[]> withOther = List.of(new int[] { 27 }, new int[] { 1 });
		assert pick(withOther, mixed, a, now, Set.of()) == 0;
		assert pick(withOther, mixed, b, now, Set.of()) == 1 : "another chest has items: never share";
		assert !RedstoneGolemRules.canShare(4, 1) && RedstoneGolemRules.canShare(5, 1) && RedstoneGolemRules.canShare(1, 0);

		// give-up: blacklisted chest released and skipped
		SourceReservations<String> gu = new SourceReservations<>();
		List<int[]> g = List.of(new int[] { 10 }, new int[] { 3 });
		assert pick(g, gu, a, now, Set.of()) == 0;
		gu.release(a);
		assert pick(g, gu, a, now, Set.of(0)) == 1 && gu.othersOn("c0", b, now) == 0;

		// death / unload without release: the claim lapses after RESERVATION_TTL
		SourceReservations<String> dead = new SourceReservations<>();
		List<int[]> d = List.of(new int[] { 4 }, new int[] { 0 });
		assert pick(d, dead, a, now, Set.of()) == 0; // A dies here, never releases
		assert pick(d, dead, b, now + RedstoneGolemRules.RESERVATION_TTL, Set.of()) == -1 : "still claimed within TTL";
		assert pick(d, dead, b, now + RedstoneGolemRules.RESERVATION_TTL + 1, Set.of()) == 0 : "lapsed after TTL";
		// renewal keeps a walking golem's claim alive indefinitely
		SourceReservations<String> walk = new SourceReservations<>();
		for (long t = 0; t < 1000; t++) walk.claim("c0", a, t);
		assert walk.othersOn("c0", b, 999) == 1;
		// a world reload in the same JVM with a smaller clock never leaves a permanent lock
		assert walk.othersOn("c0", b, 3) == 0 : "expiry beyond TTL from 'now' counts as stale";

		// randomized: 3 golems, 6 chests; no two golems on one chest unless sharing is allowed
		java.util.Random r = new java.util.Random(12);
		for (int run = 0; run < 2000; run++) {
			SourceReservations<String> bk = new SourceReservations<>();
			List<int[]> cs = new ArrayList<>();
			for (int i = 0; i < 6; i++) cs.add(new int[] { r.nextInt(3) == 0 ? 0 : r.nextInt(28) });
			int[] target = new int[3];
			UUID[] gs = { a, b, c };
			for (int i = 0; i < 3; i++) target[i] = pick(cs, bk, gs[i], now, Set.of());
			for (int i = 0; i < 3; i++)
				for (int j = i + 1; j < 3; j++)
					if (target[i] >= 0 && target[i] == target[j]) {
						for (int k = 0; k < cs.size(); k++)
							if (k != target[i]) {
								boolean otherUnclaimed = true;
								for (int t : target) if (t == k) otherUnclaimed = false;
								assert cs.get(k)[0] == 0 || !otherUnclaimed : "shared while another chest was free";
							}
						assert cs.get(target[i])[0] > 4 : "shared a chest without enough for two";
					}
		}
	}

	public static void main(String[] args) {
		thirdRow();
		int[] t = reaction();
		reservations();
		System.out.printf("Redstone round-12 checks passed: third row, reaction (old worst %d ticks, new %d after own chest"
			+ " change, %d after a source change), reservations (2-3 golems, release, TTL, 2000 random runs)%n", t[0], t[1], t[2]);
	}
}
