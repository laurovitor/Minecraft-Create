package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


/**
 * Lauro edition: chest of an Andesite Golem. ACTIVE while its golem lives; a placed chest or one
 * whose golem died is inactive (different texture) and only good for pressing back into alloy.
 */
public class AndesiteGolemChestBlock extends HorizontalDirectionalBlock implements IBE<AndesiteGolemChestBlockEntity> {

	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
	private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

	public AndesiteGolemChestBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING, ACTIVE));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
		if (level.isClientSide())
			return InteractionResult.SUCCESS;
		withBlockEntityDo(level, pos, be -> player.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> ChestMenu.threeRows(id, inventory, be.getContainer()), getName())));
		return InteractionResult.SUCCESS;
	}

	@Override
	protected boolean hasAnalogOutputSignal(BlockState state) {
		return true;
	}

	@Override
	protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
		return getBlockEntityOptional(level, pos)
			.map(be -> net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(be.getContainer()))
			.orElse(0);
	}

	@Override
	public Class<AndesiteGolemChestBlockEntity> getBlockEntityClass() {
		return AndesiteGolemChestBlockEntity.class;
	}

	@Override
	public BlockEntityType<? extends AndesiteGolemChestBlockEntity> getBlockEntityType() {
		return AllBlockEntityTypes.ANDESITE_GOLEM_CHEST.get();
	}
}
