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
 * Lauro edition: home of an Andesite Golem. Holds what the golem gathers (21 storage slots, see
 * GolemChestLayout), 3 personal slots for the golem's own maintenance items, and a Create filter on
 * its lid that tells the golem what to gather and keep. Automation only ever sees the storage.
 */
public class AndesiteGolemChestBlockEntity extends SmartBlockEntity implements Clearable {

	/** Storage slots of the Andesite and Netherite chests (7 columns x 3 rows). */
	public static final int SIZE = GolemChestLayout.ROWS * GolemChestLayout.STANDARD_STORAGE_COLUMNS;
	/** Save layout: 2 = storage + personal (+ distribution); absent = old 27-slot "Items". */
	private static final int LAYOUT_VERSION = 2;

	public FilteringBehaviour filtering;
	/** Storage: what automation, hoppers, packagers and Redstone golems see. */
	protected final SimpleContainer container;
	/** The golem's own slots (repair items, glass bottles): never exposed to automation. */
	protected final SimpleContainer personal;
	/** Stacks from an old layout that fit nowhere; dropped next to the chest on the next lazy tick. */
	protected final List<ItemStack> pendingDrops = new java.util.ArrayList<>();
	/** The golem living off this chest; null once it died or never existed (placed by hand). */
	protected @Nullable UUID golem;

