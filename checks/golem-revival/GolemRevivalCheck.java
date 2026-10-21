import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.equipment.golemRevival.GolemRevivalRules;
import com.simibubi.create.content.equipment.golemRevival.GolemRevivalRules.Outcome;

/**
 * Offline check of revival-pumpkin binding; run with java -ea, compiled with the real
 * GolemRevivalRules. The lifecycle model mirrors the code: AndesiteGolem#die releases a loaded
 * chest (UUID -> null), GolemRevivalEvents buries the UUID when the chest is unloaded, and
 * RevivalPumpkinBlock#revive forgets the old UUID and binds the new golem. No Minecraft runtime.
 */
public class GolemRevivalCheck {

	static final class Chest {
		final int kind;
		UUID bound;
		boolean chunkLoaded = true;
		boolean active;
		final List<String> contents = new ArrayList<>(List.of("cobblestone x64", "filter:oak_log", "freq:abc"));
		Chest(int kind) { this.kind = kind; }
	}

	static final class Golem {
		final UUID id = UUID.randomUUID();
		final Chest home;
		boolean alive = true;
		Golem(Chest home) { this.home = home; }
	}

	static final Set<UUID> graveyard = new HashSet<>();
	static final List<Golem> golems = new ArrayList<>();

	static Outcome place(int pumpkinKind, Chest chest) {
		Outcome o = GolemRevivalRules.decide(chest != null, chest != null && chest.kind == pumpkinKind,
			chest != null && chest.bound != null, chest != null && chest.bound != null && graveyard.contains(chest.bound));
		if (o == Outcome.REVIVE) {
			if (chest.bound != null)
				graveyard.remove(chest.bound);
			Golem g = new Golem(chest);
			golems.add(g);
			chest.bound = g.id;
			chest.active = true;
		}
		return o;
	}

	static void die(Golem g) {
		g.alive = false;
		if (g.home.chunkLoaded && g.id.equals(g.home.bound)) {
			g.home.bound = null; // releaseGolem
			g.home.active = false;
		} else if (!g.home.chunkLoaded)
			graveyard.add(g.id); // recordUnheardDeath
	}

	public static void main(String[] args) {
		// decision matrix
		assert GolemRevivalRules.decide(false, false, false, false) == Outcome.DECORATIVE;
		assert GolemRevivalRules.decide(true, false, false, false) == Outcome.REFUSE_WRONG_MATERIAL;
		assert GolemRevivalRules.decide(true, false, true, true) == Outcome.REFUSE_WRONG_MATERIAL;
		assert GolemRevivalRules.decide(true, true, false, false) == Outcome.REVIVE;
		assert GolemRevivalRules.decide(true, true, true, false) == Outcome.REFUSE_HAS_GOLEM;
		assert GolemRevivalRules.decide(true, true, true, true) == Outcome.REVIVE : "known-dead binding is free";

		// scenario: dead chest revived, keeps contents, leaves the dead (inactive) state
		Chest c = new Chest(0);
		assert place(0, c) == Outcome.REVIVE && c.active && c.contents.size() == 3;
		Golem first = golems.get(0);
		assert place(0, c) == Outcome.REFUSE_HAS_GOLEM : "one live golem per chest";
		c.chunkLoaded = false; // golem wanders, chest unloaded: still bound, still refused later
		assert place(0, c) == Outcome.REFUSE_HAS_GOLEM : "unloaded golem counts as alive";
		die(first); // dies while chest unloaded -> graveyard
		c.chunkLoaded = true;
		assert c.bound == first.id && graveyard.contains(first.id);
		assert place(1, c) == Outcome.REFUSE_WRONG_MATERIAL;
		assert place(0, c) == Outcome.REVIVE && !graveyard.contains(first.id) : "graveyard consumed";

		// randomized lifecycle: never two live golems bound to one chest
		Random r = new Random(3);
		List<Chest> chests = new ArrayList<>();
		for (int i = 0; i < 6; i++) chests.add(new Chest(i % 3));
		for (int step = 0; step < 20000; step++) {
			Chest ch = chests.get(r.nextInt(chests.size()));
			switch (r.nextInt(4)) {
			case 0 -> place(r.nextInt(3), ch);
			case 1 -> ch.chunkLoaded = !ch.chunkLoaded;
			case 2 -> golems.stream().filter(g -> g.alive && g.home == ch).findFirst().ifPresent(GolemRevivalCheck::die);
			default -> place(r.nextInt(3), null);
			}
			for (Chest each : chests) {
				long live = golems.stream().filter(g -> g.alive && g.home == each && g.id.equals(each.bound)).count();
				long liveAny = golems.stream().filter(g -> g.alive && g.home == each).count();
				assert liveAny <= 1 : "duplicate live golems on one chest";
				assert live == liveAny : "a live golem is always the bound one";
				assert each.contents.size() == 3 : "chest data never touched";
			}
		}
		System.out.println("Golem revival checks passed: decision matrix, scenario, 20000 random lifecycle steps");
	}
}
