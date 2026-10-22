package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Lauro edition: slot layout of the golem chests (window 9 columns x 3 rows) and the migration of
 * old 27-slot chests. Pure, checked offline.
 *
 * <pre>
 * Andesite / Netherite:  [storage x7] [separator] [personal]       21 storage + 3 personal
 * Redstone:              [distribution] ½ [storage x6] ½ [personal] 3 + 18 + 3
 * </pre>
 */
public final class GolemChestLayout {
	private GolemChestLayout() {}

	public static final int ROWS = 3;
	public static final int LEGACY_COLUMNS = 9;
	public static final int LEGACY_SIZE = ROWS * LEGACY_COLUMNS;
	public static final int PERSONAL_SIZE = ROWS;
	public static final int DISTRIBUTION_SIZE = ROWS;
	public static final int STANDARD_STORAGE_COLUMNS = 7;
	public static final int REDSTONE_STORAGE_COLUMNS = 6;
	/** Old column of the first storage column (Redstone: column 0 becomes distribution). */
	public static final int STANDARD_STORAGE_OFFSET = 0;
	public static final int REDSTONE_STORAGE_OFFSET = 1;

	/** Result of reading an old 27-slot list: positional storage/personal and what must be dropped. */
	public record Migration<T>(List<T> storage, List<T> personal, List<T> overflow) {}

	/**
	 * Old slot (row, col) keeps its row and column when that column is still storage. Every other
	 * stack goes to the first free storage slot, else to a free personal slot when the golem uses it,
	 * else into {@code overflow} (dropped next to the chest). Nothing is lost or duplicated.
	 */
	public static <T> Migration<T> migrate(List<T> legacy, T empty, Predicate<T> isEmpty, Predicate<T> personalValid,
		int storageColumns, int storageOffset) {
		List<T> storage = new ArrayList<>(), personal = new ArrayList<>(), overflow = new ArrayList<>(), moved = new ArrayList<>();
		for (int i = 0; i < ROWS * storageColumns; i++)
			storage.add(empty);
		for (int i = 0; i < PERSONAL_SIZE; i++)
			personal.add(empty);
		for (int i = 0; i < legacy.size(); i++) {
			T stack = legacy.get(i);
			if (isEmpty.test(stack))
				continue;
			int row = i / LEGACY_COLUMNS, col = i % LEGACY_COLUMNS - storageOffset;
			if (i < LEGACY_SIZE && col >= 0 && col < storageColumns)
				storage.set(row * storageColumns + col, stack);
			else
				moved.add(stack);
		}
		for (T stack : moved) {
			int free = storage.indexOf(empty);
			if (free < 0)
				for (int s = 0; s < storage.size() && free < 0; s++)
					if (isEmpty.test(storage.get(s)))
						free = s;
			if (free >= 0) {
				storage.set(free, stack);
				continue;
			}
			int p = -1;
			for (int s = 0; s < personal.size() && p < 0; s++)
				if (isEmpty.test(personal.get(s)))
					p = s;
			if (p >= 0 && personalValid.test(stack))
				personal.set(p, stack);
			else
				overflow.add(stack);
		}
		return new Migration<>(storage, personal, overflow);
	}
}
