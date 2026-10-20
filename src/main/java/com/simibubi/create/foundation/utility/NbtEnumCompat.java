package com.simibubi.create.foundation.utility;

import net.minecraft.nbt.CompoundTag;

/** Stable enum NBT format for Create data after Catnip removed its enum helpers. */
public final class NbtEnumCompat {
	private NbtEnumCompat() {}

	public static <E extends Enum<E>> void writeEnum(CompoundTag tag, String key, E value) {
		tag.putString(key, value.name());
	}

	public static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> type) {
		E[] values = type.getEnumConstants();
		if (values.length == 0)
			throw new IllegalArgumentException("Enum has no constants: " + type.getName());
		try {
			return Enum.valueOf(type, tag.getStringOr(key, ""));
		} catch (IllegalArgumentException ignored) {
			return values[0];
		}
	}
}
