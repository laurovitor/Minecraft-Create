package com.simibubi.create.content.electricity.creative;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.logistics.crate.CrateBlock;

import com.simibubi.create.foundation.block.IBE;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeEnergyBlock extends CrateBlock implements IBE<CreativeEnergyBlockEntity> {
	
	public CreativeEnergyBlock(Properties props) {
		super(props);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.CREATIVE_ENERGY;
	}

	@Override
	public Class<CreativeEnergyBlockEntity> getBlockEntityClass() {
		return CreativeEnergyBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends CreativeEnergyBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.CREATIVE_ENERGY.get();
	}
	
	@Override
	protected void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, @Nullable Orientation orientation, boolean isMoving) {
		BlockEntity tileentity = state.hasBlockEntity() ? worldIn.getBlockEntity(pos) : null;
		if(tileentity != null) {
			if(tileentity instanceof CreativeEnergyBlockEntity) {
				((CreativeEnergyBlockEntity)tileentity).updateCache();
			}
		}
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return AllBlockEntityTypes.CREATIVE_ENERGY.create(pos, state);
	}
}
