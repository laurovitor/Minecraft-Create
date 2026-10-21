package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Offline regression for natural 2x2 dark oak trees; run with java -ea.
 * Compile together with GolemWorkGeometry.java (real source) using javac.
 *
 * Builds natural dark oak shapes with a line-by-line port of vanilla 26.3
 * DarkOakTrunkPlacer + DarkOakFoliagePlacer (lean, hanging side branches, wide canopy,
 * worldgen leaf distances), single and as a dense forest, then runs ports of
 * AndesiteGolemWorkGoal.kindAt/baseCluster/findTree/cutTree and TreeCutter.findTree
 * (regular-tree branch). It proves detection and the cut's block set offline; it does not
 * simulate Minecraft pathfinding, goal scheduling or the client.
 */
public class NaturalDarkOakCheck {
	enum B { AIR, DIRT, GRASS, DARK_OAK_LOG, OAK_LOG, DARK_OAK_LEAVES, LEAF_LITTER }

	static final class World {
		final Map<Long, B> blocks = new HashMap<>();
		final Map<Long, Integer> distance = new HashMap<>();
		B get(int x, int y, int z) { return blocks.getOrDefault(key(x, y, z), y < 64 ? B.DIRT : B.AIR); }
		void set(int x, int y, int z, B b) { blocks.put(key(x, y, z), b); }
		World copy() { World w = new World(); w.blocks.putAll(blocks); w.distance.putAll(distance); return w; }
	}

	static long key(int x, int y, int z) { return ((long) (x + 1_000) << 40) | ((long) (y + 1_000) << 20) | (z + 1_000); }
	static int kx(long k) { return (int) (k >>> 40) - 1_000; }
	static int ky(long k) { return (int) ((k >>> 20) & 0xFFFFF) - 1_000; }
	static int kz(long k) { return (int) (k & 0xFFFFF) - 1_000; }

	static boolean isLog(B b) { return b == B.DARK_OAK_LOG || b == B.OAK_LOG; }
	static boolean isLeaf(B b) { return b == B.DARK_OAK_LEAVES; }
	static boolean airOrLeaves(B b) { return b == B.AIR || isLeaf(b) || b == B.LEAF_LITTER; }
	static boolean collides(B b) { return b != B.AIR && b != B.LEAF_LITTER; }

	// ---- vanilla 26.3 dark oak generation (DarkOakTrunkPlacer(6, 2, 1), DarkOakFoliagePlacer(0, 0))

	record Attachment(int x, int y, int z, boolean doubleTrunk) {}

	/** @return the trunk origin, or null when vanilla would not have room. */
	static int[] growDarkOak(World w, Random random, int x, int y, int z) {
		int treeHeight = 6 + random.nextInt(3) + random.nextInt(2);
		for (int dx = 0; dx <= 1; dx++)
			for (int dz = 0; dz <= 1; dz++)
				if (w.get(x + dx, y - 1, z + dz) == B.GRASS)
					w.set(x + dx, y - 1, z + dz, B.DIRT); // grass onTreeGrow -> dirt
		int[][] lean = { { 0, -1 }, { 1, 0 }, { 0, 1 }, { -1, 0 } };
		int[] dir = lean[random.nextInt(4)];
		int leanHeight = treeHeight - random.nextInt(4);
		int leanSteps = 2 - random.nextInt(3);
		int tx = x, tz = z, ey = y + treeHeight - 1;
		List<Attachment> attachments = new ArrayList<>();
		for (int dy = 0; dy < treeHeight; dy++) {
			if (dy >= leanHeight && leanSteps > 0) {
				tx += dir[0];
				tz += dir[1];
				leanSteps--;
			}
			if (airOrLeaves(w.get(tx, y + dy, tz)))
				for (int[] o : new int[][] { { 0, 0 }, { 1, 0 }, { 0, 1 }, { 1, 1 } })
					placeLog(w, tx + o[0], y + dy, tz + o[1]);
		}
		attachments.add(new Attachment(tx, ey, tz, true));
		for (int ox = -1; ox <= 2; ox++)
			for (int oz = -1; oz <= 2; oz++)
				if ((ox < 0 || ox > 1 || oz < 0 || oz > 1) && random.nextInt(3) <= 0) {
					int length = random.nextInt(3) + 2;
					for (int by = 0; by < length; by++)
						placeLog(w, x + ox, ey - by - 1, z + oz);
					attachments.add(new Attachment(x + ox, ey, z + oz, false));
				}
		for (Attachment a : attachments) {
			if (a.doubleTrunk) {
				leavesRow(w, random, a, 2, -1);
				leavesRow(w, random, a, 3, 0);
				leavesRow(w, random, a, 2, 1);
				if (random.nextBoolean())
					leavesRow(w, random, a, 0, 2);
			} else {
				leavesRow(w, random, a, 2, -1);
				leavesRow(w, random, a, 1, 0);
			}
		}
		return new int[] { x, y, z };
	}

