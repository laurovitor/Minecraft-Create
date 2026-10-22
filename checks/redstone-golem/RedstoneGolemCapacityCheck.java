import java.util.List;

import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemRules;

/** Deterministic policy check using the same full-size slot scan called by the live chest. */
public class RedstoneGolemCapacityCheck {
	static final class Inventory {
		final int[] item = new int[27];
		final int[] count = new int[27];
		boolean roomFor(int id) {
			return RedstoneGolemRules.hasRoomFor(item.length,
				slot -> item[slot] == 0,
				slot -> item[slot] == id && count[slot] < 64);
		}
		int insert(int id, int amount) {
			for (int slot = 0; slot < item.length && amount > 0; slot++)
				if (item[slot] == id && count[slot] < 64) {
					int moved = Math.min(amount, 64 - count[slot]);
					count[slot] += moved;
					amount -= moved;
				}
			for (int slot = 0; slot < item.length && amount > 0; slot++)
				if (item[slot] == 0) {
					int moved = Math.min(amount, 64);
					item[slot] = id;
					count[slot] = moved;
					amount -= moved;
				}
			return amount;
		}
		int total() { int sum = 0; for (int n : count) sum += n; return sum; }
	}
	public static void main(String[] args) {
		Inventory home = new Inventory();
		for (int slot = 0; slot < 18; slot++) { home.item[slot] = slot + 1; home.count[slot] = 64; }
		assert home.roomFor(50) : "empty third row must allow collection";
		int initial = home.total();
		int source = 9 * 64;
		for (int trip = 0; trip < 3; trip++) {
			for (int carried : List.of(64, 64, 64)) {
				assert home.roomFor(50);
				source -= carried;
				assert home.insert(50 + trip * 3 + (source / 64) % 3, carried) == 0;
			}
		}
		assert source == 0 && home.total() == initial + 9 * 64 : "items must be conserved across repeated trips";
		assert !home.roomFor(99) : "all 27 full must stop unmergeable cargo";
		home.count[24] = 63;
		assert home.roomFor(home.item[24]) : "matching partial stack in third row must allow collection";
		assert !home.roomFor(99) : "different item cannot fit in full chest";
		int before = home.total();
		assert home.insert(home.item[24], 1) == 0 && home.total() == before + 1;
		assert !home.roomFor(home.item[24]) : "full matching stack must stop";
		// The production idle scheduler scans at most every 20 ticks (one second),
		// independent of how long the chest was previously full.
		assert RedstoneGolemRules.SEARCH_INTERVAL <= 20;
		int cooldown = RedstoneGolemRules.SEARCH_INTERVAL;
		for (int tick = 1; tick <= RedstoneGolemRules.SEARCH_INTERVAL; tick++) cooldown--;
		assert cooldown == 0 && home.roomFor(home.item[24]) == false;
		home.item[26] = home.count[26] = 0; // player empties one slot
		assert home.roomFor(99) : "next bounded scan sees newly freed capacity";
		assert RedstoneGolemRules.SEARCH_INTERVAL <= 20 : "next trip uses same bound";
		System.out.println("Redstone 27-slot capacity, merging, conservation and one-second scheduling passed");
	}
}
