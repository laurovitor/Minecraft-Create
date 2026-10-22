package com.simibubi.create.content.equipment.redstoneGolem;

import net.minecraft.core.GlobalPos;

/** Server-side claims on source chests, shared by every Redstone Golem (any network). */
public final class RedstoneGolemReservations {
	private RedstoneGolemReservations() {}

	/** Only touched on the server thread from goal ticks and entity removal. */
	public static final SourceReservations<GlobalPos> SERVER = new SourceReservations<>();
}
