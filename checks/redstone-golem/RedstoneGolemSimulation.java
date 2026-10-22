import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules.Candidate;

/**
 * Offline simulation of the Redstone Golem's collecting; run with java -ea.
 * Compile with the real RedstoneGolemRules. Network membership mirrors RedstoneGolemNetwork#sources
 * (guarded against drift by checks/netherite-golem/verify-integration.py): Stock Links on the
 * home frequency, attached chest not the home, in range, chest or golem chest, a double chest once.
 * Does not simulate pathfinding, chunk loading or Minecraft containers.
 */
public class RedstoneGolemSimulation {
	enum Kind { CHEST, GOLEM_CHEST, FURNACE }

	/** A container: slots hold item ids (0 = empty), counts kept separately for conservation. */
	static final class Inv {
		final int[] id, count;
		Inv(int size) { id = new int[size]; count = new int[size]; }
		int occupied() { int n = 0; for (int i : id) if (i != 0) n++; return n; }
		boolean hasEmpty() { for (int i : id) if (i == 0) return true; return false; }
		long total() { long t = 0; for (int c : count) t += c; return t; }
		List<Boolean> occupiedSlots() { List<Boolean> l = new ArrayList<>(); for (int i : id) l.add(i != 0); return l; }
	}

	/** A placed Stock Link: frequency, attached block, and (for double chests) the other half. */
	record Link(UUID freq, int x, int y, int z, Kind kind, Inv inv, String pairKey) {}

	record Source(Link link, Inv inv) {}

	static List<Source> sources(UUID homeFreq, int hx, int hy, int hz, List<Link> links) {
		List<Source> out = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		seen.add(hx + "," + hy + "," + hz);
		for (Link l : links) {
			if (!l.freq().equals(homeFreq))
				continue;
			if (!RedstoneGolemRules.inRange(l.x() - hx, l.y() - hy, l.z() - hz))
				continue;
			if (!seen.add(l.x() + "," + l.y() + "," + l.z()))
				continue;
			if (l.kind() == Kind.FURNACE)
				continue;
			if (l.pairKey() != null && !seen.add(l.pairKey()))
				continue;
			out.add(new Source(l, l.inv()));
		}
		return out;
	}

	static void fill(Inv inv, Random r, int slots) {
		for (int s = 0; s < slots; s++) {
			inv.id[s] = 1 + r.nextInt(50);
			inv.count[s] = 1 + r.nextInt(64);
		}
	}

