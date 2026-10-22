package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/**
 * Lauro edition: home of a Redstone Golem. The proven 27-slot golem chest, with no filter (it
 * stores everything) and a logistics link whose frequency is the golem's network.
 */
public class RedstoneGolemChestBlockEntity extends AndesiteGolemChestBlockEntity {

	/** Not global: the chest is the network's root, not a Stock Link registered with Create's logistics. */
	public LogisticallyLinkedBehaviour link;

	/** Storage slots of the Redstone chest (6 columns x 3 rows, GolemChestLayout). */
	public static final int SIZE = com.simibubi.create.content.equipment.andesiteGolem.GolemChestLayout.ROWS
		* com.simibubi.create.content.equipment.andesiteGolem.GolemChestLayout.REDSTONE_STORAGE_COLUMNS;

	/** Maintenance items the golem shares out to the golems of its network (never automation). */
	protected final net.minecraft.world.SimpleContainer distribution;

	public RedstoneGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state, SIZE);
		distribution = notifying(com.simibubi.create.content.equipment.andesiteGolem.GolemChestLayout.DISTRIBUTION_SIZE);
	}

	public net.minecraft.world.SimpleContainer getDistribution() {
		return distribution;
	}

	public static boolean distributable(ItemStack stack) {
		return !stack.isEmpty() && com.simibubi.create.content.equipment.andesiteGolem.GolemMaintenance.anyUses(
			net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
	}

	/** Undelivered items come back here; @return what did not fit */
	public ItemStack returnToDistribution(ItemStack stack) {
		return distributable(stack) ? distribution.addItem(stack) : stack;
	}

	@Override
	protected int storageColumns() {
		return com.simibubi.create.content.equipment.andesiteGolem.GolemChestLayout.REDSTONE_STORAGE_COLUMNS;
	}

	@Override
	protected int storageOffset() {
		return com.simibubi.create.content.equipment.andesiteGolem.GolemChestLayout.REDSTONE_STORAGE_OFFSET;
	}

	@Override
	public void writeInventory(net.minecraft.nbt.CompoundTag tag, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
		super.writeInventory(tag, ops);
		tag.store("Distribution", ItemStack.OPTIONAL_CODEC.listOf(), ops, distribution.getItems());
	}

	@Override
	public void readInventory(net.minecraft.nbt.CompoundTag tag, com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
		super.readInventory(tag, ops);
		distribution.clearContent();
		fill(distribution, tag.read("Distribution", ItemStack.OPTIONAL_CODEC.listOf(), ops).orElse(java.util.List.of()));
	}

	@Override
	public void destroy() {
		super.destroy();
		if (level != null)
			net.minecraft.world.Containers.dropContents(level, worldPosition, distribution);
	}

	@Override
	public void clearContent() {
		super.clearContent();
		distribution.clearContent();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.REDSTONE_GOLEM_CHEST.get(),
			(be, context) -> VanillaContainerWrapper.of(be.getContainer()));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(link = new LogisticallyLinkedBehaviour(this, false));
	}

	/** Bumped on every change of the chest (contents or state): the golem re-plans at once. */
	private int contentVersion;

	public int getContentVersion() {
		return contentVersion;
	}

	@Override
	public void setChanged() {
		super.setChanged();
		contentVersion++;
	}

	/** A full set of slots can still accept more of an existing stack. */
	public boolean hasRoomFor(ItemStack incoming) {
		if (incoming.isEmpty()) return false;
		var inventory = getContainer();
		return RedstoneGolemRules.hasRoomFor(inventory.getContainerSize(),
			slot -> inventory.getItem(slot).isEmpty(),
			slot -> {
				ItemStack stored = inventory.getItem(slot);
				return ItemStack.isSameItemSameComponents(stored, incoming)
					&& stored.getCount() < Math.min(stored.getMaxStackSize(), inventory.getMaxStackSize(incoming));
			});
	}

	/** No filter: everything is accepted. */
	@Override
	public boolean accepts(ItemStack stack) {
		return true;
	}

	@Override
	public @Nullable RedstoneGolem getGolem() {
		if (golem == null || !(level instanceof ServerLevel server))
			return null;
		return server.getEntity(golem) instanceof RedstoneGolem entity ? entity : null;
	}

	@Override
	public void releaseGolem() {
		golem = null;
		BlockState state = getBlockState();
		if (state.getBlock() instanceof RedstoneGolemChestBlock && state.getValue(RedstoneGolemChestBlock.ACTIVE))
			level.setBlockAndUpdate(worldPosition, state.setValue(RedstoneGolemChestBlock.ACTIVE, false));
		notifyUpdate();
	}
}
