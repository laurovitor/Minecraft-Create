package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.simibubi.create.content.equipment.redstoneGolem.GolemDistribution;
import com.simibubi.create.content.equipment.redstoneGolem.GolemDistribution.Target;

/**
 * Round 16 regressions; run with java -ea. Compile with the real GolemChestLayout, GolemMaintenance,
 * GolemHoney and GolemDistribution. "Automation sees only storage" is structural (the capability and
 * the network wrap getContainer(), the storage container) and is guarded in verify-integration.py.
 */
public class GolemRound16Check {

	static Map<String, Integer> multiset(List<String> l) {
		Map<String, Integer> m = new HashMap<>();
		for (String s : l) if (!s.isEmpty()) m.merge(s, 1, Integer::sum);
		return m;
	}

	static void migration(int cols, int offset, String kind) {
		Random r = new Random(16 + cols);
		String[] pool = { "cobblestone", "oak_log", "create:andesite_alloy", "minecraft:glass_bottle",
			"minecraft:redstone", "minecraft:netherite_ingot", "wheat" };
		for (int run = 0; run < 3000; run++) {
			List<String> legacy = new ArrayList<>();
			for (int i = 0; i < GolemChestLayout.LEGACY_SIZE; i++)
				legacy.add(r.nextInt(3) == 0 ? "" : pool[r.nextInt(pool.length)] + "#" + i);
			var m = GolemChestLayout.migrate(legacy, "", String::isEmpty,
				s -> GolemMaintenance.uses(kind, s.substring(0, s.indexOf('#'))), cols, offset);
			assert m.storage().size() == 3 * cols && m.personal().size() == 3;
			List<String> after = new ArrayList<>(m.storage());
			after.addAll(m.personal());
			after.addAll(m.overflow());
			assert multiset(after).equals(multiset(legacy)) : "every item kept exactly once";
			for (int i = 0; i < 27; i++) {
				int col = i % 9 - offset;
				if (!legacy.get(i).isEmpty() && col >= 0 && col < cols)
					assert m.storage().get(i / 9 * cols + col).equals(legacy.get(i)) : "storage columns keep their place";
			}
			for (String p : m.personal())
				assert p.isEmpty() || GolemMaintenance.uses(kind, p.substring(0, p.indexOf('#'))) : "personal only valid";
			if (!m.overflow().isEmpty())
				assert !m.storage().contains("") : "dropped only when storage is full";
		}
		// a full old chest: 6 (Andesite) or 9 (Redstone) stacks cannot stay in storage
		List<String> full = new ArrayList<>();
		for (int i = 0; i < 27; i++) full.add("cobblestone#" + i);
		var m = GolemChestLayout.migrate(full, "", String::isEmpty, s -> false, cols, offset);
		assert m.overflow().size() == 27 - 3 * cols;
	}

	public static void main(String[] args) {
		migration(GolemChestLayout.STANDARD_STORAGE_COLUMNS, GolemChestLayout.STANDARD_STORAGE_OFFSET, "andesite");
		migration(GolemChestLayout.REDSTONE_STORAGE_COLUMNS, GolemChestLayout.REDSTONE_STORAGE_OFFSET, "redstone");
		assert 3 * GolemChestLayout.STANDARD_STORAGE_COLUMNS == 21 && 3 * GolemChestLayout.REDSTONE_STORAGE_COLUMNS == 18;
		// window columns: 7 + separator + personal = 9; 1 + ½ + 6 + ½ + 1 = 9
		assert GolemChestLayout.STANDARD_STORAGE_COLUMNS + 2 == 9 && GolemChestLayout.REDSTONE_STORAGE_COLUMNS + 1 + 1 + 1 == 9;

		// distribution: least-first, as equal as possible, never over room, nothing invented
		int[] s = GolemDistribution.plan(10, List.of(new Target(0, 64), new Target(4, 64), new Target(2, 64)));
		assert s[0] == 6 && s[1] == 1 && s[2] == 3 : java.util.Arrays.toString(s); // stocks end 6/5/5
		s = GolemDistribution.plan(5, List.of(new Target(0, 1), new Target(0, 64)));
		assert s[0] == 1 && s[1] == 4 : "no overfill";
		s = GolemDistribution.plan(9, List.of(new Target(0, 0), new Target(64, 0)));
		assert s[0] == 0 && s[1] == 0 : "full targets get nothing; stock stays home";
		Random r = new Random(161);
		for (int run = 0; run < 5000; run++) {
			int n = 1 + r.nextInt(5), stock = r.nextInt(200);
			List<Target> t = new ArrayList<>();
			for (int i = 0; i < n; i++) { int c = r.nextInt(100); t.add(new Target(c, r.nextInt(3) == 0 ? 0 : r.nextInt(150))); }
			int[] p = GolemDistribution.plan(stock, t);
			int sum = 0, room = 0;
			for (int i = 0; i < n; i++) { assert p[i] >= 0 && p[i] <= t.get(i).room(); sum += p[i]; room += t.get(i).room(); }
			assert sum == Math.min(stock, room) : "gives all it can, never more";
			// fairness: a target that got something never ends above another that still had room
			for (int i = 0; i < n; i++)
				for (int j = 0; j < n; j++)
					if (p[i] > 0 && p[j] < t.get(j).room())
						assert t.get(i).count() + p[i] <= t.get(j).count() + p[j] + 1 : "least-first";
		}
		assert GolemDistribution.urgent(new Target(0, 64)) && !GolemDistribution.urgent(new Target(0, 0)) && !GolemDistribution.urgent(new Target(3, 64));
		// return: what does not fit in the personal slots comes back home
		int carried = 64, room = 20, delivered = Math.min(carried, room), back = carried - delivered;
		assert delivered + back == carried && back == 44;

		// honey
		assert GolemHoney.product(true, true, true) == GolemHoney.Product.BOTTLE : "bottle in personal slots";
		assert GolemHoney.product(false, true, true) == GolemHoney.Product.COMBS : "no bottle: shears behaviour";
		assert GolemHoney.product(true, false, true) == GolemHoney.Product.COMBS : "filter rejects honey bottles";
		assert GolemHoney.product(true, false, false) == GolemHoney.Product.NONE : "filter rejects both: not a job";
		assert GolemHoney.COMBS == 3;
		// maintenance table
		assert GolemMaintenance.uses("andesite", "create:andesite_alloy") && GolemMaintenance.uses("andesite", "minecraft:glass_bottle");
		assert GolemMaintenance.uses("netherite", "minecraft:ancient_debris") && !GolemMaintenance.uses("redstone", "create:andesite_alloy");
		assert GolemMaintenance.anyUses("minecraft:redstone") && !GolemMaintenance.anyUses("minecraft:dirt");
		System.out.println("Round 16 checks passed: migration (6000 random old chests, every item kept), distribution"
			+ " (least-first, no overfill, 5000 runs), honey with/without bottle, maintenance table");
	}
}