	/** Exact post-deposit decisions for two trips, a full home, and an emptied source. */
	static void lifecycleCases() {
		Inv home = new Inv(3), source = new Inv(9);
		for (int i = 0; i < 6; i++) { source.id[i] = i + 1; source.count[i] = 1; }
		int completed = 0;
		while (home.hasEmpty() && source.occupied() > 0) {
			List<Integer> pickup = RedstoneGolemRules.pickupSlots(source.occupiedSlots(), 0);
			for (int slot : pickup) {
				for (int dest = 0; dest < home.id.length; dest++) {
					if (home.id[dest] != 0) continue;
					home.id[dest] = source.id[slot]; home.count[dest] = source.count[slot];
					source.id[slot] = source.count[slot] = 0;
					break;
				}
			}
			completed++;
		}
		assert completed == 1 && source.occupied() == 3 && !home.hasEmpty() : "full destination blocks next trip";
		for (int i = 0; i < home.id.length; i++) home.id[i] = home.count[i] = 0;
		while (source.occupied() > 0) {
			List<Integer> pickup = RedstoneGolemRules.pickupSlots(source.occupiedSlots(), 0);
			for (int slot : pickup) {
				int dest = slot % home.id.length;
				home.id[dest] = source.id[slot]; home.count[dest] = source.count[slot];
				source.id[slot] = source.count[slot] = 0;
			}
			completed++;
		}
		assert completed == 2 && source.occupied() == 0 : "second trip drains source, then stops";
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(0, 9, 1, false))) == -1;
	}

	public static void main(String[] args) {
		lifecycleCases();
		// --- rules: fullest first, ties nearer, empty and skipped ignored
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(3, 27, 4, false), new Candidate(10, 27, 400, false),
			new Candidate(0, 27, 1, false))) == 1 : "fullest wins even when farther";
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(5, 27, 100, false), new Candidate(5, 27, 9, false))) == 1
			: "tie goes to the nearer";
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(27, 27, 4, true), new Candidate(1, 27, 4, false))) == 1
			: "blacklisted (unreachable) skipped";
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(0, 27, 1, false))) == -1 : "all empty: idle";
		assert RedstoneGolemRules.chooseSource(List.of(new Candidate(5, 9, 1, false), new Candidate(10, 54, 1, false))) == 0
			: "fullness is a fraction: 5/9 beats 10/54";
		// --- 4-slot pickup
		assert RedstoneGolemRules.pickupSlots(List.of(false, true, false, true, true, true, true), 0).equals(List.of(1, 3, 4, 5));
		assert RedstoneGolemRules.pickupSlots(List.of(true, true, true), 0).equals(List.of(0, 1, 2));
		assert RedstoneGolemRules.pickupSlots(List.of(true, true, true), 3).equals(List.of(0));
		assert RedstoneGolemRules.pickupSlots(List.of(true, true), 4).isEmpty();
		// --- stop when full
		assert !RedstoneGolemRules.shouldCollect(false, 0) && !RedstoneGolemRules.shouldCollect(true, 2) && RedstoneGolemRules.shouldCollect(true, 0);
		// --- tuning constants
		assert RedstoneGolemRules.MAX_HEALTH < 12 : "most fragile (Andesite 12, Netherite 80)";
		assert RedstoneGolemRules.BASE_SPEED > 0.22 && RedstoneGolemRules.BASE_SPEED * RedstoneGolemRules.RUN_SPEED > RedstoneGolemRules.BASE_SPEED;

		// --- network membership
		UUID home = UUID.randomUUID(), other = UUID.randomUUID();
		Inv a = new Inv(27), b = new Inv(27), dbl = new Inv(54), golemBox = new Inv(27), otherNet = new Inv(27), furnace = new Inv(3), far = new Inv(27), own = new Inv(27);
		List<Link> links = List.of(
			new Link(home, 5, 64, 0, Kind.CHEST, a, null),
			new Link(home, -5, 64, 0, Kind.CHEST, b, null),
			new Link(home, 0, 64, 9, Kind.CHEST, dbl, "1,64,9"), new Link(home, 1, 64, 9, Kind.CHEST, dbl, "0,64,9"),
			new Link(home, 0, 64, -7, Kind.GOLEM_CHEST, golemBox, null),
			new Link(other, 3, 64, 3, Kind.CHEST, otherNet, null),
			new Link(home, 2, 64, 2, Kind.FURNACE, furnace, null),
			new Link(home, 129, 64, 0, Kind.CHEST, far, null),
			new Link(home, 0, 64, 0, Kind.GOLEM_CHEST, own, null));
		List<Source> net = sources(home, 0, 64, 0, links);
		assert net.size() == 4 : "two chests, one double chest once, one golem chest; got " + net.size();
		assert net.stream().noneMatch(s -> s.inv() == otherNet) : "other frequency excluded";
		assert net.stream().noneMatch(s -> s.inv() == own) : "own chest never a source";
		assert net.stream().noneMatch(s -> s.inv() == far || s.inv() == furnace) : "out of range / not a chest";

		// --- full runs: always fullest, 4 stacks per trip, items conserved, stops when home full
		Random r = new Random(7);
		for (int run = 0; run < 500; run++) {
			Inv[] chests = { new Inv(27), new Inv(27), new Inv(54), new Inv(27) };
			for (Inv c : chests)
				fill(c, r, r.nextInt(c.id.length + 1));
			Inv homeChest = new Inv(27);
			fill(homeChest, r, r.nextInt(10));
			long before = homeChest.total();
			for (Inv c : chests) before += c.total();
			int trips = 0;
			while (true) {
				if (!RedstoneGolemRules.shouldCollect(homeChest.hasEmpty(), 0))
					break;
				List<Candidate> cands = new ArrayList<>();
				for (int i = 0; i < chests.length; i++)
					cands.add(new Candidate(chests[i].occupied(), chests[i].id.length, i * 10.0, false));
				int pick = RedstoneGolemRules.chooseSource(cands);
				if (pick < 0)
					break;
				double best = 0;
				for (Inv c : chests)
					best = Math.max(best, RedstoneGolemRules.fullness(c.occupied(), c.id.length));
				assert RedstoneGolemRules.fullness(chests[pick].occupied(), chests[pick].id.length) == best : "not the fullest";
				List<Integer> slots = RedstoneGolemRules.pickupSlots(chests[pick].occupiedSlots(), 0);
				assert slots.size() == Math.min(4, chests[pick].occupied()) : "4 stacks per trip";
				List<int[]> carried = new ArrayList<>();
				for (int s : slots) {
					carried.add(new int[] { chests[pick].id[s], chests[pick].count[s] });
					chests[pick].id[s] = 0;
					chests[pick].count[s] = 0;
				}
				// deposit into empty slots (each stack its own slot); leftovers stay carried
				for (int[] st : carried)
					for (int i = 0; i < homeChest.id.length; i++)
						if (homeChest.id[i] == 0) {
							homeChest.id[i] = st[0];
							homeChest.count[i] = st[1];
							st[1] = 0;
							break;
						}
				long left = carried.stream().mapToLong(st -> st[1]).sum();
				assert left == 0 || !homeChest.hasEmpty() : "leftovers only when the home chest is full";
				if (left > 0)
					before -= left; // still in the golem's hands, not lost
				trips++;
				assert trips < 1000;
			}
			long after = homeChest.total();
			for (Inv c : chests) after += c.total();
			assert after == before : "items conserved";
			boolean anySource = false;
			for (Inv c : chests) anySource |= c.occupied() > 0;
			assert !homeChest.hasEmpty() || !anySource : "stops only when full or nothing left";
		}
		System.out.println("Redstone golem simulation passed: rules, network membership, 500 random runs");
	}
}
