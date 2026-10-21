package com.simibubi.create.content.equipment.golemRevival;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Lauro edition: golems that died while their chest was unloaded. A golem that dies next to its
 * loaded chest releases it on the spot (AndesiteGolem#die); otherwise the chest keeps the dead
 * golem's UUID and would refuse revival forever. Entries are consumed when that chest is revived.
 */
public class GolemGraveyard extends SavedData {

	private static final Codec<GolemGraveyard> CODEC = UUIDUtil.CODEC.listOf()
		.xmap(list -> new GolemGraveyard(new HashSet<>(list)), graveyard -> graveyard.dead.stream().toList());

	public static final SavedDataType<GolemGraveyard> TYPE = new SavedDataType<>(
		Identifier.parse("create:golem_graveyard"), level -> new GolemGraveyard(new HashSet<>()), level -> CODEC);

	private final Set<UUID> dead;

	private GolemGraveyard(Set<UUID> dead) {
		this.dead = dead;
	}

	public static GolemGraveyard get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public void bury(UUID golem) {
		if (dead.add(golem))
			setDirty();
	}

	public boolean isDead(UUID golem) {
		return dead.contains(golem);
	}

	public void forget(UUID golem) {
		if (dead.remove(golem))
			setDirty();
	}
}
