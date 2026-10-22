package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** Rolling Mill with its two counter-rotating rollers, for JEI. */
public class AnimatedRollingMill extends AnimatedKinetics {

	private final boolean shadow;

	public AnimatedRollingMill(boolean shadow) {
		this.shadow = shadow;
	}

	public AnimatedRollingMill() {
		this(true);
	}

	@Override
	public void draw(GuiGraphicsExtractor graphics, int xOffset, int yOffset) {
		viewRotation(-22.5f, 22.5f);
		Matrix3x2fStack matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		matrixStack.translate((float) xOffset, (float) yOffset);
		if (shadow)
			AllGuiTextures.JEI_SHADOW.render(graphics, -16, 13);
		matrixStack.translate(-2f, 18f);
		int scale = 22;

		BlockState shaft = shaft(Direction.Axis.Z);
		BlockState mill = AllBlocks.ROLLING_MILL.getDefaultState();
		float angle = getCurrentAngle();

		scene(graphics, scale, (ps, col) -> {
			part(ps, col, modelOf(shaft), shaft, 0, 0, 0, 0, 0, -angle);
			part(ps, col, modelOf(shaft), shaft, 0, -4 / 16f, 0, 0, 0, angle);
			part(ps, col, modelOf(mill), mill, 0, 0, 0, 0, 0, 0);
		});

		matrixStack.popMatrix();
	}
}
