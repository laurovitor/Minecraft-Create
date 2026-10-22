package com.simibubi.create.content.electricity.connector;

import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlock;
import com.simibubi.create.content.electricity.connector.base.ConnectorMode;
import com.simibubi.create.content.electricity.connector.base.ConnectorVariant;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LargeConnectorBlock extends AbstractConnectorBlock<LargeConnectorBlockEntity> {
    public LargeConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<LargeConnectorBlockEntity> getBlockEntityClass() {
        return LargeConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LargeConnectorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.LARGE_CONNECTOR.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return AllBlockEntityTypes.LARGE_CONNECTOR.create(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return AllShapes.LARGE_CONNECTOR.get(state.getValue(FACING).getOpposite());
    }

}