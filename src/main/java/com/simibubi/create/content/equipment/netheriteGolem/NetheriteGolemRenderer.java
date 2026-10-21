package com.simibubi.create.content.equipment.netheriteGolem;

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

/** The copper golem's model and animations, in andesite alloy colours. */
public class NetheriteGolemRenderer extends MobRenderer<NetheriteGolem, CopperGolemRenderState, CopperGolemModel> {

	private static final Identifier TEXTURE = Create.asResource("textures/entity/netherite_golem/netherite_golem.png");
	private static final Identifier EYES = Create.asResource("textures/entity/netherite_golem/netherite_golem_eyes.png");

	public NetheriteGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new SleepyModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
		// eyes go dark while it sleeps
		addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES,
			(state, ageInTicks) -> state instanceof SleepyState sleepy && sleepy.sleeping ? 0.0F : 1.0F,
			new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), RenderTypes::eyes, false));
		addLayer(new ItemInHandLayer<>(this));
	}

	@Override
	public Identifier getTextureLocation(CopperGolemRenderState state) {
		return TEXTURE;
	}

	@Override
	public CopperGolemRenderState createRenderState() {
		return new SleepyState();
	}

	@Override
	public void extractRenderState(NetheriteGolem entity, CopperGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
		state.copperGolemState = entity.getState();
		state.idleAnimationState.copyFrom(entity.getIdleAnimationState());
		state.interactionGetNoItem.copyFrom(entity.getWorkAnimationState());
		state.interactionDropItem.copyFrom(entity.getDepositAnimationState());
		state.blockOnAntenna.clear();
		if (state instanceof SleepyState sleepy)
			sleepy.sleeping = entity.isAsleep();
	}

	public static class SleepyState extends CopperGolemRenderState {
		public boolean sleeping;
	}

	/** The copper golem model, with the head dropped forward while asleep. */
	public static class SleepyModel extends CopperGolemModel {
		public SleepyModel(net.minecraft.client.model.geom.ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(CopperGolemRenderState state) {
			super.setupAnim(state);
			if (state instanceof SleepyState sleepy && sleepy.sleeping) {
				getHead().xRot = 0.6F;
				getHead().yRot = 0;
			}
		}
	}
}
