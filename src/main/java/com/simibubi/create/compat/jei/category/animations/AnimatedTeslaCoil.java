package com.simibubi.create.compat.jei.category.animations;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.electricity.tesla.TeslaCoilBlock;
import com.simibubi.create.foundation.gui.AllGuiTextures;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/** A powered Tesla Coil facing down, for JEI. */
public class AnimatedTeslaCoil extends AnimatedKinetics {

	private final boolean shadow;
	private final Direction facing;

	public AnimatedTeslaCoil(boolean shadow, Direction facing) {
		this.shadow = shadow;
		this.facing = facing;
	}

	public AnimatedTeslaCoil() {
		this(true, Direction.DOWN);
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

		BlockState coil = AllBlocks.TESLA_COIL.getDefaultState()
			.setValue(TeslaCoilBlock.FACING, facing)
			.setValue(TeslaCoilBlock.POWERED, true);
		scene(graphics, 22, (ps, col) -> part(ps, col, modelOf(coil), coil, 0, 0, 0, 0, 0, 0));

		matrixStack.popMatrix();
	}
}
