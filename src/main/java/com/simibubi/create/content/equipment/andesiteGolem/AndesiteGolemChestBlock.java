package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.foundation.utility.RegistryNbt;
import java.util.List;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.context.UseOnContext;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;


/**
 * Lauro edition: chest of an Andesite Golem. ACTIVE while its golem lives; a placed chest or one
 * whose golem died is inactive (different texture) and only good for pressing back into alloy.
 */
public class AndesiteGolemChestBlock extends HorizontalDirectionalBlock
	implements IBE<AndesiteGolemChestBlockEntity>, IWrenchable {

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
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		if (!level.isClientSide() && !player.isCreative())
			Block.popResource(level, pos, takeAsItem(level, pos, state));
		return super.playerWillDestroy(level, pos, state, player);
	}

	/** Shift + wrench picks the chest up into the inventory, golem included, like other Create blocks. */
	@Override
	public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Player player = context.getPlayer();
		if (!(level instanceof ServerLevel))
			return InteractionResult.SUCCESS;
		ItemStack drop = takeAsItem(level, pos, state);
		if (player != null && !player.isCreative())
			player.getInventory().placeItemBackInInventory(drop, Prediction.SERVER_ONLY);
		level.destroyBlock(pos, false);
		IWrenchable.playRemoveSound(level, pos);
		return InteractionResult.SUCCESS;
	}

	/**
	 * The chest as an item. With a live golem this is the packed chest carrying the golem's health,
	 * the chest contents, filter and what the golem carried; the golem itself is removed with the block
	 * (see the block entity). Without one it is the plain, pressable chest.
	 */
	private ItemStack takeAsItem(Level level, BlockPos pos, BlockState state) {
		if (!state.getValue(ACTIVE) || !(level.getBlockEntity(pos) instanceof AndesiteGolemChestBlockEntity chest)
			|| !(chest.getGolem() instanceof AndesiteGolem golem))
			return new ItemStack(this);
		ItemStack drop = new ItemStack(AllItems.PACKED_ANDESITE_GOLEM_CHEST.get());
		CompoundTag data = new CompoundTag();
		data.putFloat("Health", golem.getHealth());
		data.putLong("CreatedGameTime", golem.getCreatedGameTime());
		data.store("Items", ItemStack.OPTIONAL_CODEC.listOf(), RegistryNbt.ops(level.registryAccess()),
			chest.getContainer().getItems());
		data.store("Filter", ItemStack.OPTIONAL_CODEC, RegistryNbt.ops(level.registryAccess()),
			chest.filtering.getFilter());
		data.putInt("FilterAmount", chest.filtering.count);
		data.putBoolean("UpTo", chest.filtering.upTo);
		data.store("Carried", ItemStack.CODEC.listOf(), RegistryNbt.ops(level.registryAccess()),
			golem.getCarried());
		drop.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
		// these now live inside the packed item; do not drop duplicates on removal
		chest.getContainer().clearContent();
		chest.filtering.setFilter(ItemStack.EMPTY);
		golem.getCarried().clear();
		return drop;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (!(level instanceof ServerLevel) || !AllItems.PACKED_ANDESITE_GOLEM_CHEST.isIn(stack))
			return;
		AndesiteGolem golem = AllEntityTypes.ANDESITE_GOLEM.get().create(level, EntitySpawnReason.TRIGGERED);
		if (golem == null)
			return;
		float health = stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData custom
			? custom.copyTag().getFloat("Health").orElse(golem.getMaxHealth()) : golem.getMaxHealth();
		golem.setHealth(Math.clamp(health, 0.01f, golem.getMaxHealth()));
		if (stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData ageData)
			golem.setCreatedGameTime(ageData.copyTag().getLongOr("CreatedGameTime", level.getGameTime()));
		golem.snapTo(pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0);
		golem.setChest(pos);
		if (level.getBlockEntity(pos) instanceof AndesiteGolemChestBlockEntity chest) {
			chest.linkGolem(golem.getUUID());
			if (stack.get(DataComponents.CUSTOM_DATA) instanceof CustomData custom) {
				CompoundTag data = custom.copyTag();
				List<ItemStack> items = data.read("Items", ItemStack.OPTIONAL_CODEC.listOf(),
					RegistryNbt.ops(level.registryAccess())).orElse(List.of());
				for (int i = 0; i < Math.min(items.size(), chest.getContainer().getContainerSize()); i++)
					chest.getContainer().setItem(i, items.get(i));
				chest.filtering.setFilter(data.read("Filter", ItemStack.OPTIONAL_CODEC,
					RegistryNbt.ops(level.registryAccess())).orElse(ItemStack.EMPTY));
				chest.filtering.count = data.getIntOr("FilterAmount", chest.filtering.count);
				chest.filtering.upTo = data.getBooleanOr("UpTo", chest.filtering.upTo);
				data.read("Carried", ItemStack.CODEC.listOf(), RegistryNbt.ops(level.registryAccess()))
					.ifPresent(golem.getCarried()::addAll);
				golem.updateHeldItem();
			}
		}
		level.setBlockAndUpdate(pos, state.setValue(ACTIVE, true));
		level.addFreshEntity(golem);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Not copper: an axe never scrapes, strips or un-waxes the chest (NeoForge tool hook). */
	@Override
	public @org.jetbrains.annotations.Nullable BlockState getToolModifiedState(BlockState state, UseOnContext context,
		net.neoforged.neoforge.common.ItemAbility itemAbility, boolean simulate) {
		return null;
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
