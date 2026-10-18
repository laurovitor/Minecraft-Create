package com.simibubi.create.foundation.gui;

import java.util.Collection;
import java.util.List;

import net.createmod.catnip.api.client.animation.AnimationTickHolder;
import net.createmod.catnip.api.client.gui.TickableGuiEventListener;
import net.createmod.catnip.api.client.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

/** Create's windowed-screen hooks retained after Catnip's 26.3 screen base became minimal. */
public abstract class AbstractSimiScreen extends net.createmod.catnip.api.client.gui.AbstractSimiScreen {
	protected int guiLeft, guiTop, windowWidth, windowHeight;
	private int windowXOffset, windowYOffset;

	protected AbstractSimiScreen() {
		this(Component.empty());
	}

	protected AbstractSimiScreen(Component title) {
		super(title);
	}

	protected void setWindowSize(int width, int height) {
		windowWidth = width;
		windowHeight = height;
	}

	protected void setWindowOffset(int x, int y) {
		windowXOffset = x;
		windowYOffset = y;
	}

	@Override
	protected void init() {
		super.init();
		guiLeft = (width - windowWidth) / 2 + windowXOffset;
		guiTop = (height - windowHeight) / 2 + windowYOffset;
	}

	@Override
	public void tick() {
		super.tick();
		for (GuiEventListener listener : children())
			if (listener instanceof TickableGuiEventListener tickable)
				tickable.tick();
	}

	@SuppressWarnings("unchecked")
	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(W... widgets) {
		for (W widget : widgets)
			addRenderableWidget(widget);
	}

	protected <W extends GuiEventListener & Renderable & NarratableEntry> void addRenderableWidgets(Collection<W> widgets) {
		for (W widget : widgets)
			addRenderableWidget(widget);
	}

	protected void removeWidgets(Collection<? extends GuiEventListener> widgets) {
		for (GuiEventListener widget : widgets)
			removeWidget(widget);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(graphics, mouseX, mouseY, partialTicks);
		renderWindow(graphics, mouseX, mouseY, AnimationTickHolder.getGuiPartialTicks());
	}

	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
		renderWindowForeground(graphics, mouseX, mouseY, AnimationTickHolder.getGuiPartialTicks());
	}

	protected void renderWindowForeground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		for (Renderable renderable : getRenderables())
			if (renderable instanceof AbstractSimiWidget widget && widget.isMouseOver(mouseX, mouseY)) {
				List<Component> tooltip = widget.getToolTip();
				if (!tooltip.isEmpty()) {
					int x = widget.lockedTooltipX == -1 ? mouseX : widget.lockedTooltipX + widget.getX();
					int y = widget.lockedTooltipY == -1 ? mouseY : widget.lockedTooltipY + widget.getY();
					graphics.setComponentTooltipForNextFrame(font, tooltip, x, y);
				}
			}
	}
}
