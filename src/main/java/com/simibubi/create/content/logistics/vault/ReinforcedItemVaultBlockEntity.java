package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Item vault lined with sturdy sheets. Behaves exactly like the regular vault, but forms
 * larger multiblocks and each block holds 25% more slots.
 */
public class ReinforcedItemVaultBlockEntity extends ItemVaultBlockEntity {

	/** Largest cross-section (width x width); the regular vault stops at 3. */
	public static final int MAX_WIDTH = 5;
	/** Length gained per block of width; the regular vault uses 3 (3, 6, 9), this one 5 (5, 10, 15). */
	public static final int LENGTH_PER_WIDTH = 5;
	/** Longest vault of any width, reached from width 3 upwards. */
	public static final int MAX_LENGTH = 15;

	public ReinforcedItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, getSlotsPerBlock());
	}

	/**
	 * Regular vault capacity ({@code vaultCapacity} server config) plus 25%, rounded down.
	 * Default config: 20 -> 25 slots per block.
	 */
	public static int getSlotsPerBlock() {
		long base = AllConfigs.server().logistics.vaultCapacity.get();
		return (int) Math.max(1, base * 5 / 4);
	}

	@Override
	public int getMaxWidth() {
		return MAX_WIDTH;
	}

	// Width 1 -> 5 long, width 2 -> 10, width 3 to 5 -> 15
	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y)
			return getMaxWidth();
		return Math.min(width * LENGTH_PER_WIDTH, MAX_LENGTH);
	}
}
