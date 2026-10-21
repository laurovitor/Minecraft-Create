package com.simibubi.create.content.equipment.andesiteGolem;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.api.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Filter slot lying on the middle of the chest lid (the lid top is at 14 px). */
public class AndesiteGolemChestFilterSlot extends ValueBoxTransform.Sided {

	@Override
	protected Vec3 getSouthLocation() {
		return VecHelper.voxelSpace(8, 8, 14.05);
	}

	@Override
	protected boolean isSideActive(BlockState state, Direction direction) {
		return direction == Direction.UP;
	}
}
