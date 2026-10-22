package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.content.equipment.redstoneGolem.SourceReservations;

import net.minecraft.core.GlobalPos;

/**
 * Lauro edition: which Andesite golem is working on which tree or crop. Several golems pick
 * different jobs in parallel instead of all walking to the same nearest one. Same lifecycle as the
 * Redstone source claims: renewed every work tick, released on finish, give-up, skip, goal stop and
 * entity removal, and lapsing after RESERVATION_TTL ticks without renewal.
 */
public final class AndesiteGolemWorkReservations {
	private AndesiteGolemWorkReservations() {}

	/** Only touched on the server thread from goal ticks and entity removal. */
	public static final SourceReservations<GlobalPos> SERVER = new SourceReservations<>();
}
