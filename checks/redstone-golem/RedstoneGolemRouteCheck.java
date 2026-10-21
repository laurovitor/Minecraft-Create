import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules;

/** Offline voxel fixture for the stand-spot policy. This is not Minecraft pathfinding. */
public class RedstoneGolemRouteCheck {
	record P(int x, int z) { P add(int dx, int dz) { return new P(x + dx, z + dz); } }
	static final int[][] DIR = {{1,0},{-1,0},{0,1},{0,-1}};
	static Set<P> flood(P start, Set<P> floor) {
		Set<P> seen = new HashSet<>();
		ArrayDeque<P> queue = new ArrayDeque<>();
		queue.add(start);
		while (!queue.isEmpty()) {
			P p = queue.remove();
			if (!floor.contains(p) || !seen.add(p)) continue;
			for (int[] d : DIR) queue.add(p.add(d[0], d[1]));
		}
		return seen;
	}
	static void testRoom(boolean source) {
		// A winding single-width cave entrance, with a roof directly above the chest.
		Set<P> floor = new HashSet<>();
		for (int x = 0; x <= 8; x++) floor.add(new P(x, 0));
		for (int z = 0; z <= 6; z++) floor.add(new P(8, z));
		for (int x = 3; x <= 8; x++) floor.add(new P(x, 6));
		for (int z = 4; z <= 6; z++) floor.add(new P(3, z));
		floor.add(new P(4, 4)); // the only chest-side standing tile
		P chest = new P(5, 4);
		Set<P> reachable = flood(new P(0, 0), floor);
		assert reachable.contains(new P(4, 4)) : "winding entrance must reach the " + (source ? "source" : "home");
		assert RedstoneGolemRules.validStandSpot(-1, 0, 0, true, true, true, true);
		assert !RedstoneGolemRules.validStandSpot(0, 2, 0, true, true, true, true) : "roof is not arrival";
		assert !RedstoneGolemRules.validStandSpot(-1, 1, 0, true, true, true, false) : "roof blocks chest interaction";
		assert !RedstoneGolemRules.validStandSpot(-1, 0, 0, true, true, false, true) : "no unsafe floor hatch";
		assert !RedstoneGolemRules.validStandSpot(-1, 0, 0, false, true, true, true) : "wall is not a standing tile";
		assert chest.x() == 5; // chest itself is not in the walkable floor set
	}
	public static void main(String[] args) {
		testRoom(false); // chest in a cave under a hill
		testRoom(true);  // source in a roofed building
		System.out.println("route geometry fixtures passed");
	}
}
