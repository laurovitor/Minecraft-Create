package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.pathfinder.Path;

/**
 * Lauro edition: at night, if its work area is not lit, the golem walks to the side of its chest and
 * sleeps until it is light again (day, or the area gets lit). Sleeping golems are ignored by hostile
 * mobs. A hit wakes it up.
 */
public class AndesiteGolemSleepGoal extends Goal {

	/** Block light (torches, lamps) a spot needs to count as lit; 8 is about what keeps mobs away. */
	private static final int LIT_LEVEL = 8;
	/** How many of the 9 sample spots in the work area must be lit for the area to count as lit. */
	private static final int LIT_SPOTS_NEEDED = 6;
	/** Night check interval, GAME ticks (2 s); also how soon a woken golem lies down again. */
	private static final int CHECK_INTERVAL = 40;
	/** Walking to the bedside gives up after this long without getting 0.25 blocks closer, ticks (3 s). */
	private static final int STUCK_TICKS = 60;
	/** ... or after this long in total, ticks (20 s): then it sleeps where it stands. */
	private static final int MAX_TRAVEL_TICKS = 400;

	private final AndesiteGolem golem;
	private @Nullable BlockPos bed;
	private long nextCheck = Long.MIN_VALUE;
	private int travelTicks;
	private int stuckTicks;
	private double bestDistance;

	public AndesiteGolemSleepGoal(AndesiteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		long now = golem.level().getGameTime();
		if (now < nextCheck && nextCheck - now <= CHECK_INTERVAL)
			return false;
		nextCheck = now + CHECK_INTERVAL;
		return shouldSleep();
	}

	@Override
	public boolean canContinueToUse() {
		if (golem.hurtTime > 0 || bed == null)
			return false;
		// Recheck on every goal update: a light placed near the chest or /time set day
		// must release the MOVE/LOOK flags promptly so work can resume.
		return shouldSleep();
	}

	private boolean shouldSleep() {
		AndesiteGolemChestBlockEntity chest = golem.getChest();
		long timeOfDay = Math.floorMod(golem.level().getOverworldClockTime(), 24000);
		return chest != null && timeOfDay >= 12500 && timeOfDay < 23500
			&& !isWorkAreaLit(golem.level(), chest.getBlockPos());
	}

	@Override
	public void start() {
		AndesiteGolemChestBlockEntity chest = golem.getChest();
		bed = chest == null ? null : bedNextTo(chest.getBlockPos());
		// No reachable bedside used to release the goal, so the golem stayed awake all night in the
		// work goal. A dark night now always ends in sleep: here, if no bedside can be reached.
		if (GolemWorkGeometry.sleepHereInstead(bed != null, 0, 0, STUCK_TICKS, MAX_TRAVEL_TICKS))
			bed = golem.blockPosition();
		travelTicks = 0;
		stuckTicks = 0;
		bestDistance = Double.MAX_VALUE;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (bed == null)
			return;
		double distance = golem.position().distanceTo(Vec3.atBottomCenterOf(bed));
		if (distance > 1.2) {
			if (distance < bestDistance - 0.25) {
				bestDistance = distance;
				stuckTicks = 0;
			} else
				stuckTicks++;
			if (GolemWorkGeometry.sleepHereInstead(true, stuckTicks, ++travelTicks, STUCK_TICKS, MAX_TRAVEL_TICKS)) {
				bed = golem.blockPosition(); // cannot get there: sleep here instead of standing awake
				return;
			}
			golem.setSleeping(false);
			if (golem.getNavigation().isDone())
				golem.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 1.0);
			return;
		}
		golem.getNavigation().stop();
		golem.setSleeping(true);
	}

	@Override
	public void stop() {
		golem.setSleeping(false);
		bed = null;
		// Woken by a hit: lie down again soon (not a full interval later) so mobs lose interest.
		nextCheck = golem.level().getGameTime() + 10;
	}

	/** Prefer a supported, reachable bedside. A clear-looking block can still be a hole or
	 * have no route from this golem; such a choice leaves it standing awake all night. */
	private @Nullable BlockPos bedNextTo(BlockPos chest) {
		Direction facing = golem.level().getBlockState(chest).getOptionalValue(AndesiteGolemChestBlock.FACING)
			.orElse(Direction.NORTH);
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Direction side : new Direction[] { facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite() }) {
			BlockPos spot = chest.relative(side);
			if (!canRestAt(spot))
				continue;
			double distance = golem.position().distanceToSqr(Vec3.atBottomCenterOf(spot));
			Path path = golem.getNavigation().createPath(spot, 1);
			if (distance > 1.2 * 1.2 && (path == null || path.getEndNode() == null
				|| !GolemWorkGeometry.closeToBed(path.getEndNode().x + 0.5, path.getEndNode().y,
					path.getEndNode().z + 0.5, spot.getX(), spot.getY(), spot.getZ())))
				continue;
			if (distance < bestDistance) {
				best = spot;
				bestDistance = distance;
			}
		}
		// A golem already on its chest can rest there if every side is blocked.
		if (best == null && golem.position().distanceToSqr(Vec3.atBottomCenterOf(chest.above())) <= 1.2 * 1.2)
			return chest.above();
		return best;
	}

	private boolean canRestAt(BlockPos spot) {
		return golem.level().isLoaded(spot) && golem.level().isLoaded(spot.below())
			&& golem.level().getBlockState(spot).getCollisionShape(golem.level(), spot).isEmpty()
			&& golem.level().getBlockState(spot.above()).getCollisionShape(golem.level(), spot.above()).isEmpty()
			&& golem.level().getBlockState(spot.below()).isFaceSturdy(golem.level(), spot.below(), Direction.UP);
	}

	/** The chest spot and eight spots halfway to the edge of the work area, each at ground level. */
	public static boolean isWorkAreaLit(Level level, BlockPos chest) {
		int half = AndesiteGolem.WORK_RADIUS / 2;
		int lit = 0;
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				BlockPos column = chest.offset(dx * half, 0, dz * half);
				BlockPos ground = dx == 0 && dz == 0 ? chest.above()
					: level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
				if (Math.abs(ground.getY() - chest.getY()) > AndesiteGolem.WORK_RADIUS)
					ground = column;
				if (level.getBrightness(LightLayer.BLOCK, ground) >= LIT_LEVEL)
					lit++;
			}
		}
		return lit >= LIT_SPOTS_NEEDED;
	}
}
