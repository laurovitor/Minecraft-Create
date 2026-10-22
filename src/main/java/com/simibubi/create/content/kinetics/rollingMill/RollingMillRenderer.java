package com.simibubi.create.content.kinetics.rollingMill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperByteBufferRenderState;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

/**
 * Draws the mill's two counter-rotating rollers when Flywheel is not available; with Flywheel,
 * {@link RollingMillVisual} draws them instead.
 */
public class RollingMillRenderer
	extends KineticBlockEntityRenderer<RollingMillBlockEntity, RollingMillRenderer.RollingMillRenderState> {

	public static class RollingMillRenderState extends KineticRenderState {
		public @Nullable SuperByteBufferRenderState lowerRoller;
		public @Nullable SuperByteBufferRenderState upperRoller;
	}

	public RollingMillRenderer(BlockEntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public RollingMillRenderState createRenderState() {
		return new RollingMillRenderState();
	}

	@Override
	protected void extractSafe(RollingMillBlockEntity be, RollingMillRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		super.extractSafe(be, state, partialTicks, cameraPosition);
		state.lowerRoller = null;
		state.upperRoller = null;
		if (VisualizationManager.supportsVisualization(be.getLevel()))
			return;

		BlockState blockState = be.getBlockState();
		BlockPos pos = be.getBlockPos();
		Axis axis = getRotationAxisOf(be);
		float angle = getAngleForBe(be, pos, axis);
		float yaw = axis == Axis.Z ? 0 : (float) Math.PI / 2;

		SuperByteBuffer lower = CachedBufferer.partial(AllPartialModels.SHAFT_HALF, blockState);
		TransformStack.of(lower.getTransforms())
			.rotateCentered(yaw, Direction.UP)
			.translate(0, 4f / 16f, 0)
			.rotateCentered(angle, Direction.NORTH);
		state.lowerRoller = SuperByteBufferRenderState.capture(lower.light(state.lightCoords));

		SuperByteBuffer upper = CachedBufferer.partial(AllPartialModels.SHAFT_HALF, blockState);
		TransformStack.of(upper.getTransforms())
			.rotateCentered(yaw + (float) Math.PI, Direction.UP)
			.translate(0, 4f / 16f, 0)
			.rotateCentered(-angle, Direction.NORTH);
		state.upperRoller = SuperByteBufferRenderState.capture(upper.light(state.lightCoords));
	}

	@Override
	protected void submitSafe(RollingMillRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		super.submitSafe(state, ms, queue, camera);
		if (state.lowerRoller != null)
			state.lowerRoller.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
		if (state.upperRoller != null)
			state.upperRoller.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}

	@Override
	protected BlockState getRenderedBlockState(RollingMillBlockEntity be) {
		return shaft(getRotationAxisOf(be));
	}

}
