package com.simibubi.create.content.equipment.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.mixin.accessor.EntityRenderDispatcherAccessor;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBufferRenderState;
import dev.engine_room.flywheel.lib.transform.TransformStack;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.CachedBuffers;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.math.AngleHelper;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Draws a worn backtank on its wearer's back.
 * <p>
 * A render layer works off the entity's render state in 26.3 rather than the entity itself, which
 * suits this: the worn chest item is already extracted for us, and the rest of what the backtank
 * draws is cached geometry.
 */
public class BacktankArmorLayer<S extends HumanoidRenderState, M extends HumanoidModel<? super S>>
	extends RenderLayer<S, M> {

	public BacktankArmorLayer(RenderLayerParent<S, M> renderer) {
		super(renderer);
	}

	@Override
	public void submit(PoseStack ms, SubmitNodeCollector queue, int light, S state, float yRot, float xRot) {
		if (state.pose == Pose.SLEEPING)
			return;
		if (!(state.chestEquipment.getItem() instanceof BacktankItem item))
			return;

		BlockState renderedState = item.getBlock()
			.defaultBlockState()
			.setValue(BacktankBlock.HORIZONTAL_FACING, Direction.SOUTH);

		SuperByteBuffer backtankBuffer = CachedBuffers.block(renderedState).disableDiffuse().light(light);
		SuperByteBufferRenderState backtank = SuperByteBufferRenderState.capture(backtankBuffer);

		SuperByteBuffer nobBuffer = CachedBufferer.partial(BacktankRenderer.getShaftModel(renderedState), renderedState);
		TransformStack.of(nobBuffer.getTransforms()).translate(0, -3f / 16, 0);
		SuperByteBufferRenderState nob = SuperByteBufferRenderState.capture(nobBuffer.disableDiffuse().light(light));

		SuperByteBuffer cogsBuffer = CachedBufferer.partial(BacktankRenderer.getCogsModel(renderedState), renderedState);
		TransformStack.of(cogsBuffer.getTransforms())
			.center()
			.rotateYDegrees(180)
			.uncenter()
			.translate(0, 6.5f / 16, 11f / 16)
			.rotate(AngleHelper.rad(2 * AnimationTickHolder.getRenderTime() % 360), Direction.EAST)
			.translate(0, -6.5f / 16, -11f / 16);
		SuperByteBufferRenderState cogs = SuperByteBufferRenderState.capture(cogsBuffer.disableDiffuse().light(light));

		ms.pushPose();

		getParentModel().body.translateAndRotate(ms);
		ms.translate(-1 / 2f, 10 / 16f, 1f);
		ms.scale(1, -1, -1);

		RenderType renderType = RenderTypes.cutoutMovingBlock();
		backtank.submit(ms, renderType, queue);
		nob.submit(ms, renderType, queue);
		cogs.submit(ms, renderType, queue);

		// The foil is a second pass over the same geometry rather than a wrapped vertex consumer.
		if (state.chestEquipment.hasFoil()) {
			RenderType glint = RenderTypes.entitySolidGlint(TextureAtlas.LOCATION_BLOCKS);
			backtank.submit(ms, glint, queue);
			nob.submit(ms, glint, queue);
			cogs.submit(ms, glint, queue);
		}

		ms.popPose();
	}

	public static void registerOnAll(EntityRenderDispatcher renderManager) {
		for (EntityRenderer<? extends Player, ?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getPlayerRenderers().values())
			registerOn(renderer);
		for (EntityRenderer<?, ?> renderer : ((EntityRenderDispatcherAccessor) renderManager).create$getRenderers().values())
			registerOn(renderer);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void registerOn(EntityRenderer<?, ?> entityRenderer) {
		if (!(entityRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer))
			return;
		if (!(livingRenderer.getModel() instanceof HumanoidModel))
			return;
		BacktankArmorLayer<?, ?> layer = new BacktankArmorLayer<>((RenderLayerParent) livingRenderer);
		livingRenderer.addLayer((RenderLayer) layer);
	}

}
