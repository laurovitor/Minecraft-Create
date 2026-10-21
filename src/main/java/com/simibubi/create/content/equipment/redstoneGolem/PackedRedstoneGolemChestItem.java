package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.Map;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Places the same chest block without replacing its ordinary inactive item mapping. */
public class PackedRedstoneGolemChestItem extends BlockItem {
	public PackedRedstoneGolemChestItem(Block block, Properties properties) {
		super(block, properties);
	}

	@Override
	public void registerBlocks(Map<Block, Item> map, Item item) {
		// The ordinary chest item must remain the block's canonical drop and press ingredient.
	}
}
