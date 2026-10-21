package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item vault lined with sturdy sheets. Behaves exactly like the regular vault, but forms
 * larger multiblocks and each block holds 10% more slots.
 */
public class ReinforcedItemVaultBlockEntity extends ItemVaultBlockEntity {

	/** Largest cross-section (width x width); the regular vault stops at 3. */
	public static final int MAX_WIDTH = 5;

	public ReinforcedItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, getSlotsPerBlock());
	}

	/**
	 * Regular vault capacity ({@code vaultCapacity} server config) plus 10%, rounded down so the
	 * bonus never exceeds 10%. Default config: 20 -> 22 slots per block.
	 */
	public static int getSlotsPerBlock() {
		long base = AllConfigs.server().logistics.vaultCapacity.get();
		return (int) Math.max(1, base * 11 / 10);
	}

	// Length still follows the regular rule of 3x the width, so a 5x5 vault reaches 15.
	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}
}
