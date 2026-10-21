package com.simibubi.create.content.equipment.redstoneGolem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

/** Only the collector treats closed doors and gates as traversable nodes. */
public class RedstoneGolemNavigation extends GroundPathNavigation {
	public RedstoneGolemNavigation(Mob mob, Level level) {
		super(mob, level);
		setCanOpenDoors(true);
		setMaxVisitedNodesMultiplier(4);
	}

	@Override
	protected PathFinder createPathFinder(int nodes) {
		nodeEvaluator = new WalkNodeEvaluator() {
			@Override
			public PathType getPathType(PathfindingContext context, int x, int y, int z) {
				BlockState state = context.getBlockState(new BlockPos(x, y, z));
				if (state.getBlock() instanceof DoorBlock && !state.getValue(DoorBlock.OPEN)
					|| state.getBlock() instanceof FenceGateBlock && !state.getValue(FenceGateBlock.OPEN))
					return PathType.WALKABLE_DOOR;
				// Open a hatch only when another solid floor supports the entire crossing.
				if (state.getBlock() instanceof TrapDoorBlock && !state.getValue(TrapDoorBlock.OPEN)) {
					BlockPos floor = new BlockPos(x, y - 1, z);
					return context.getBlockState(floor).isFaceSturdy(context.level(), floor,
						net.minecraft.core.Direction.UP) ? PathType.WALKABLE_DOOR : PathType.BLOCKED;
				}
				return super.getPathType(context, x, y, z);
			}
		};
		nodeEvaluator.setCanOpenDoors(true);
		nodeEvaluator.setCanPassDoors(true);
		return new PathFinder(nodeEvaluator, nodes);
	}
}
