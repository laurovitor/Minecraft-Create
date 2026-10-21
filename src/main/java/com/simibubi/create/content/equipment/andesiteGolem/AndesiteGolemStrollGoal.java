package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * With nothing to do or a full chest, the Andesite Golem ambles around within
 * {@link AndesiteGolem#IDLE_RADIUS} blocks of its chest (or of where it stands, chest gone).
 */
public class AndesiteGolemStrollGoal extends Goal {

	private final AndesiteGolem golem;
	private BlockPos spot = BlockPos.ZERO;

	public AndesiteGolemStrollGoal(AndesiteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE));
	}

	@Override
	public boolean canUse() {
		if (golem.isVehicle() || golem.getRandom().nextInt(reducedTickDelay(60)) != 0)
			return false;
		BlockPos center = golem.getChestPos() != null ? golem.getChestPos() : golem.blockPosition();
		int r = AndesiteGolem.IDLE_RADIUS;
		int dx = golem.getRandom().nextInt(2 * r + 1) - r;
		int dz = golem.getRandom().nextInt(2 * r + 1) - r;
		if (dx * dx + dz * dz > r * r)
			return false;
		spot = center.offset(dx, 0, dz);
		// stay at the chest's height unless the ground there is clearly elsewhere
		BlockPos ground = golem.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spot);
		if (Math.abs(ground.getY() - center.getY()) <= 3)
			spot = ground;
		return true;
	}

	@Override
	public void start() {
		golem.getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.8);
	}

	@Override
	public boolean canContinueToUse() {
		return !golem.getNavigation().isDone();
	}

	@Override
	public void stop() {
		golem.getNavigation().stop();
	}
}
