package com.simibubi.create.content.logistics.vault;

import com.simibubi.create.AllBlockEntityTypes;

import net.minecraft.world.level.block.entity.BlockEntityType;

public class ReinforcedItemVaultBlock extends ItemVaultBlock {

	public ReinforcedItemVaultBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntityType<? extends ItemVaultBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.REINFORCED_ITEM_VAULT.get();
	}
}
