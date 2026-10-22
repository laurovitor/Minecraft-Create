package com.simibubi.create.content.electricity.pump;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.SuperByteBufferRenderState;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the two pulsing bellows of the Electric Pump; the static body comes from the block model.
 * Ported from Create Crafts &amp; Additions (MIT, by MRH0).
 */
public class ElectricPumpRenderer
	extends SafeBlockEntityRenderer<ElectricPumpBlockEntity, ElectricPumpRenderer.ElectricPumpRenderState> {

	public static class ElectricPumpRenderState extends SafeRenderState {
		public final List<SuperByteBufferRenderState> parts = new ArrayList<>();
	}

	public ElectricPumpRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public ElectricPumpRenderState createRenderState() {
		return new ElectricPumpRenderState();
	}

	@Override
	protected void extractSafe(ElectricPumpBlockEntity be, ElectricPumpRenderState state, float partialTicks,
		Vec3 cameraPosition) {
		state.parts.clear();
		BlockState blockState = be.getBlockState();
		Direction facing = blockState.getValue(ElectricPumpBlock.FACING);
		float renderTime = AnimationTickHolder.getRenderTime();

		for (int i = 0; i < ElectricPumpBlockEntity.PARTIAL_OFFSETS.length; i++) {
			float[] offset = ElectricPumpBlockEntity.PARTIAL_OFFSETS[i];
			float scale = ElectricPumpBlockEntity.getPulseScale(be.isActive(), be.getPumpSpeed(), renderTime,
				ElectricPumpBlockEntity.PARTIAL_PHASE_OFFSETS[i]);
			SuperByteBuffer part = CachedBufferer.partial(AllPartialModels.ELECTRIC_PUMP_PARTIAL, blockState);
			TransformStack.of(part.getTransforms())
				.rotateCentered(ElectricPumpBlockEntity.getFacingRotation(facing))
				.translate(offset[0], offset[1], offset[2])
				.scale(scale);
			state.parts.add(SuperByteBufferRenderState.capture(part.light(state.lightCoords)));
		}
	}

	@Override
	protected void submitSafe(ElectricPumpRenderState state, PoseStack ms, SubmitNodeCollector queue,
		CameraRenderState camera) {
		for (SuperByteBufferRenderState part : state.parts)
			part.submit(ms, RenderTypes.cutoutMovingBlock(), queue);
	}
}
