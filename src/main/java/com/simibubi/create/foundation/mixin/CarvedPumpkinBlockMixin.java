package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemSpawning;
import com.simibubi.create.content.equipment.netheriteGolem.NetheriteGolemSpawning;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;

/** Lauro edition: carved pumpkin on an andesite alloy block makes an Andesite Golem. */
@Mixin(CarvedPumpkinBlock.class)
public class CarvedPumpkinBlockMixin {

	@Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
	private void create$spawnAndesiteGolem(Level level, BlockPos topPos, CallbackInfo ci) {
		if (NetheriteGolemSpawning.trySpawn(level, topPos) || AndesiteGolemSpawning.trySpawn(level, topPos))
			ci.cancel();
	}

	// lets dispensers place a pumpkin onto an andesite alloy block, as they do for the other golems
	@Inject(method = "canSpawnGolem", at = @At("RETURN"), cancellable = true)
	private void create$canSpawnAndesiteGolem(LevelReader level, BlockPos topPos, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && (AllBlocks.ANDESITE_ALLOY_BLOCK.has(level.getBlockState(topPos.below())) || level.getBlockState(topPos.below()).is(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK)))
			cir.setReturnValue(true);
	}
}
