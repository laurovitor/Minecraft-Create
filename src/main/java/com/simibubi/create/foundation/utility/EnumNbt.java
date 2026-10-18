package com.simibubi.create.foundation.utility;

import net.minecraft.nbt.CompoundTag;

/**
 * Reads and writes Create's legacy enum tag format after Catnip removed its enum helpers.
 * Values are stored by name, so existing worlds keep the same serialized shape.
 */
public final class EnumNbt {

	public static <T extends Enum<T>> T read(CompoundTag tag, String key, Class<T> type) {
		String name = tag.getStringOr(key, "");
		for (T value : type.getEnumConstants())
			if (value.name().equals(name))
				return value;
		return type.getEnumConstants()[0];
	}

	public static void write(CompoundTag tag, String key, Enum<?> value) {
		tag.putString(key, value.name());
	}

	private EnumNbt() {
	}
}
