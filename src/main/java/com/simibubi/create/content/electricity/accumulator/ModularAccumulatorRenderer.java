package com.simibubi.create.content.electricity.accumulator;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperByteBufferRenderState;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.data.Iterate;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Draws the charge gauges on the four sides of an accumulator's top row. */
public class ModularAccumulatorRenderer
	extends SafeBlockEntityRenderer<ModularAccumulatorBlockEntity, ModularAccumulatorRenderer.AccumulatorRenderState> {

	public static class AccumulatorRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> gauges = new ArrayList<>();
		public int width = 1;
		public int height = 1;
	}

	public ModularAccumulatorRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public AccumulatorRenderState createRenderState() {
		return new AccumulatorRenderState();
	}

	@Override
	protected void extractSafe(ModularAccumulatorBlockEntity be, AccumulatorRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.gauges.clear();
		if (!be.isController())
			return;
		be.observe();

		BlockState blockState = be.getBlockState();
		state.width = be.width;
		state.height = be.height;
		float dialPivotY = 6f / 16f;
		float dialPivotZ = 8f / 16f;
		float progress = be.gauge.getValue(partialTicks);

		for (Direction d : Iterate.horizontalDirections) {
			SuperByteBuffer gauge = CachedBufferer.partial(AllPartialModels.ACCUMULATOR_GAUGE, blockState);
			TransformStack.of(gauge.getTransforms())
				.rotateYDegrees(d.toYRot())
				.uncenter()
				.translate(be.width / 2f - 6 / 16f, 0, 0);
			state.gauges.add(SuperByteBufferRenderState.capture(gauge.light(state.lightCoords)));

			SuperByteBuffer dial = CachedBufferer.partial(AllPartialModels.ACCUMULATOR_DIAL, blockState);
			TransformStack.of(dial.getTransforms())
				.rotateYDegrees(d.toYRot())
				.uncenter()
				.translate(be.width / 2f - 6 / 16f, 0, 0)
				.translate(0, dialPivotY, dialPivotZ)
				.rotateXDegrees(-180 * progress)
				.translate(0, -dialPivotY, -dialPivotZ);
			state.gauges.add(SuperByteBufferRenderState.capture(dial.light(state.lightCoords)));
		}
	}

	@Override
	protected void submitSafe(AccumulatorRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		if (state.gauges.isEmpty())
			return;
		ms.pushPose();
		TransformStack.of(ms)
			.translate(state.width / 2f, state.height - 0.5f, state.width / 2f);
		for (SuperByteBufferRenderState gauge : state.gauges)
			gauge.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		ms.popPose();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}
}
