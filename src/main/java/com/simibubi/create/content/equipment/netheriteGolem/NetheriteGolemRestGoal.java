package com.simibubi.create.content.equipment.netheriteGolem;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Lauro edition: by day, with nothing hostile around, the golem walks home and sleeps standing on
 * its chest, or on the closest free spot beside it. If neither can be reached it sleeps wherever its
 * route ended or it stopped making progress; it never stands awake waiting for a path.
 */
public class NetheriteGolemRestGoal extends Goal {

	/** The chest is 14/16 of a block tall; a golem on top stands this far above its base. */
	public static final double CHEST_TOP = 14 / 16.0;
	/** Gives up walking and sleeps where it is after this long, ticks (30 s). */
	private static final int MAX_TRAVEL_TICKS = 600;
	/** Sleeps where it is after this many ticks without getting 0.25 blocks closer (3 s). */
	private static final int STUCK_TICKS = 60;

	private final NetheriteGolem golem;
	private @Nullable RestSpot spot;
	private boolean settled;
	private int travelTicks;
	private int stuckTicks;
	private double bestDistance;

	/** Where to stand: feet block and whether that block is the space above the chest. */
	public record RestSpot(BlockPos feet, boolean onChest) {
		public Vec3 standing() {
			return new Vec3(feet.getX() + 0.5, onChest ? feet.getY() - 1 + CHEST_TOP : feet.getY(), feet.getZ() + 0.5);
		}
	}

	public NetheriteGolemRestGoal(NetheriteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		return golem.getChest() != null && golem.wantsRest();
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void start() {
		settled = false;
		travelTicks = 0;
		stuckTicks = 0;
		bestDistance = Double.MAX_VALUE;
		BlockPos chest = golem.getChestPos();
		spot = chest == null ? null : restSpot(golem.level(), chest, golem.position());
		if (spot == null || arrived()) {
			settled = true;
			return;
		}
		Path path = golem.getNavigation().createPath(spot.feet(), 0);
		// No route at all: sleep here rather than stand awake all day.
		if (path == null || !golem.getNavigation().moveTo(path, 1.0))
			settled = true;
	}

	@Override
	public void tick() {
		if (!settled && spot != null) {
			double distance = golem.position().distanceTo(spot.standing());
			if (distance < bestDistance - 0.25) {
				bestDistance = distance;
				stuckTicks = 0;
			} else
				stuckTicks++;
			// A partial route ends at the closest reachable spot: done walking there too.
			settled = arrived() || golem.getNavigation().isDone() || stuckTicks > STUCK_TICKS
				|| ++travelTicks > MAX_TRAVEL_TICKS;
		}
		if (!settled)
			return;
		golem.getNavigation().stop();
		golem.setSleeping(true);
	}

	@Override
	public void stop() {
		golem.setSleeping(false);
		golem.getNavigation().stop();
		spot = null;
	}

	private boolean arrived() {
		Vec3 at = spot.standing();
		return Math.abs(golem.getY() - at.y) <= 0.5
			&& Math.pow(golem.getX() - at.x, 2) + Math.pow(golem.getZ() - at.z, 2) <= (spot.onChest() ? 0.35 * 0.35 : 0.6 * 0.6);
	}

	/** The chest top when free, otherwise the free spot around the chest closest to {@code from}. */
	public static @Nullable RestSpot restSpot(Level level, BlockPos chest, Vec3 from) {
		if (clear(level, chest.above()) && clear(level, chest.above(2)))
			return new RestSpot(chest.above(), true);
		RestSpot best = null;
		double bestDistance = Double.MAX_VALUE;
		for (int dy : new int[] { 0, 1, -1 })
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++) {
					BlockPos feet = chest.offset(dx, dy, dz);
					if (dx == 0 && dz == 0 || !canStandAt(level, feet))
						continue;
					double distance = from.distanceToSqr(Vec3.atBottomCenterOf(feet));
					if (distance < bestDistance) {
						best = new RestSpot(feet, false);
						bestDistance = distance;
					}
				}
		return best;
	}

	private static boolean clear(Level level, BlockPos pos) {
		return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	private static boolean canStandAt(Level level, BlockPos feet) {
		return clear(level, feet) && clear(level, feet.above()) && level.isLoaded(feet.below())
			&& level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
	}
}
