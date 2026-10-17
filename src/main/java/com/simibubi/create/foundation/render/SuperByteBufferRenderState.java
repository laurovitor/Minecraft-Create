package com.simibubi.create.foundation.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.createmod.catnip.api.client.render.SuperByteBuffer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector.CustomGeometryRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.ARGB;

/**
 * Immutable vertex snapshot used to carry a {@link SuperByteBuffer} from render-state extraction to
 * Minecraft's deferred 26.3 submit pass.
 */
public final class SuperByteBufferRenderState implements CustomGeometryRenderer {

	private final List<Vertex> vertices;

	private SuperByteBufferRenderState(List<Vertex> vertices) {
		this.vertices = vertices;
	}

	/**
	 * Resolves the buffer immediately because Catnip's cached buffers are mutable and reset after
	 * rendering. The resulting state can safely be submitted later without retaining the cache entry.
	 */
	public static SuperByteBufferRenderState capture(SuperByteBuffer buffer) {
		CaptureConsumer capture = new CaptureConsumer();
		buffer.renderInto(new PoseStack(), capture);
		return new SuperByteBufferRenderState(List.copyOf(capture.vertices));
	}

	public void submit(PoseStack matrices, RenderType renderType, OrderedSubmitNodeCollector queue) {
		if (!vertices.isEmpty())
			queue.submitCustomGeometry(matrices, renderType, this);
	}

	public boolean isEmpty() {
		return vertices.isEmpty();
	}

	@Override
	public void render(PoseStack.Pose pose, VertexConsumer consumer) {
		Vector4f position = new Vector4f();
		Vector3f normal = new Vector3f();
		for (Vertex vertex : vertices) {
			position.set(vertex.x, vertex.y, vertex.z, 1);
			position.mul(pose.pose());
			pose.transformNormal(vertex.normalX, vertex.normalY, vertex.normalZ, normal);
			consumer.addVertex(position.x(), position.y(), position.z(), vertex.color, vertex.u, vertex.v,
				vertex.overlay, vertex.light, normal.x(), normal.y(), normal.z());
		}
	}

	private static final class CaptureConsumer implements VertexConsumer {
		private final List<Vertex> vertices = new ArrayList<>();
		private Vertex current;

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			current = new Vertex(x, y, z);
			vertices.add(current);
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			current.color = ARGB.color(alpha, red, green, blue);
			return this;
		}

		@Override
		public VertexConsumer setColor(int color) {
			current.color = color;
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			current.u = u;
			current.v = v;
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			current.overlay = u | v << 16;
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			current.light = u | v << 16;
			return this;
		}

		@Override
		public VertexConsumer setUv3(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			current.normalX = x;
			current.normalY = y;
			current.normalZ = z;
			return this;
		}

		@Override
		public VertexConsumer setLineWidth(float width) {
			return this;
		}
	}

	private static final class Vertex {
		private final float x;
		private final float y;
		private final float z;
		private int color = -1;
		private float u;
		private float v;
		private int overlay;
		private int light;
		private float normalX;
		private float normalY;
		private float normalZ;

		private Vertex(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}
}
