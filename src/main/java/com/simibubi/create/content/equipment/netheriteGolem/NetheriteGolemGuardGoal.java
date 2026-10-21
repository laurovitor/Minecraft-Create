package com.simibubi.create.content.equipment.netheriteGolem;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

/** Low-priority patrol; no farming, chopping, paving, or other work goals. */
public class NetheriteGolemGuardGoal extends Goal {
	private final NetheriteGolem golem;
	private BlockPos destination;
	public NetheriteGolemGuardGoal(NetheriteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE));
	}
	@Override public boolean canUse() {
		if (golem.isAsleep() || golem.getTarget() != null || golem.getChest() == null
			|| golem.getRandom().nextInt(reducedTickDelay(80)) != 0) return false;
		BlockPos home = golem.getChestPos();
		int dx = golem.getRandom().nextInt(49) - 24;
		int dz = golem.getRandom().nextInt(49) - 24;
		if (dx * dx + dz * dz > 24 * 24) return false;
		destination = home.offset(dx, 0, dz);
		BlockPos ground = golem.level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, destination);
		if (Math.abs(ground.getY() - home.getY()) <= 3) destination = ground;
		return golem.inTerritory(destination);
	}
	@Override public void start() { golem.getNavigation().moveTo(destination.getX()+.5, destination.getY(), destination.getZ()+.5, .9); }
	@Override public boolean canContinueToUse() { return !golem.isAsleep() && golem.getTarget() == null && !golem.getNavigation().isDone(); }
	@Override public void stop() { golem.getNavigation().stop(); }
}
