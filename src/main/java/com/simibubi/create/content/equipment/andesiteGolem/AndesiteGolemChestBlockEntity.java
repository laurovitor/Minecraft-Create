package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Clearable;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * Lauro edition: home of an Andesite Golem. Holds what the golem gathers (27 slots, like a chest)
 * and a Create filter on its lid that tells the golem what to gather and keep.
 */
public class AndesiteGolemChestBlockEntity extends SmartBlockEntity implements Clearable {

	public static final int SIZE = 27;

	public FilteringBehaviour filtering;
	protected final SimpleContainer container = new SimpleContainer(SIZE) {
		@Override
		public void setChanged() {
			super.setChanged();
			AndesiteGolemChestBlockEntity.this.setChanged();
		}
	};
	/** The golem living off this chest; null once it died or never existed (placed by hand). */
	protected @Nullable UUID golem;

	public AndesiteGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.ANDESITE_GOLEM_CHEST.get(),
			(be, context) -> VanillaContainerWrapper.of(be.container));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = new FilteringBehaviour(this, new AndesiteGolemChestFilterSlot()));
	}

	/** Keeps the latch tint (GolemChestLinks.LINKED) in step with the network, every lazy tick (0.5 s). */
	@Override
	public void lazyTick() {
		super.lazyTick();
		if (level == null || level.isClientSide())
			return;
		var linked = com.simibubi.create.content.equipment.redstoneGolem.GolemChestLinks.LINKED;
		BlockState state = getBlockState();
		if (!state.hasProperty(linked))
			return;
		boolean connected = com.simibubi.create.content.equipment.redstoneGolem.GolemChestLinks.isConnected(level, this);
		if (state.getValue(linked) != connected)
			level.setBlock(worldPosition, state.setValue(linked, connected), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
	}

	public SimpleContainer getContainer() {
		return container;
	}

	/** Empty filter means "everything". */
	public boolean accepts(ItemStack stack) {
		return filtering == null || filtering.test(stack);
	}

	public boolean hasEmptySlot() {
		for (int i = 0; i < container.getContainerSize(); i++)
			if (container.getItem(i).isEmpty())
				return true;
		return false;
	}

	/** @return what did not fit */
	public ItemStack insert(ItemStack stack) {
		return container.addItem(stack);
	}

	/** Takes one item matching the predicate out of the chest, e.g. a sapling to replant. */
	public ItemStack takeOne(java.util.function.Predicate<ItemStack> predicate) {
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty() && predicate.test(stack))
				return container.removeItem(i, 1);
		}
		return ItemStack.EMPTY;
	}

	/** The bound golem's UUID (synced to clients), or null when the chest has none. Read-only. */
	public @Nullable UUID getGolemId() {
		return golem;
	}

	public boolean isHomeOf(UUID golemId) {
		return golemId.equals(golem);
	}

	/** The linked entity, if its chunk is currently loaded. */
	public @Nullable AndesiteGolem getGolem() {
		if (golem == null || !(level instanceof ServerLevel server))
			return null;
		return server.getEntity(golem) instanceof AndesiteGolem entity ? entity : null;
	}

	public void linkGolem(UUID golemId) {
		golem = golemId;
		notifyUpdate();
	}

	/** The golem died: the chest is left without one for good. */
	public void releaseGolem() {
		golem = null;
		BlockState state = getBlockState();
		if (state.getBlock() instanceof AndesiteGolemChestBlock && state.getValue(AndesiteGolemChestBlock.ACTIVE))
			level.setBlockAndUpdate(worldPosition, state.setValue(AndesiteGolemChestBlock.ACTIVE, false));
		notifyUpdate();
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		if (golem != null)
			tag.store("Golem", UUIDUtil.CODEC, golem);
		if (!clientPacket)
			tag.store("Items", ItemStack.OPTIONAL_CODEC.listOf(), registries.createSerializationContext(NbtOps.INSTANCE),
				container.getItems());
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		golem = tag.read("Golem", UUIDUtil.CODEC).orElse(null);
		if (clientPacket)
			return;
		container.clearContent();
		List<ItemStack> items = tag.read("Items", ItemStack.OPTIONAL_CODEC.listOf(),
			registries.createSerializationContext(NbtOps.INSTANCE)).orElse(List.of());
		NonNullList<ItemStack> target = container.getItems();
		for (int i = 0; i < Math.min(items.size(), target.size()); i++)
			target.set(i, items.get(i));
	}

	@Override
	public void destroy() {
		// Breaking the home packs the live golem; this is not a death and must not
		// turn the chest into the recyclable, inactive variant.
		AndesiteGolem linked = getGolem();
		if (linked != null)
			linked.discard();
		super.destroy();
		if (level != null)
			Containers.dropContents(level, worldPosition, container);
	}

	@Override
	public void clearContent() {
		container.clearContent();
	}
}
