package com.simibubi.create.content.trains.station;

import org.joml.Matrix3x2fStack;
import net.minecraft.client.Minecraft;
import java.lang.ref.WeakReference;
import java.util.List;

import com.simibubi.create.CreateClient;
import com.simibubi.create.compat.computercraft.ComputerScreen;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TrainIconType;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.render.GuiScene;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import net.createmod.catnip.api.client.gui.ILightingSettings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class AbstractStationScreen extends AbstractSimiScreen {

	protected AllGuiTextures background;
	protected StationBlockEntity blockEntity;
	protected GlobalStation station;

	protected WeakReference<Train> displayedTrain;

	private IconButton confirmButton;

	public AbstractStationScreen(StationBlockEntity be, GlobalStation station) {
		super(be.getBlockState()
			.getBlock()
			.getName());
		this.blockEntity = be;
		this.station = station;
		displayedTrain = new WeakReference<>(null);
	}

	@Override
	protected void init() {
		if (blockEntity.computerBehaviour.hasAttachedComputer())
			minecraft.gui.setScreen(new ComputerScreen(title, () ->
                Component.literal(station.name),
				this::renderAdditional, this, blockEntity.computerBehaviour::hasAttachedComputer));

		setWindowSize(background.getWidth(), background.getHeight());
		super.init();
		clearWidgets();

		int x = guiLeft;
		int y = guiTop;

		confirmButton = new IconButton(x + background.getWidth() - 33, y + background.getHeight() - 24, AllIcons.I_CONFIRM);
		confirmButton.withCallback(this::onClose);
		addRenderableWidget(confirmButton);
	}

	public int getTrainIconWidth(Train train) {
		TrainIconType icon = train.icon;
		List<Carriage> carriages = train.carriages;

		int w = icon.getIconWidth(TrainIconType.ENGINE);
		if (carriages.size() == 1)
			return w;

		for (int i = 1; i < carriages.size(); i++) {
			if (i == carriages.size() - 1 && train.doubleEnded) {
				w += icon.getIconWidth(TrainIconType.FLIPPED_ENGINE) + 1;
				break;
			}
			Carriage carriage = carriages.get(i);
			w += icon.getIconWidth(carriage.bogeySpacing) + 1;
		}

		return w;
	}

	@Override
	public void tick() {
		super.tick();

		if (blockEntity.computerBehaviour.hasAttachedComputer())
			minecraft.gui.setScreen(new ComputerScreen(title, () ->
                Component.literal(station.name),
				this::renderAdditional, this, blockEntity.computerBehaviour::hasAttachedComputer));
	}

	@Override
	protected void renderWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
		int x = guiLeft;
		int y = guiTop;

		background.render(graphics, x, y);
		renderAdditional(graphics, mouseX, mouseY, partialTicks, x, y, background);
	}

	private void renderAdditional(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks, int guiLeft, int guiTop, AllGuiTextures background) {
		Matrix3x2fStack ms = graphics.pose();
		ms.pushMatrix();
		// The GUI pose locates the picture; GuiScene supplies its shared 3D view rotation.
		ms.translate(guiLeft + background.getWidth() + 4, guiTop + background.getHeight() + 4);

		var stationState = blockEntity.getBlockState()
			.setValue(BlockStateProperties.WATERLOGGED, false);
		float nudge = 1 / 512f;
		float pitch = StationRenderer.flagProgress(blockEntity, partialTicks) * 90 + 270;
		boolean showFlag = blockEntity.resolveFlagAngle();
		// Body and animated flag share one depth pass, preserving their relative placement.
		GuiScene.submit(graphics, 40, -22, 63, ILightingSettings.ITEMS_3D, (ps, collector) -> {
			GuiScene.part(ps, collector, GuiScene.modelOf(stationState), stationState, 0, 0, 0, 0, 0, 0);
			if (showFlag)
				GuiScene.partSpun(ps, collector, getFlag(partialTicks).get(), null,
					1 + 1 / 16f - nudge, -9.5f / 16f, 1 / 8f - nudge, pitch, 180, 0);
		});

		ms.popMatrix();
	}

	protected abstract PartialModel getFlag(float partialTicks);

	protected Train getImminent() {
		return blockEntity.imminentTrain == null ? null : CreateClient.RAILWAYS.trains.get(blockEntity.imminentTrain);
	}

	protected boolean trainPresent() {
		return blockEntity.trainPresent;
	}

}
