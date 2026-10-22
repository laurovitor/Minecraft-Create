package com.simibubi.create.content.electricity.portable;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

public class ModularAccumulatorMovement implements MovementBehaviour {
    private static final int TRACK_PERIOD = 20;
    @Override public void tick(MovementContext context) {
        if (context.world.isClientSide() || context.contraption.entity == null || !context.blockEntityData.contains("EnergyContent")) return;
        if (context.world.getGameTime() % TRACK_PERIOD == 0) PortableEnergyManager.track(context);
    }
    @Override public void startMoving(MovementContext context) {
        if (!context.world.isClientSide() && context.blockEntityData.contains("EnergyContent")) PortableEnergyManager.track(context);
    }
    @Override public void stopMoving(MovementContext context) {
        if (!context.world.isClientSide()) PortableEnergyManager.untrack(context);
    }
}