	public AndesiteGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		this(type, pos, state, SIZE);
	}

	protected AndesiteGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int storageSize) {
		super(type, pos, state);
		container = notifying(storageSize);
		personal = notifying(GolemChestLayout.PERSONAL_SIZE);
	}

	protected SimpleContainer notifying(int size) {
		return new SimpleContainer(size) {
			@Override
			public void setChanged() {
				super.setChanged();
				AndesiteGolemChestBlockEntity.this.setChanged();
			}
		};
	}

	public SimpleContainer getPersonal() {
		return personal;
	}

	/** Golem kind of this chest, for the maintenance-item table (GolemMaintenance). */
	public String kindId() {
		var kind = com.simibubi.create.content.equipment.golemRevival.GolemKind.ofChest(getBlockState());
		return kind == null ? "" : kind.id;
	}

	public boolean usesPersonally(ItemStack stack) {
		return !stack.isEmpty() && GolemMaintenance.uses(kindId(),
			net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	/** How many of {@code item} the personal slots hold, and how many more they could take. */
	public int personalCount(net.minecraft.world.item.Item item) {
		int n = 0;
		for (int i = 0; i < personal.getContainerSize(); i++)
			if (personal.getItem(i).is(item))
				n += personal.getItem(i).getCount();
		return n;
	}

	public int personalRoom(ItemStack sample) {
		if (!usesPersonally(sample))
			return 0;
		int room = 0, max = Math.min(sample.getMaxStackSize(), personal.getMaxStackSize());
		for (int i = 0; i < personal.getContainerSize(); i++) {
			ItemStack slot = personal.getItem(i);
			if (slot.isEmpty())
				room += max;
			else if (ItemStack.isSameItemSameComponents(slot, sample))
				room += Math.max(0, max - slot.getCount());
		}
		return room;
	}

	/** Puts a maintenance item into the personal slots (merging first). @return what did not fit */
	public ItemStack insertPersonal(ItemStack stack) {
		return usesPersonally(stack) ? personal.addItem(stack) : stack;
	}

	/** One matching item, personal slots first, then storage (auto-repair, saplings keep storage-only takeOne). */
	public ItemStack takeMaintenance(java.util.function.Predicate<ItemStack> predicate) {
		for (int i = 0; i < personal.getContainerSize(); i++) {
			ItemStack stack = personal.getItem(i);
			if (!stack.isEmpty() && predicate.test(stack))
				return personal.removeItem(i, 1);
		}
		return takeOne(predicate);
	}

	/** One matching item from the personal slots only (honey bottles). */
	public ItemStack takePersonal(java.util.function.Predicate<ItemStack> predicate) {
		for (int i = 0; i < personal.getContainerSize(); i++) {
			ItemStack stack = personal.getItem(i);
			if (!stack.isEmpty() && predicate.test(stack))
				return personal.removeItem(i, 1);
		}
		return ItemStack.EMPTY;
	}

	// ---- saving: storage, personal (+ distribution in the Redstone chest), migration of old chests

	protected int storageColumns() {
		return GolemChestLayout.STANDARD_STORAGE_COLUMNS;
	}

	protected int storageOffset() {
		return GolemChestLayout.STANDARD_STORAGE_OFFSET;
	}

	/** Also used by the packed chest item. */
	public void writeInventory(CompoundTag tag, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
		tag.putInt("Layout", LAYOUT_VERSION);
		tag.store("Storage", ItemStack.OPTIONAL_CODEC.listOf(), ops, container.getItems());
		tag.store("Personal", ItemStack.OPTIONAL_CODEC.listOf(), ops, personal.getItems());
		if (!pendingDrops.isEmpty())
			tag.store("PendingDrops", ItemStack.CODEC.listOf(), ops, pendingDrops);
	}

	/** Reads the current layout, or migrates an old 27-slot "Items" list (GolemChestLayout#migrate). */
	public void readInventory(CompoundTag tag, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
		container.clearContent();
		personal.clearContent();
		pendingDrops.clear();
		tag.read("PendingDrops", ItemStack.CODEC.listOf(), ops).ifPresent(pendingDrops::addAll);
		if (tag.getIntOr("Layout", 0) >= LAYOUT_VERSION) {
			fill(container, tag.read("Storage", ItemStack.OPTIONAL_CODEC.listOf(), ops).orElse(List.of()));
			fill(personal, tag.read("Personal", ItemStack.OPTIONAL_CODEC.listOf(), ops).orElse(List.of()));
			return;
		}
		List<ItemStack> legacy = tag.read("Items", ItemStack.OPTIONAL_CODEC.listOf(), ops).orElse(List.of());
		GolemChestLayout.Migration<ItemStack> m = GolemChestLayout.migrate(legacy, ItemStack.EMPTY, ItemStack::isEmpty,
			this::usesPersonally, storageColumns(), storageOffset());
		fill(container, m.storage());
		fill(personal, m.personal());
		pendingDrops.addAll(m.overflow());
	}

	protected static void fill(SimpleContainer target, List<ItemStack> items) {
		for (int i = 0; i < Math.min(items.size(), target.getContainerSize()); i++)
			target.setItem(i, items.get(i));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		// Storage only: personal (and distribution) slots never enter or leave through automation.
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
		flushPendingDrops();
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
			writeInventory(tag, registries.createSerializationContext(NbtOps.INSTANCE));
	}

	/** Migrated stacks that fit nowhere: storage if room appeared, else dropped beside the chest. */
	protected void flushPendingDrops() {
		if (pendingDrops.isEmpty())
			return;
		for (ItemStack stack : pendingDrops) {
			ItemStack rest = container.addItem(stack);
			if (!rest.isEmpty())
				rest = insertPersonal(rest);
			if (!rest.isEmpty())
				Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, rest);
		}
		pendingDrops.clear();
		setChanged();
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		golem = tag.read("Golem", UUIDUtil.CODEC).orElse(null);
		if (clientPacket)
			return;
		readInventory(tag, registries.createSerializationContext(NbtOps.INSTANCE));
	}

	@Override
	public void destroy() {
		// Breaking the home packs the live golem; this is not a death and must not
		// turn the chest into the recyclable, inactive variant.
		AndesiteGolem linked = getGolem();
		if (linked != null)
			linked.discard();
		super.destroy();
		if (level != null) {
			Containers.dropContents(level, worldPosition, container);
			Containers.dropContents(level, worldPosition, personal);
			for (ItemStack stack : pendingDrops)
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
			pendingDrops.clear();
		}
	}

	@Override
	public void clearContent() {
		container.clearContent();
		personal.clearContent();
		pendingDrops.clear();
	}
}