	static void placeLog(World w, int x, int y, int z) {
		if (airOrLeaves(w.get(x, y, z)) || w.get(x, y, z) == B.GRASS)
			w.set(x, y, z, B.DARK_OAK_LOG);
	}

	static void leavesRow(World w, Random random, Attachment a, int radius, int y) {
		int t = a.doubleTrunk ? 1 : 0;
		for (int dx = -radius; dx <= radius + t; dx++)
			for (int dz = -radius; dz <= radius + t; dz++) {
				if (skipSigned(dx, y, dz, radius, a.doubleTrunk))
					continue;
				int px = a.x + dx, py = a.y + y, pz = a.z + dz;
				if (airOrLeaves(w.get(px, py, pz)) && !isLeaf(w.get(px, py, pz)))
					w.set(px, py, pz, B.DARK_OAK_LEAVES);
			}
	}

	static boolean skipSigned(int dx, int y, int dz, int r, boolean doubleTrunk) {
		if (y == 0 && doubleTrunk && (dx == -r || dx >= r) && (dz == -r || dz >= r))
			return true;
		int ax = doubleTrunk ? Math.min(Math.abs(dx), Math.abs(dx - 1)) : Math.abs(dx);
		int az = doubleTrunk ? Math.min(Math.abs(dz), Math.abs(dz - 1)) : Math.abs(dz);
		if (y == -1 && !doubleTrunk)
			return ax == r && az == r;
		return y == 1 && ax + az > r * 2 - 2;
	}

