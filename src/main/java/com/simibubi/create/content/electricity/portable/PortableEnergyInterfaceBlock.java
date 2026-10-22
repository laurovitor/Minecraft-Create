package com.simibubi.create.content.electricity.portable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PortableEnergyInterfaceBlock extends WrenchableDirectionalBlock implements IBE<PortableEnergyInterfaceBlockEntity> {
    public PortableEnergyInterfaceBlock(Properties p) { super(p); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
        @Nullable Orientation orientation, boolean moving) { withBlockEntityDo(level,pos,PortableEnergyInterfaceBlockEntity::neighbourChanged); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) {
        Direction facing = c.getNearestLookingDirection();
        if (c.getPlayer() != null && c.getPlayer().isShiftKeyDown()) facing = facing.getOpposite();
        return defaultBlockState().setValue(FACING, facing.getOpposite());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext c) {
        return AllShapes.PORTABLE_STORAGE_INTERFACE.get(state.getValue(FACING));
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return getBlockEntityOptional(level,pos).map(be -> be.isTransferring() ? 15 : 0).orElse(0);
    }
    @Override public Class<PortableEnergyInterfaceBlockEntity> getBlockEntityClass() { return PortableEnergyInterfaceBlockEntity.class; }
    @Override public BlockEntityType<? extends PortableEnergyInterfaceBlockEntity> getBlockEntityType() { return AllBlockEntityTypes.PORTABLE_ENERGY_INTERFACE.get(); }
}
