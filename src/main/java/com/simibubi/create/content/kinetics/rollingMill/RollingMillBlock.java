package com.simibubi.create.content.kinetics.rollingMill;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.EntityRestingOnBlock;
import com.simibubi.create.foundation.block.IBE;

import net.createmod.catnip.api.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class RollingMillBlock extends HorizontalKineticBlock implements EntityRestingOnBlock, IBE<RollingMillBlockEntity> {

	public RollingMillBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return AllShapes.ROLLING_MILL;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
		InteractionHand hand, BlockHitResult hitResult) {
		if (!stack.isEmpty())
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		if (level.isClientSide())
			return InteractionResult.SUCCESS;

		withBlockEntityDo(level, pos, rollingMill -> {
			boolean emptyOutput = true;
			ItemStacksResourceHandler inv = rollingMill.outputInv;
			for (int slot = 0; slot < inv.size(); slot++) {
				ItemStack stackInSlot = ItemUtil.getStack(inv, slot);
				if (!stackInSlot.isEmpty())
					emptyOutput = false;
				player.getInventory()
					.placeItemBackInInventory(stackInSlot, Prediction.SERVER_ONLY);
				inv.set(slot, ItemResource.EMPTY, 0);
			}

			if (emptyOutput) {
				inv = rollingMill.inputInv;
				for (int slot = 0; slot < inv.size(); slot++) {
					player.getInventory()
						.placeItemBackInInventory(ItemUtil.getStack(inv, slot), Prediction.SERVER_ONLY);
					inv.set(slot, ItemResource.EMPTY, 0);
				}
			}

			rollingMill.setChanged();
			rollingMill.sendData();
		});

		return InteractionResult.SUCCESS;
	}

	@Override
	public void updateEntityAfterFallOn(Level worldIn, Entity entityIn) {
		if (entityIn.level()
			.isClientSide())
			return;
		if (!(entityIn instanceof ItemEntity itemEntity))
			return;
		if (!entityIn.isAlive())
			return;

		RollingMillBlockEntity rollingMill = null;
		for (BlockPos pos : Iterate.hereAndBelow(entityIn.blockPosition()))
			if (rollingMill == null)
				rollingMill = getBlockEntity(worldIn, pos);
		if (rollingMill == null)
			return;

		ResourceHandler<ItemResource> capability = rollingMill.getLevel()
			.getCapability(Capabilities.Item.BLOCK, rollingMill.getBlockPos(), null);
		if (capability == null)
			return;

		ItemStack item = itemEntity.getItem();
		ItemStack remainder;
		try (Transaction transaction = Transaction.openRoot()) {
			int transferred = item.isEmpty() ? 0 : capability.insert(0, ItemResource.of(item), item.getCount(), transaction);
			remainder = transferred == item.getCount() ? ItemStack.EMPTY : item.copyWithCount(item.getCount() - transferred);
			transaction.commit();
		}
		if (remainder.isEmpty())
			itemEntity.discard();
		if (remainder.getCount() < item.getCount())
			itemEntity.setItem(remainder);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferredSide = getPreferredHorizontalFacing(context);
		if (preferredSide != null)
			return defaultBlockState().setValue(HORIZONTAL_FACING, preferredSide);
		return super.getStateForPlacement(context);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face.getAxis() == state.getValue(HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public Class<RollingMillBlockEntity> getBlockEntityClass() {
		return RollingMillBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends RollingMillBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ROLLING_MILL.get();
	}

	@Override
	protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
		return false;
	}
}
