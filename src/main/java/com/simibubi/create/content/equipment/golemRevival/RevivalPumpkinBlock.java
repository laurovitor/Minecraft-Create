package com.simibubi.create.content.equipment.golemRevival;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolem;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Lauro edition: a carved pumpkin in a golem's material. Placed on a chest of the same golem kind
 * that has no golem bound, it becomes that chest's new golem; the chest keeps its contents, filter
 * and network. On a chest whose golem may still be alive, or of another material, it is not placed
 * (stays in hand, with a message). Anywhere else it is a decorative block.
 */
public class RevivalPumpkinBlock extends HorizontalDirectionalBlock {

	private final GolemKind kind;

	public RevivalPumpkinBlock(Properties properties, GolemKind kind) {
		super(properties);
		this.kind = kind;
		registerDefaultState(defaultBlockState().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}

	public GolemKind getKind() {
		return kind;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder.add(FACING));
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Level level = context.getLevel();
		GolemRevivalRules.Outcome outcome = outcome(level, context.getClickedPos().below());
		if (outcome == GolemRevivalRules.Outcome.REFUSE_HAS_GOLEM || outcome == GolemRevivalRules.Outcome.REFUSE_WRONG_MATERIAL) {
			Player player = context.getPlayer();
			if (player != null && !level.isClientSide())
				player.sendOverlayMessage(CreateLang.translate(outcome == GolemRevivalRules.Outcome.REFUSE_HAS_GOLEM
					? "golem_revival.has_golem" : "golem_revival.wrong_material").style(ChatFormatting.RED).component());
			return null; // not placed: the pumpkin stays in hand
		}
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (oldState.is(state.getBlock()) || !(level instanceof ServerLevel serverLevel))
			return;
		if (outcome(level, pos.below()) == GolemRevivalRules.Outcome.REVIVE)
			revive(serverLevel, pos, state);
	}

	/**
	 * "Has a golem" = the chest holds a golem UUID. A golem in an unloaded chunk keeps it, so it counts
	 * as alive. Death releases it (AndesiteGolem#die), or, when the chest was unloaded at that moment,
	 * the GolemGraveyard records it (server only).
	 */
	private GolemRevivalRules.Outcome outcome(Level level, BlockPos chestPos) {
		BlockState below = level.getBlockState(chestPos);
		GolemKind chestKind = GolemKind.ofChest(below);
		UUID bound = level.getBlockEntity(chestPos) instanceof AndesiteGolemChestBlockEntity chest ? chest.getGolemId() : null;
		boolean knownDead = bound != null && level instanceof ServerLevel server
			&& GolemGraveyard.get(server.getServer()).isDead(bound);
		return GolemRevivalRules.decide(chestKind != null, chestKind == kind, bound != null, knownDead);
	}

	private void revive(ServerLevel level, BlockPos pumpkinPos, BlockState pumpkin) {
		BlockPos chestPos = pumpkinPos.below();
		if (!(level.getBlockEntity(chestPos) instanceof AndesiteGolemChestBlockEntity chest))
			return;
		AndesiteGolem golem = kind.golem().create(level, EntitySpawnReason.TRIGGERED);
		if (golem == null)
			return;
		UUID previous = chest.getGolemId();
		if (previous != null)
			GolemGraveyard.get(level.getServer()).forget(previous);

		level.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		level.levelEvent(2001, pumpkinPos, Block.getId(pumpkin));
		// Same block, ACTIVE flipped: the block entity with contents, filter and network stays.
		BlockState chestState = level.getBlockState(chestPos);
		if (!chestState.getValue(kind.active))
			level.setBlock(chestPos, chestState.setValue(kind.active, true), Block.UPDATE_ALL);

		golem.snapTo(pumpkinPos.getX() + 0.5, pumpkinPos.getY() + 0.05, pumpkinPos.getZ() + 0.5,
			pumpkin.getValue(FACING).toYRot(), 0);
		golem.setChest(chestPos);
		chest.linkGolem(golem.getUUID());
		level.addFreshEntity(golem);
		golem.playSpawnSound();
		for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0)))
			CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
		level.updateNeighborsAt(pumpkinPos, Blocks.AIR);
	}
}
