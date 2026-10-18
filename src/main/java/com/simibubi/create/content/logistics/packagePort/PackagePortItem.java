package com.simibubi.create.content.logistics.packagePort;

import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class PackagePortItem extends BlockItem {

	public PackagePortItem(Block pBlock, Properties pProperties) {
		super(pBlock, pProperties);
	}

	@Override
	public void onBlockPlacedBy(BlockPlaceContext context, Level world, BlockPos pos, BlockState state, Player player, ItemStack stack) {
		if (!world.isClientSide() && player instanceof ServerPlayer sp)
			NetworkHelper.INSTANCE.sendToClient(sp, new PackagePortPlacementPacket.ClientBoundRequest(pos));
	}

}
