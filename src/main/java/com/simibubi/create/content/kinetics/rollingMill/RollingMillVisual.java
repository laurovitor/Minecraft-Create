package com.simibubi.create.content.kinetics.rollingMill;

import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;

/** Two counter-rotating rollers, the lower one at shaft height and the upper one 4px above it. */
public class RollingMillVisual extends KineticBlockEntityVisual<RollingMillBlockEntity> {
	protected final RotatingInstance lowerRoller;
	protected final RotatingInstance upperRoller;

	public RollingMillVisual(VisualizationContext context, RollingMillBlockEntity blockEntity, float partialTick) {
		super(context, blockEntity, partialTick);

		Axis axis = axis();
		var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT));

		lowerRoller = instancer.createInstance()
			.rotateToFace(Direction.UP, axis);
		upperRoller = instancer.createInstance()
			.rotateToFace(Direction.UP, axis);

		lowerRoller.setup(blockEntity, axis)
			.setPosition(getVisualPosition())
			.setChanged();
		upperRoller.setup(blockEntity, axis, -blockEntity.getSpeed())
			.setPosition(getVisualPosition())
			.nudge(0, 4f / 16f, 0)
			.setChanged();
	}

	private Axis axis() {
		return blockState.getValue(RollingMillBlock.HORIZONTAL_FACING)
			.getAxis();
	}

	@Override
	public void update(float partialTick) {
		Axis axis = axis();
		lowerRoller.setup(blockEntity, axis, blockEntity.getSpeed())
			.setChanged();
		upperRoller.setup(blockEntity, axis, -blockEntity.getSpeed())
			.setChanged();
	}

	@Override
	public void updateLight(float partialTick) {
		relight(lowerRoller, upperRoller);
	}

	@Override
	public void collectCrumblingInstances(Consumer<@Nullable Instance> consumer) {
		consumer.accept(lowerRoller);
		consumer.accept(upperRoller);
	}

	@Override
	protected void _delete() {
		lowerRoller.delete();
		upperRoller.delete();
	}
}
