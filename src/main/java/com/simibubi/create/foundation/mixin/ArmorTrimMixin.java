package com.simibubi.create.foundation.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.Create;

import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

/**
 * The 26.3 renderer resolves trim textures from its equipment-info and pattern pair, not from
 * ArmorTrim.layerAssetId (which no longer exists). Keep the cardboard pattern in Create's texture
 * namespace while leaving the material's palette ID for the game's dynamic recoloring path.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer$TrimTextureKey")
public abstract class ArmorTrimMixin {
	@Shadow @Final private ArmorTrim trim;
	@Shadow @Final private EquipmentClientInfo.LayerType layerType;
	@Shadow @Final private EquipmentClientInfo equipmentInfo;

	@ModifyExpressionValue(method = "getOrPrepareTexture", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/resources/Identifier;withPath(Ljava/util/function/UnaryOperator;)Lnet/minecraft/resources/Identifier;"))
	private Identifier create$useCardboardTrimTexture(Identifier original) {
		// The key carries the resolved asset info, not the equipment asset key. Match Create's
		// cardboard layer texture, preserving the ordinary trim path for every other asset.
		boolean cardboard = equipmentInfo.getLayers(layerType)
			.stream()
			.anyMatch(layer -> layer.textureId().equals(Create.asResource("cardboard")));
		if (!cardboard)
			return original;

		String pattern = trim.pattern().value().assetId().getPath();
		String leggings = layerType == EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS ? "_leggings" : "";
		return Create.asResource("trims/models/armor/card_" + pattern + leggings);
	}
}
