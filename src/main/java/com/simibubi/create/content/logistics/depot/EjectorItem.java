package com.simibubi.create.content.logistics.depot;


import com.simibubi.create.foundation.item.BlockBreakingItem;
import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EjectorItem extends BlockItem implements BlockBreakingItem {

	public EjectorItem(Block p_i48527_1_, Properties p_i48527_2_) {
		super(p_i48527_1_, p_i48527_2_);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		if (player != null && player.isShiftKeyDown())
			return InteractionResult.SUCCESS;
		return super.useOn(ctx);
	}

	@Override
	protected BlockState getPlacementState(BlockPlaceContext p_195945_1_) {
		BlockState stateForPlacement = super.getPlacementState(p_195945_1_);
		return stateForPlacement;
	}

	@Override
	public void onBlockPlacedBy(BlockPlaceContext context, Level world, BlockPos pos, BlockState state, Player player, ItemStack stack) {
		if (!world.isClientSide() && player instanceof ServerPlayer sp)
			NetworkHelper.INSTANCE.sendToClient(sp, new EjectorPlacementPacket.ClientBoundRequest(pos));
	}

	public boolean canAttackBlock(BlockState state, Level world, BlockPos pos,
		Player p_195938_4_) {
		return !p_195938_4_.isShiftKeyDown();
	}

}
