package com.simibubi.create.content.electricity.barbedwire;

import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class BarbedWireBlock extends Block implements SimpleWaterloggedBlock, IWrenchable {
	public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");
	public static final EnumProperty<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public BarbedWireBlock(Properties props) {
		super(props);
		this.registerDefaultState(this.defaultBlockState().setValue(VERTICAL, false).setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return !(Boolean)state.getValue(WATERLOGGED);
	}

	@Override
	public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		double delta = Math.abs(entity.getX() - entity.xOld) + Math.abs(entity.getY() - entity.yOld) + Math.abs(entity.getZ() - entity.zOld);
		if((entity instanceof LivingEntity) && delta > 0d) {
			entity.hurt(new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(AllDamageTypes.BARBED_WIRE)), AllConfigs.server().electricity.barbedWireDamage.get());
			entity.playSound(SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1f, 1f);
		}
		entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
	}

	@Override
	protected FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
		builder.add(VERTICAL, HORIZONTAL_FACING, WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext c) {
		FluidState fluidstate = c.getLevel().getFluidState(c.getClickedPos());
		
		if(c.getClickedFace().getAxis() == Axis.Y)
			return defaultBlockState()
					.setValue(HORIZONTAL_FACING, c.getPlayer() != null && c.getPlayer().isShiftKeyDown() ? c.getHorizontalDirection().getClockWise() : c.getHorizontalDirection())
					.setValue(VERTICAL, false)
					.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
		else
			return defaultBlockState()
					.setValue(HORIZONTAL_FACING, c.getClickedFace().getOpposite())
					.setValue(VERTICAL, true)
					.setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
	}
	@Override
	public BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
		Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
		if (state.getValue(WATERLOGGED))
			ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
	}
}
