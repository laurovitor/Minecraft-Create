package com.simibubi.create.content.equipment.redstoneGolem;

import com.simibubi.create.Create;

import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** The copper golem's model and animations, in redstone colours. It never sleeps: eyes always lit. */
public class RedstoneGolemRenderer extends MobRenderer<RedstoneGolem, CopperGolemRenderState, CopperGolemModel> {

	private static final Identifier TEXTURE = Create.asResource("textures/entity/redstone_golem/redstone_golem.png");
	private static final Identifier EYES = Create.asResource("textures/entity/redstone_golem/redstone_golem_eyes.png");

	public RedstoneGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
		addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, ageInTicks) -> 1.0F,
			new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), RenderTypes::eyes, false));
		addLayer(new RaisedItemLayer(this));
	}

	/**
	 * The carried cardboard box, lifted: the short golem's hand sits so low that the block-sized box
	 * dipped into the ground while walking. Model space Y points down, so the lift is negative.
	 */
	static class RaisedItemLayer extends ItemInHandLayer<CopperGolemRenderState, CopperGolemModel> {
		/** Lift, blocks (6 px); raise further if the box still touches the ground. */
		static final float LIFT = 6 / 16f;

		RaisedItemLayer(RedstoneGolemRenderer renderer) {
			super(renderer);
		}

		@Override
		public void submit(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector,
			int light, CopperGolemRenderState state, float yRot, float xRot) {
			poseStack.pushPose();
			poseStack.translate(0, -LIFT, 0);
			super.submit(poseStack, collector, light, state, yRot, xRot);
			poseStack.popPose();
		}
	}

	@Override
	public Identifier getTextureLocation(CopperGolemRenderState state) {
		return TEXTURE;
	}

	@Override
	public CopperGolemRenderState createRenderState() {
		return new CopperGolemRenderState();
	}

	@Override
	public void extractRenderState(RedstoneGolem entity, CopperGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
		state.copperGolemState = entity.getState();
		state.idleAnimationState.copyFrom(entity.getIdleAnimationState());
		state.interactionGetNoItem.copyFrom(entity.getWorkAnimationState());
		state.interactionDropItem.copyFrom(entity.getDepositAnimationState());
		state.blockOnAntenna.clear();
	}
}
