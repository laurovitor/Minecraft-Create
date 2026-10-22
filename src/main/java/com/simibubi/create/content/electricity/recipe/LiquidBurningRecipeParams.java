package com.simibubi.create.content.electricity.recipe;


import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

public class LiquidBurningRecipeParams extends ProcessingRecipeParams {
    public static MapCodec<LiquidBurningRecipeParams> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            codec(LiquidBurningRecipeParams::new).forGetter(Function.identity()),
            Codec.INT.fieldOf("burn_time").forGetter(LiquidBurningRecipeParams::getBurnTime),
            Codec.BOOL.optionalFieldOf("superheated", false).forGetter(LiquidBurningRecipeParams::isSuperheated)
    ).apply(instance, (params, burnTime, superheated) -> {
        params.burnTime = burnTime;
        params.superheated = superheated;
        return params;
    }));
    public static StreamCodec<RegistryFriendlyByteBuf, LiquidBurningRecipeParams> STREAM_CODEC = streamCodec(LiquidBurningRecipeParams::new);

    int burnTime;
    boolean superheated;

    public int getBurnTime() {
        return burnTime;
    }

    public boolean isSuperheated() {
        return superheated;
    }

    @Override
    protected void encode(RegistryFriendlyByteBuf buffer) {
        super.encode(buffer);
        ByteBufCodecs.INT.encode(buffer, burnTime);
        ByteBufCodecs.BOOL.encode(buffer, superheated);
    }

    @Override
    protected void decode(RegistryFriendlyByteBuf buffer) {
        super.decode(buffer);
        burnTime = ByteBufCodecs.INT.decode(buffer);
        superheated = ByteBufCodecs.BOOL.decode(buffer);
    }
}
