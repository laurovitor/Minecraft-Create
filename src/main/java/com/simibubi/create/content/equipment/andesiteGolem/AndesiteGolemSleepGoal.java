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
	private static final int CHECK_INTERVAL = 40;

	private final AndesiteGolem golem;
	private @Nullable BlockPos bed;
	private int checkCooldown;

	public AndesiteGolemSleepGoal(AndesiteGolem golem) {
		this.golem = golem;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (checkCooldown-- > 0)
			return false;
		checkCooldown = CHECK_INTERVAL;
		return shouldSleep();
	}

	@Override
	public boolean canContinueToUse() {
		if (golem.hurtTime > 0)
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
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void tick() {
		if (bed == null)
			return;
		if (golem.position().distanceTo(Vec3.atBottomCenterOf(bed)) > 1.2) {
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
	}

	/** A free spot beside the chest, front first. */
	private BlockPos bedNextTo(BlockPos chest) {
		Direction facing = golem.level().getBlockState(chest).getOptionalValue(AndesiteGolemChestBlock.FACING)
			.orElse(Direction.NORTH);
		for (Direction side : new Direction[] { facing, facing.getClockWise(), facing.getCounterClockWise(), facing.getOpposite() }) {
			BlockPos spot = chest.relative(side);
			if (golem.level().getBlockState(spot).getCollisionShape(golem.level(), spot).isEmpty()
				&& golem.level().getBlockState(spot.above()).getCollisionShape(golem.level(), spot.above()).isEmpty())
				return spot;
		}
		return chest.above();
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