	/** Worldgen leaf distance: 6-connected steps from the nearest log, capped at 7. */
	static void updateLeafDistances(World w) {
		LinkedList<Long> frontier = new LinkedList<>();
		w.distance.clear();
		for (var e : w.blocks.entrySet())
			if (isLeaf(e.getValue()))
				w.distance.put(e.getKey(), 7);
		for (var e : w.blocks.entrySet())
			if (isLog(e.getValue()))
				frontier.add(e.getKey());
		Map<Long, Integer> d = new HashMap<>();
		for (long k : frontier)
			d.put(k, 0);
		while (!frontier.isEmpty()) {
			long k = frontier.removeFirst();
			int dist = d.get(k);
			if (dist >= 6)
				continue;
			for (int[] o : new int[][] { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } }) {
				long n = key(kx(k) + o[0], ky(k) + o[1], kz(k) + o[2]);
				if (!isLeaf(w.blocks.getOrDefault(n, B.AIR)) || d.containsKey(n))
					continue;
				d.put(n, dist + 1);
				w.distance.put(n, dist + 1);
				frontier.add(n);
			}
		}
	}

	static World flatGround(int r) {
		World w = new World();
		for (int x = -r; x <= r; x++)
			for (int z = -r; z <= r; z++)
				w.set(x, 63, z, B.GRASS);
		return w;
	}

	// ---- ports of the golem/cutter code under test

	interface Reader { B get(int x, int y, int z); default int dist(int x, int y, int z) { return 7; } }

	static Reader reader(World w) {
		return new Reader() {
			public B get(int x, int y, int z) { return w.get(x, y, z); }
			public int dist(int x, int y, int z) { return w.distance.getOrDefault(key(x, y, z), 7); }
		};
	}

	record Tree(List<Long> logs, List<Long> leaves) {}

	/** TreeCutter.findTree regular-tree branch (no roots/attachments in this terrain). */
	static Tree findTreeForGolem(Reader r, int px, int py, int pz) {
		List<Long> logs = new ArrayList<>(), leaves = new ArrayList<>();
		Set<Long> visited = new HashSet<>();
		LinkedList<Long> frontier = new LinkedList<>();
		visited.add(key(px, py, pz));
		for (int x = px - 1; x <= px + 1; x++)
			for (int y = py; y <= py + 1; y++)
				for (int z = pz - 1; z <= pz + 1; z++)
					frontier.add(key(x, y, z));
		while (!frontier.isEmpty()) {
			long c = frontier.removeFirst();
			if (!visited.add(c))
				continue;
			if (!isLog(r.get(kx(c), ky(c), kz(c))))
				continue;
			logs.add(c);
			neighbours(c, 0, 1, visited, frontier::add);
		}
		visited.clear();
		visited.addAll(logs);
		frontier.addAll(logs);
		while (!frontier.isEmpty()) {
			long prev = frontier.removeFirst();
			B prevState = r.get(kx(prev), ky(prev), kz(prev));
			int prevDist = isLeaf(prevState) ? r.dist(kx(prev), ky(prev), kz(prev)) : 0;
			List<Long> next = new ArrayList<>();
			neighbours(prev, -1, 1, visited, next::add);
			for (long c : next) {
				B state = r.get(kx(c), ky(c), kz(c));
				if (isLeaf(state) && r.dist(kx(c), ky(c), kz(c)) > prevDist && visited.add(c)) {
					leaves.add(c);
					frontier.add(c);
				}
			}
		}
		return new Tree(logs, leaves);
	}

	static void neighbours(long c, int minY, int maxY, Set<Long> visited, java.util.function.Consumer<Long> out) {
		for (int x = -1; x <= 1; x++)
			for (int y = minY; y <= maxY; y++)
				for (int z = -1; z <= 1; z++) {
					long n = key(kx(c) + x, ky(c) + y, kz(c) + z);
					if (!visited.contains(n))
						out.accept(n);
				}
	}

	static String path(B b) { return b == B.DARK_OAK_LOG ? "dark_oak_log" : b == B.OAK_LOG ? "oak_log" : "other"; }

	/** AndesiteGolemWorkGoal.baseCluster, using the real GolemWorkGeometry species gate. */
	static List<Long> baseCluster(World w, int bx, int by, int bz) {
		B log = w.get(bx, by, bz);
		if (!GolemWorkGeometry.canHaveWideTrunk(path(log)))
			return List.of(key(bx, by, bz));
		for (int x = -1; x <= 0; x++)
			for (int z = -1; z <= 0; z++) {
				List<Long> square = List.of(key(bx + x, by, bz + z), key(bx + x + 1, by, bz + z),
					key(bx + x, by, bz + z + 1), key(bx + x + 1, by, bz + z + 1));
				if (square.stream().allMatch(p -> w.get(kx(p), ky(p), kz(p)) == log
					&& !isLog(w.get(kx(p), ky(p) - 1, kz(p)))))
					return square;
			}
		return List.of(key(bx, by, bz));
	}

	/** kindAt for logs: lowest log on soil, and the chest filter accepts that log. */
	static boolean treeCandidate(World w, int x, int y, int z, Predicate<B> filter) {
		B state = w.get(x, y, z);
		if (!isLog(state))
			return false;
		B below = w.get(x, y - 1, z);
		if (isLog(below) || !(below == B.DIRT || below == B.GRASS))
			return false;
		return filter.test(state);
	}

	/** Fixed findTree: every 2x2 species masks the other three base logs. */
	static Tree findTreeFixed(World w, int bx, int by, int bz, List<Long> cluster) {
		long base = key(bx, by, bz);
		Reader masked = new Reader() {
			public B get(int x, int y, int z) {
				long k = key(x, y, z);
				return k != base && cluster.contains(k) ? B.AIR : w.get(x, y, z);
			}
			public int dist(int x, int y, int z) { return w.distance.getOrDefault(key(x, y, z), 7); }
		};
		return findTreeForGolem(masked, bx, by, bz);
	}

	/** Commit 3bc20b8bf: dark oak 2x2 searched unmasked and its footprint loop skipped. */
	static Set<Long> cutBefore(World w, int bx, int by, int bz) {
		List<Long> fp = baseCluster(w, bx, by, bz);
		boolean darkWide = fp.size() == 4 && w.get(bx, by, bz) == B.DARK_OAK_LOG;
		Tree t = darkWide ? findTreeForGolem(reader(w), bx, by, bz) : findTreeFixed(w, bx, by, bz, fp);
		Set<Long> broken = new HashSet<>();
		if (t.leaves.isEmpty())
			return broken;
		if (!darkWide)
			broken.addAll(fp);
		broken.addAll(t.logs);
		broken.addAll(t.leaves);
		return broken;
	}

	static Set<Long> cutFixed(World w, int bx, int by, int bz) {
		List<Long> fp = baseCluster(w, bx, by, bz);
		Tree t = findTreeFixed(w, bx, by, bz, fp);
		Set<Long> broken = new HashSet<>();
		if (t.leaves.isEmpty())
			return broken;
		broken.addAll(fp);
		broken.addAll(t.logs);
		broken.addAll(t.leaves);
		return broken;
	}

	static boolean canStandAt(World w, int x, int y, int z) {
		return !collides(w.get(x, y, z)) && !collides(w.get(x, y + 1, z))
			&& (w.get(x, y - 1, z) == B.DIRT || w.get(x, y - 1, z) == B.GRASS || isLog(w.get(x, y - 1, z)));
	}

	/** walkToTree's candidate standing spots (1-3 blocks out from each trunk face). */
	static int approachSpots(World w, List<Long> fp) {
		Set<Long> spots = new HashSet<>();
		int[][] sides = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
		for (long t : fp)
			for (int[] s : sides)
				for (int out = 1; out <= 3; out++)
					for (int dy = 1; dy >= -7; dy--) {
						int x = kx(t) + s[0] * out, y = ky(t) + dy, z = kz(t) + s[1] * out;
						if (canStandAt(w, x, y, z))
							spots.add(key(x, y, z));
					}
		return spots.size();
	}

	/** Trunk-column logs (the 2x2 at every height up to the lean) that a cut must remove. */
	static Set<Long> trunk(World w, int x, int y, int z) {
		Set<Long> out = new HashSet<>();
		for (int dy = 0; dy < 4; dy++)
			for (int dx = 0; dx <= 1; dx++)
				for (int dz = 0; dz <= 1; dz++)
					if (isLog(w.get(x + dx, y + dy, z + dz)))
						out.add(key(x + dx, y + dy, z + dz));
		return out;
	}

	public static void main(String[] args) {
		Predicate<B> anything = b -> true;
		Predicate<B> oakOnly = b -> b == B.OAK_LOG;
		int trees = 0, leftoverBefore = 0, leftoverFixed = 0, orphanLogs = 0;
		for (int seed = 0; seed < 2000; seed++) {
			World w = flatGround(12);
			int[] o = growDarkOak(w, new Random(seed), 0, 64, 0);
			updateLeafDistances(w);
			trees++;
			// every base log is a target candidate, and its 2x2 footprint is complete
			for (int dx = 0; dx <= 1; dx++)
				for (int dz = 0; dz <= 1; dz++) {
					assert treeCandidate(w, dx, 64, dz, anything) : "seed " + seed + " base not a candidate";
					assert !treeCandidate(w, dx, 64, dz, oakOnly) : "oak-log filter must reject dark oak";
					List<Long> fp = baseCluster(w, dx, 64, dz);
					assert fp.size() == 4 : "seed " + seed + " natural trunk is not 2x2";
					Tree masked = findTreeFixed(w, dx, 64, dz, fp);
					Tree unmasked = findTreeForGolem(reader(w), dx, 64, dz);
					assert !masked.leaves.isEmpty() : "seed " + seed + " masked search found no leaves";
					assert !unmasked.leaves.isEmpty() : "seed " + seed + " unmasked search found no leaves";
					assert approachSpots(w, fp) > 0 : "seed " + seed + " no standing spot";
				}
			Set<Long> trunk = trunk(w, o[0], o[1], o[2]);
			Set<Long> before = cutBefore(w, 0, 64, 0);
			Set<Long> fixed = cutFixed(w, 0, 64, 0);
			for (long t : trunk) {
				if (!before.contains(t)) leftoverBefore++;
				if (!fixed.contains(t)) leftoverFixed++;
			}
			assert before.size() > 0 && !before.contains(key(0, 64, 0))
				: "3bc20b8bf: target base log must be missing from the old cut (seed " + seed + ")";
			assert fixed.containsAll(trunk) : "seed " + seed + " fixed cut leaves trunk logs";
			for (var e : w.blocks.entrySet())
				if (isLog(e.getValue()) && !fixed.contains(e.getKey()))
					orphanLogs++;
		}
		assert leftoverFixed == 0 && orphanLogs == 0 : "fixed cut left logs: trunk " + leftoverFixed + " other " + orphanLogs;

		// a dense natural forest: neighbouring canopies merge, nothing should block detection
		Random forest = new Random(42);
		World w = flatGround(40);
		List<int[]> origins = new ArrayList<>();
		for (int gx = -30; gx <= 30; gx += 6)
			for (int gz = -30; gz <= 30; gz += 6) {
				int x = gx + forest.nextInt(2), z = gz + forest.nextInt(2);
				if (w.get(x, 64, z) == B.AIR && w.get(x + 1, 64, z + 1) == B.AIR)
					origins.add(growDarkOak(w, forest, x, 64, z));
			}
		updateLeafDistances(w);
		for (int[] o : origins) {
			List<Long> fp = baseCluster(w, o[0], o[1], o[2]);
			assert fp.size() == 4;
			assert !findTreeFixed(w, o[0], o[1], o[2], fp).leaves.isEmpty() : "forest tree undetected";
			assert approachSpots(w, fp) > 0 : "forest tree without standing spot";
			assert cutFixed(w, o[0], o[1], o[2]).containsAll(trunk(w, o[0], o[1], o[2]));
		}
		System.out.printf("Natural dark oak checks passed: %d single trees x4 bases, %d forest trees;"
			+ " 3bc20b8bf left %d trunk logs standing (always incl. the target base), fixed cut left %d%n",
			trees, origins.size(), leftoverBefore, leftoverFixed);
	}
}
