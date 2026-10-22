package com.simibubi.create.compat.jei.category.sequencedAssembly;

import org.joml.Matrix3x2fStack;

import com.simibubi.create.compat.jei.category.animations.AnimatedTeslaCoil;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;

/** Tesla Coil charging step of a sequenced assembly. */
public class ChargingAssemblySubCategory extends SequencedAssemblySubCategory {

	private final AnimatedTeslaCoil teslaCoil = new AnimatedTeslaCoil(false, Direction.DOWN);

	public ChargingAssemblySubCategory() {
		super(25);
	}

	@Override
	public void draw(SequencedRecipe<?> recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY, int index) {
		Matrix3x2fStack ms = graphics.pose();
		ms.pushMatrix();
		ms.translate(-5f, 50f);
		ms.scale(.6f, .6f);
		teslaCoil.draw(graphics, getWidth() / 2, 0);
		ms.popMatrix();
	}
}
