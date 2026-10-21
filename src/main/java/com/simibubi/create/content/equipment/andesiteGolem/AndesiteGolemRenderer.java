package com.simibubi.create.content.equipment.andesiteGolem;

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
public class AndesiteGolemRenderer extends MobRenderer<AndesiteGolem, CopperGolemRenderState, CopperGolemModel> {

	private static final Identifier TEXTURE = Create.asResource("textures/entity/andesite_golem/andesite_golem.png");
	private static final Identifier EYES = Create.asResource("textures/entity/andesite_golem/andesite_golem_eyes.png");

	public AndesiteGolemRenderer(EntityRendererProvider.Context context) {
		super(context, new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), 0.5F);
		addLayer(new LivingEntityEmissiveLayer<>(this, state -> EYES, (state, ageInTicks) -> 1.0F,
			new CopperGolemModel(context.bakeLayer(ModelLayers.COPPER_GOLEM)), RenderTypes::eyes, false));
		addLayer(new ItemInHandLayer<>(this));
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
	public void extractRenderState(AndesiteGolem entity, CopperGolemRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
		state.copperGolemState = entity.getState();
		state.idleAnimationState.copyFrom(entity.getIdleAnimationState());
		state.interactionGetNoItem.copyFrom(entity.getWorkAnimationState());
		state.interactionDropItem.copyFrom(entity.getDepositAnimationState());
		state.blockOnAntenna.clear();
	}
}
