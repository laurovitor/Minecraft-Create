package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Window of the golem chests: the chest layout with separator / gaps drawn into the background. */
public class GolemChestScreen extends AbstractContainerScreen<GolemChestMenu> {

	private static final Identifier STANDARD = Create.asResource("textures/gui/golem_chest.png");
	private static final Identifier REDSTONE = Create.asResource("textures/gui/golem_chest_redstone.png");

	public GolemChestScreen(GolemChestMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 168);
		this.inventoryLabelY = this.imageHeight - 94;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractBackground(graphics, mouseX, mouseY, a);
		int xo = (this.width - this.imageWidth) / 2;
		int yo = (this.height - this.imageHeight) / 2;
		graphics.blit(RenderPipelines.GUI_TEXTURED, menu.isRedstone() ? REDSTONE : STANDARD, xo, yo, 0.0F, 0.0F,
			this.imageWidth, this.imageHeight, 256, 256);
	}

	/** Empty personal / distribution slot: explain what goes there. */
	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (hoveredSlot instanceof GolemChestMenu.RestrictedSlot slot && !slot.hasItem() && menu.getCarried().isEmpty()) {
			String key = slot.distribution ? "golem_chest.slot.distribution"
				: "golem_chest.slot.personal." + menu.contentHolder.kindId();
			graphics.setTooltipForNextFrame(font, CreateLang.translate(key).component(), mouseX, mouseY);
		}
	}
}
