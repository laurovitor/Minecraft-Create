package com.simibubi.create.content.equipment.extendoGrip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllPartialModels;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ExtendoGripRenderHandler {

	public static float mainHandAnimation;
	public static float lastMainHandAnimation;
	public static PartialModel pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;

	private static final ItemStackRenderState SCRATCH_ITEM = new ItemStackRenderState();

	public static void tick() {
		lastMainHandAnimation = mainHandAnimation;
		mainHandAnimation *= Mth.clamp(mainHandAnimation, 0.8f, 0.99f);

		pose = AllPartialModels.DEPLOYER_HAND_PUNCHING;
		if (!AllItems.EXTENDO_GRIP.isIn(getRenderedOffHandStack()))
			return;
		ItemStack main = getRenderedMainHandStack();
		if (main.isEmpty())
			return;
		if (!(main.getItem() instanceof BlockItem))
			return;
		Minecraft.getInstance()
			.getItemModelResolver()
			.updateForTopItem(SCRATCH_ITEM, main, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, null, null, 0);
		if (!SCRATCH_ITEM.usesBlockLight())
			return;
		pose = AllPartialModels.DEPLOYER_HAND_HOLDING;
	}

	@SubscribeEvent
	public static void onRenderPlayerHand(RenderHandEvent event) {
		ItemStack heldItem = event.getItemStack();
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null)
			return;
		boolean rightHand = event.getHand() == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;

		ItemStack offhandItem = event.getPlayerState().firstPersonHandsAndItems.offHandItem;
		boolean notInOffhand = !AllItems.EXTENDO_GRIP.isIn(offhandItem);
		if (notInOffhand && !AllItems.EXTENDO_GRIP.isIn(heldItem))
			return;

		PoseStack ms = event.getPoseStack();
		var msr = TransformStack.of(ms);

		float flip = rightHand ? 1.0F : -1.0F;
		float swingProgress = event.getSwingProgress();
		boolean blockItem = heldItem.getItem() instanceof BlockItem;
		float equipProgress = blockItem ? 0 : event.getEquipProgress() / 4;

		ms.pushPose();
		if (event.getHand() == InteractionHand.MAIN_HAND) {

			if (1 - swingProgress > mainHandAnimation && swingProgress > 0)
				mainHandAnimation = 0.95f;
			ms.translate(flip * (0.64000005F - .1f), -0.4F + equipProgress * -0.6F, -0.71999997F + .3f);

			ms.pushPose();
			msr.rotateYDegrees(flip * 75.0F);
			ms.translate(flip * -1.0F, 3.6F, 3.5F);
			msr.rotateZDegrees(flip * 120)
				.rotateXDegrees(200)
				.rotateYDegrees(flip * -135.0F);
			ms.translate(flip * 5.6F, 0.0F, 0.0F);
			msr.rotateYDegrees(flip * 40.0F);
			ms.translate(flip * 0.05f, -0.3f, -0.3f);

			AvatarRenderer<?> playerrenderer = mc.getEntityRenderDispatcher()
				.getRenderer(event.getPlayerState().avatarRenderState);
			Identifier skin = event.getPlayerState().avatarRenderState.skin
				.body()
				.texturePath();
			if (rightHand)
				playerrenderer.renderRightHand(ms, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
					event.getPlayerState().avatarRenderState.showRightSleeve);
			else
				playerrenderer.renderLeftHand(ms, event.getSubmitNodeCollector(), event.getPackedLight(), skin,
					event.getPlayerState().avatarRenderState.showLeftSleeve);
			ms.popPose();

			// Render gun
			ms.pushPose();
			ms.translate(flip * -0.1f, 0, -0.3f);
			ItemDisplayContext transform =
				rightHand ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			ItemStackRenderState itemState = new ItemStackRenderState();
			mc.getItemModelResolver().updateForTopItem(itemState, notInOffhand ? heldItem : offhandItem,
				transform, player.level(), player, 0);
			itemState.submit(ms, event.getSubmitNodeCollector(), event.getPackedLight(),
				OverlayTexture.NO_OVERLAY, 0);

			ms.popPose();
		}
		ms.popPose();
		event.setCanceled(true);
	}

	private static ItemStack getRenderedMainHandStack() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? ItemStack.EMPTY : player.getMainHandItem();
	}

	private static ItemStack getRenderedOffHandStack() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? ItemStack.EMPTY : player.getOffhandItem();
	}

}
