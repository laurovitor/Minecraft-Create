package com.simibubi.create.content.equipment.golemRevival;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolem;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlock;
import com.simibubi.create.content.equipment.netheriteGolem.NetheriteGolemChestBlock;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemChestBlock;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Lauro edition: every golem type, for its revival pumpkin. A new golem gets a pumpkin by adding one
 * constant here, one AllBlocks.revivalPumpkin line, and its recipe, assets and lang entries.
 * {@code id} names the pumpkin block ({@code <id>_revival_pumpkin}).
 */
public enum GolemKind {
	ANDESITE("andesite", () -> AllBlocks.ANDESITE_GOLEM_CHEST.get(), () -> AllEntityTypes.ANDESITE_GOLEM.get(),
		AndesiteGolemChestBlock.ACTIVE),
	NETHERITE("netherite", () -> AllBlocks.NETHERITE_GOLEM_CHEST.get(), () -> AllEntityTypes.NETHERITE_GOLEM.get(),
		NetheriteGolemChestBlock.ACTIVE),
	REDSTONE("redstone", () -> AllBlocks.REDSTONE_GOLEM_CHEST.get(), () -> AllEntityTypes.REDSTONE_GOLEM.get(),
		RedstoneGolemChestBlock.ACTIVE);

	public final String id;
	private final Supplier<Block> chest;
	private final Supplier<? extends EntityType<? extends AndesiteGolem>> golem;
	/** The chest's "has a live golem" blockstate flag (inactive = dead/unbound look). */
	public final BooleanProperty active;

	GolemKind(String id, Supplier<Block> chest, Supplier<? extends EntityType<? extends AndesiteGolem>> golem,
		BooleanProperty active) {
		this.id = id;
		this.chest = chest;
		this.golem = golem;
		this.active = active;
	}

	public Block chest() { return chest.get(); }
	public EntityType<? extends AndesiteGolem> golem() { return golem.get(); }

	/** The golem kind whose chest this is, or null for any other block. */
	public static @Nullable GolemKind ofChest(BlockState state) {
		for (GolemKind kind : values())
			if (state.is(kind.chest()))
				return kind;
		return null;
	}
}
