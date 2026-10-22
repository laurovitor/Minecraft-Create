package com.simibubi.create.content.electricity.tesla;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class TeslaCoilBlock extends Block implements IBE<TeslaCoilBlockEntity>, IWrenchable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public TeslaCoilBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
    }
    @Override public Class<TeslaCoilBlockEntity> getBlockEntityClass() { return TeslaCoilBlockEntity.class; }
    @Override public BlockEntityType<? extends TeslaCoilBlockEntity> getBlockEntityType() { return AllBlockEntityTypes.TESLA_COIL.get(); }
    @Override protected void createBlockStateDefinition(Builder<Block, BlockState> builder) { builder.add(FACING, POWERED); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.isSecondaryUseActive() ? c.getClickedFace() : c.getClickedFace().getOpposite());
    }
    public static void setPowered(Level level, BlockPos pos, boolean powered) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(POWERED) && state.getValue(POWERED) != powered) level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
    }
}
