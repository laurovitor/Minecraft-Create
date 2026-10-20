package com.simibubi.create.foundation.mixin.accessor;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.core.Holder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BlockLootSubProvider.class)
public interface BlockLootSubProviderAccessor {
	@Invoker("hasSilkTouch")
	Holder<LootItemCondition> create$hasSilkTouch();
}
