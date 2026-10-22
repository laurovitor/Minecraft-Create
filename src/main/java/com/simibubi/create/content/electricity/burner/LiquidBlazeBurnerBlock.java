package com.simibubi.create.content.electricity.burner;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

/** Blaze Burner variant accepting fluid through its block capability or a held fluid container. */
public class LiquidBlazeBurnerBlock extends HorizontalDirectionalBlock implements IBE<LiquidBlazeBurnerBlockEntity>, IWrenchable {
    public LiquidBlazeBurnerBlock(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, BlazeBurnerBlock.HeatLevel.SMOULDERING));
    }
    @Override protected void createBlockStateDefinition(Builder<Block,BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlazeBurnerBlock.HEAT_LEVEL,FACING);
    }
    public static int getLight(BlockState state) {
        return switch (state.getValue(BlazeBurnerBlock.HEAT_LEVEL)) { case NONE -> 0; case SMOULDERING -> 8; default -> 15; };
    }
    @Override public Class<LiquidBlazeBurnerBlockEntity> getBlockEntityClass() { return LiquidBlazeBurnerBlockEntity.class; }
    @Override public BlockEntityType<? extends LiquidBlazeBurnerBlockEntity> getBlockEntityType() { return AllBlockEntityTypes.LIQUID_BLAZE_BURNER.get(); }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
        Player player, InteractionHand hand, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection(), null)) return InteractionResult.SUCCESS;
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return BlazeBurnerBlock.tryInsert(state,level,pos,stack,player.isCreative(),true,false);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext c) { return AllShapes.HEATER_BLOCK_SHAPE; }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos,Direction dir) {
        return Math.max(0,state.getValue(BlazeBurnerBlock.HEAT_LEVEL).ordinal()-1);
    }
    @Override protected boolean isPathfindable(BlockState state,PathComputationType type) { return false; }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random) {
        if (random.nextInt(10)==0 && state.getValue(BlazeBurnerBlock.HEAT_LEVEL).isAtLeast(BlazeBurnerBlock.HeatLevel.SMOULDERING))
            level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,SoundEvents.CAMPFIRE_CRACKLE,SoundSource.BLOCKS,.5f,1f,false);
    }
}
