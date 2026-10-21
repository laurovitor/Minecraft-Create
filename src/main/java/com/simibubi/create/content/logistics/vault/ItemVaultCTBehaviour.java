package com.simibubi.create.content.logistics.vault;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.createmod.catnip.api.data.Couple;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public class ItemVaultCTBehaviour extends ConnectedTextureBehaviour.Base {

	private final Couple<CTSpriteShiftEntry> front, top, bottom, side;

	public ItemVaultCTBehaviour() {
		this(AllSpriteShifts.VAULT_FRONT, AllSpriteShifts.VAULT_TOP, AllSpriteShifts.VAULT_BOTTOM,
			AllSpriteShifts.VAULT_SIDE);
	}

	public ItemVaultCTBehaviour(Couple<CTSpriteShiftEntry> front, Couple<CTSpriteShiftEntry> top,
		Couple<CTSpriteShiftEntry> bottom, Couple<CTSpriteShiftEntry> side) {
		this.front = front;
		this.top = top;
		this.bottom = bottom;
		this.side = side;
	}

	public static ItemVaultCTBehaviour reinforced() {
		return new ItemVaultCTBehaviour(AllSpriteShifts.REINFORCED_VAULT_FRONT, AllSpriteShifts.REINFORCED_VAULT_TOP,
			AllSpriteShifts.REINFORCED_VAULT_BOTTOM, AllSpriteShifts.REINFORCED_VAULT_SIDE);
	}

	@Override
	public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		boolean small = !ItemVaultBlock.isLarge(state);
		if (vaultBlockAxis == null)
			return null;

		if (direction.getAxis() == vaultBlockAxis)
			return front.get(small);
		if (direction == Direction.UP)
			return top.get(small);
		if (direction == Direction.DOWN)
			return bottom.get(small);

		return side.get(small);
	}

	@Override
	protected Direction getUpDirection(BlockGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		boolean alongX = vaultBlockAxis == Axis.X;
		if (face.getAxis()
			.isVertical() && alongX)
			return super.getUpDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == vaultBlockAxis || face.getAxis()
			.isVertical())
			return super.getUpDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(vaultBlockAxis, alongX ? AxisDirection.POSITIVE : AxisDirection.NEGATIVE);
	}

	@Override
	protected Direction getRightDirection(BlockGetter reader, BlockPos pos, BlockState state, Direction face) {
		Axis vaultBlockAxis = ItemVaultBlock.getVaultBlockAxis(state);
		if (face.getAxis()
			.isVertical() && vaultBlockAxis == Axis.X)
			return super.getRightDirection(reader, pos, state, face).getClockWise();
		if (face.getAxis() == vaultBlockAxis || face.getAxis()
			.isVertical())
			return super.getRightDirection(reader, pos, state, face);
		return Direction.fromAxisAndDirection(Axis.Y, face.getAxisDirection());
	}

	public boolean buildContextForOccludedDirections() {
		return super.buildContextForOccludedDirections();
	}

	@Override
	public boolean connectsTo(BlockState state, BlockState other, BlockGetter reader, BlockPos pos,
		BlockPos otherPos, Direction face) {
		return state == other && ConnectivityHandler.isConnected(reader, pos, otherPos); //ItemVaultConnectivityHandler.isConnected(reader, pos, otherPos);
	}

}
