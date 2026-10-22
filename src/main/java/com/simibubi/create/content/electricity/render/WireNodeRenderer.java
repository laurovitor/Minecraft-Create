package com.simibubi.create.content.electricity.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.electricity.ElectricityClientEvents;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.electricity.energy.IWireNode;
import com.simibubi.create.content.electricity.energy.WireType;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import com.simibubi.create.foundation.render.CachedBufferer;
import com.simibubi.create.foundation.render.RenderTypes;
import com.simibubi.create.foundation.render.SuperByteBufferRenderState;
import com.simibubi.create.infrastructure.config.AllConfigs;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the hanging wires of a wire node (connectors), plus the wire still being placed from a held spool.
 * <p>
 * Ported from Create Crafts &amp; Additions (MIT, by MRH0) onto the extract/submit split: everything is read
 * from the level in {@link #extractSafe} and only vertices are written in {@link #submitSafe}.
 */
public class WireNodeRenderer<T extends BlockEntity> extends SafeBlockEntityRenderer<T, WireNodeRenderer.WireRenderState> {

	private static final float HANG = 0.5f;
	private static final float LIGHT_Y_OFFSET = -0.03f;
	private static final int SEGMENTS = 24;
	private static final Color[] FESTIVE_COLORS = { Color.RED, Color.GREEN, new Color(0f, 0f, 1f, 1f) };

	/** One wire, from the far end ({@code origin}) back towards this node ({@code dx, dy, dz}). */
	public record Wire(float ox, float oy, float oz, float dx, float dy, float dz, WireType type, float length,
		int blockNear, int blockFar, int skyNear, int skyFar, float hangScale) {}

	public static class WireRenderState extends SafeRenderState {
		public final List<Wire> wires = new ArrayList<>();
		public final List<SuperByteBufferRenderState> lights = new ArrayList<>();
	}

	public WireNodeRenderer(BlockEntityRendererProvider.Context context) {}

	@Override
	public WireRenderState createRenderState() {
		return new WireRenderState();
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** The wires reach far past the node, so cull and measure distance by the wires, not by the block. */
	@Override
	public AABB getRenderBoundingBox(T be) {
		IWireNode node = (IWireNode) be;
		BlockPos pos = be.getBlockPos();
		int minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
		int maxX = minX, maxY = minY, maxZ = minZ;
		for (int i = 0; i < node.getNodeCount(); i++) {
			BlockPos other = node.getNodePos(i);
			if (other == null)
				continue;
			minX = Math.min(minX, other.getX());
			minY = Math.min(minY, other.getY());
			minZ = Math.min(minZ, other.getZ());
			maxX = Math.max(maxX, other.getX());
			maxY = Math.max(maxY, other.getY());
			maxZ = Math.max(maxZ, other.getZ());
		}
		// One block of margin covers the sag.
		AABB box = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 2, maxZ + 2);
		LocalPlayer player = Minecraft.getInstance().player;
		if (ElectricityClientEvents.clientRenderHeldWire && player != null) {
			var held = ElectricityUtil.getWireNodeOfSpools(player.getMainHandItem());
			if (held != null && held.a.equals(pos))
				box = box.minmax(player.getBoundingBox());
		}
		return box;
	}

	@Override
	public boolean shouldRender(T be, Vec3 cameraPos) {
		double distance = getViewDistance();
		return getRenderBoundingBox(be).distanceToSqr(cameraPos) < distance * distance;
	}

	@Override
	protected void extractSafe(T be, WireRenderState state, float partialTicks, Vec3 cameraPosition) {
		state.wires.clear();
		state.lights.clear();
		IWireNode node = (IWireNode) be;
		Level level = be.getLevel();
		if (level == null)
			return;
		BlockPos pos = node.getPos();
		float hangScale = HANG / maxWireLength();

		for (int i = 0; i < node.getNodeCount(); i++) {
			if (!node.hasConnection(i))
				continue;
			BlockPos other = node.getNodePos(i);
			WireType type = node.getNodeType(i);
			if (other == null || type == null)
				continue;
			Vec3 d1 = node.getNodeOffset(i);
			// The other end may be outside the client's loaded chunks; aim at its block centre then.
			IWireNode remote = node.getWireNode(i);
			Vec3 d2 = remote != null ? remote.getNodeOffset(node.getOtherNodeIndex(i)) : Vec3.ZERO;

			float tx = other.getX() - pos.getX();
			float ty = other.getY() - pos.getY();
			float tz = other.getZ() - pos.getZ();
			addWire(state, be, level, other, tx + .5f + (float) d2.x, ty + .5f + (float) d2.y, tz + .5f + (float) d2.z,
				-tx - (float) d2.x + (float) d1.x, -ty - (float) d2.y + (float) d1.y, -tz - (float) d2.z + (float) d1.z,
				type, length(tx, ty, tz), hangScale);
		}

		LocalPlayer player = Minecraft.getInstance().player;
		if (!ElectricityClientEvents.clientRenderHeldWire || player == null)
			return;
		var held = ElectricityUtil.getWireNodeOfSpools(player.getMainHandItem());
		if (held == null || !held.a.equals(pos))
			return;
		Vec3 d1 = node.getNodeOffset(held.b);
		Vec3 playerPos = player.getPosition(partialTicks);
		float tx = (float) playerPos.x - pos.getX();
		float ty = (float) playerPos.y - pos.getY();
		float tz = (float) playerPos.z - pos.getZ();
		addWire(state, be, level, player.blockPosition(), tx + .5f, ty + .5f, tz + .5f, -tx + (float) d1.x,
			-ty + (float) d1.y, -tz + (float) d1.z, held.c, length(tx, ty, tz), hangScale);
	}

	private static void addWire(WireRenderState state, BlockEntity be, Level level, BlockPos other, float ox, float oy,
		float oz, float dx, float dy, float dz, WireType type, float length, float hangScale) {
		BlockPos near = be.getBlockPos();
		state.wires.add(new Wire(ox, oy, oz, dx, dy, dz, type, length, level.getBrightness(LightLayer.BLOCK, near),
			level.getBrightness(LightLayer.BLOCK, other), level.getBrightness(LightLayer.SKY, near),
			level.getBrightness(LightLayer.SKY, other), hangScale));

		if (!type.isFestive())
			return;
		boolean main = dx + dy + dz > 0;
		for (int j = 0; j < SEGMENTS; j++) {
			int index = j + 1;
			if (index % 3 != 0 || index == SEGMENTS)
				continue;
			float part = (float) index / SEGMENTS;
			float catenary = dy > 0 ? dy * part * part : dy - dy * (1 - part) * (1 - part);
			float fy = catenary - hang(part, length, hangScale) + LIGHT_Y_OFFSET;
			SuperByteBuffer bulb = CachedBufferer.partial(AllPartialModels.SMALL_LIGHT, be.getBlockState());
			TransformStack.of(bulb.getTransforms())
				.translate(ox + dx * part, oy + fy, oz + dz * part);
			Color color = FESTIVE_COLORS[main ? 2 - (index / 3) % 3 : (index / 3) % 3];
			state.lights.add(SuperByteBufferRenderState.capture(bulb.color(color)
				.light(LightCoordsUtil.FULL_BRIGHT)));
		}
	}

	@Override
	protected void submitSafe(WireRenderState state, PoseStack ms, SubmitNodeCollector queue, CameraRenderState camera) {
		for (Wire wire : state.wires) {
			ms.pushPose();
			ms.translate(wire.ox(), wire.oy(), wire.oz());
			queue.submitCustomGeometry(ms, RenderTypes.wire(), (pose, consumer) -> renderWire(pose.pose(), consumer, wire));
			ms.popPose();
		}
		for (SuperByteBufferRenderState light : state.lights)
			light.submit(ms, net.minecraft.client.renderer.rendertype.RenderTypes.cutoutMovingBlock(), queue);
	}

	private static float maxWireLength() {
		try {
			return Math.max(1, AllConfigs.server().electricity.smallConnectorMaxLength.get());
		} catch (IllegalStateException | NullPointerException e) {
			return 16;
		}
	}

	private static float hang(float part, float length, float hangScale) {
		return (float) Math.sin(part * (float) Math.PI) * hangScale * length;
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	public static void renderWire(Matrix4f matrix, VertexConsumer consumer, Wire wire) {
		float x = wire.dx(), y = wire.dy(), z = wire.dz();
		float f = Mth.invSqrt(x * x + z * z) * 0.025F / 2.0F;
		float o1 = z * f;
		float o2 = x * f;
		wirePart(consumer, matrix, wire, 0.025F, 0.025F, o1, o2);
		wirePart(consumer, matrix, wire, 0.025F, 0.0F, o1, o2);
	}

	private static void wirePart(VertexConsumer consumer, Matrix4f matrix, Wire wire, float a, float b, float o1,
		float o2) {
		for (int j = 0; j < SEGMENTS; ++j) {
			float f = (float) j / (SEGMENTS - 1);
			int block = (int) Mth.lerp(f, (float) wire.blockFar(), (float) wire.blockNear());
			int sky = (int) Mth.lerp(f, (float) wire.skyFar(), (float) wire.skyNear());
			int light = LightCoordsUtil.pack(block, sky);
			wireVertex(consumer, matrix, light, wire, a, b, j, false, o1, o2);
			wireVertex(consumer, matrix, light, wire, a, b, j + 1, true, o1, o2);
		}
	}

	private static void wireVertex(VertexConsumer consumer, Matrix4f matrix, int light, Wire wire, float a, float b,
		int index, boolean second, float o1, float o2) {
		WireType type = wire.type();
		int cr = type.getRed();
		int cg = type.getGreen();
		int cb = type.getBlue();
		if (index % 2 == 0) {
			cr *= 0.7F;
			cg *= 0.7F;
			cb *= 0.7F;
		}

		float x = wire.dx(), y = wire.dy(), z = wire.dz();
		float part = (float) index / SEGMENTS;
		float fx = x * part;
		float fy = (y > 0 ? y * part * part : y - y * (1 - part) * (1 - part)) - hang(part, wire.length(), wire.hangScale());
		float fz = z * part;

		if (Math.abs(x) + Math.abs(z) < Math.abs(y)) {
			// Nearly vertical: give the wire its width along Z instead.
			boolean p = b > 0;
			float c = 0.015f;
			if (!second)
				consumer.addVertex(matrix, fx - c, fy, fz + (p ? -c : c)).setColor(cr, cg, cb, 255).setLight(light);
			consumer.addVertex(matrix, fx + c, fy, fz + (p ? c : -c)).setColor(cr, cg, cb, 255).setLight(light);
			if (second)
				consumer.addVertex(matrix, fx - c, fy, fz + (p ? -c : c)).setColor(cr, cg, cb, 255).setLight(light);
			return;
		}

		if (!second)
			consumer.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
		consumer.addVertex(matrix, fx - o1, fy + b, fz + o2).setColor(cr, cg, cb, 255).setLight(light);
		if (second)
			consumer.addVertex(matrix, fx + o1, fy + a - b, fz - o2).setColor(cr, cg, cb, 255).setLight(light);
	}
}
