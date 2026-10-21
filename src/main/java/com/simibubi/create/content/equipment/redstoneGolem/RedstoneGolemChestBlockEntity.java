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

	public RedstoneGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.REDSTONE_GOLEM_CHEST.get(),
			(be, context) -> VanillaContainerWrapper.of(be.getContainer()));
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(link = new LogisticallyLinkedBehaviour(this, false));
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
