package com.simibubi.create.content.equipment.netheriteGolem;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import org.jetbrains.annotations.Nullable;

/** Distinct registry chest sharing the proven 27-slot/filter/logistics implementation. */
public class NetheriteGolemChestBlockEntity extends AndesiteGolemChestBlockEntity {
	public NetheriteGolemChestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Item.BLOCK, AllBlockEntityTypes.NETHERITE_GOLEM_CHEST.get(),
			(be, context) -> VanillaContainerWrapper.of(be.getContainer()));
	}
	@Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		behaviours.add(filtering = new FilteringBehaviour(this, new NetheriteGolemChestFilterSlot()));
	}
	@Override public boolean accepts(ItemStack stack) {
		return filtering == null || NetheriteGolemRules.acceptsLoot(filtering.getFilter().isEmpty(), filtering.test(stack));
	}
	@Override public @Nullable NetheriteGolem getGolem() {
		if (golem == null || !(level instanceof ServerLevel server)) return null;
		return server.getEntity(golem) instanceof NetheriteGolem entity ? entity : null;
	}
	@Override public void releaseGolem() {
		golem = null;
		BlockState state = getBlockState();
		if (state.getBlock() instanceof NetheriteGolemChestBlock && state.getValue(NetheriteGolemChestBlock.ACTIVE))
			level.setBlockAndUpdate(worldPosition, state.setValue(NetheriteGolemChestBlock.ACTIVE, false));
		notifyUpdate();
	}
}
