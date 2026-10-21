package com.simibubi.create.content.equipment.andesiteGolem;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Lauro edition: Andesite Golem. Built like a copper golem, but on an andesite alloy block, which
 * turns into its chest. It cuts trees and harvests crops within {@link #WORK_RADIUS} blocks of the
 * chest, replants what it took and stores what the chest filter accepts. With nothing to do or a
 * full chest it strolls within {@link #IDLE_RADIUS} blocks. Takes fall damage and is hunted by
 * hostile mobs (see AndesiteGolemEvents).
 */
public class AndesiteGolem extends AbstractGolem {

	/** Horizontal distance from the chest the golem works in, any height. */
	public static final int WORK_RADIUS = 10;
	/** Horizontal distance from the chest the golem strolls in while idle. */
	public static final int IDLE_RADIUS = 5;

	private static final EntityDataAccessor<CopperGolemState> STATE =
		SynchedEntityData.defineId(AndesiteGolem.class, EntityDataSerializers.COPPER_GOLEM_STATE);

	private @Nullable BlockPos chestPos;
	/** Gathered items not yet stored in the chest. */
	private final List<ItemStack> carried = new ArrayList<>();

	private int idleAnimationStartTick = 0;
	private final AnimationState idleAnimationState = new AnimationState();
	private final AnimationState workAnimationState = new AnimationState();
	private final AnimationState depositAnimationState = new AnimationState();

	public AndesiteGolem(EntityType<? extends AbstractGolem> type, Level level) {
		super(type, level);
		getNavigation().setRequiredPathLength(48.0F);
		setPersistenceRequired();
		setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 16.0F);
		setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, 16.0F);
		setPathfindingMalus(PathType.FIRE, -1.0F);
		setDropChance(EquipmentSlot.MAINHAND, 0);
	}

	public static AttributeSupplier.Builder createAttributes() {
		// same as the copper golem
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, 0.2F)
			.add(Attributes.STEP_HEIGHT, 1.0)
			.add(Attributes.MAX_HEALTH, 12.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new AndesiteGolemWorkGoal(this));
		goalSelector.addGoal(5, new AndesiteGolemStrollGoal(this));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(STATE, CopperGolemState.IDLE);
	}

	// ---- home chest

	public void setChest(BlockPos pos) {
		chestPos = pos.immutable();
		setHomeTo(chestPos, WORK_RADIUS);
	}

	public @Nullable BlockPos getChestPos() {
		return chestPos;
	}

	/** The chest, if it still exists and still belongs to this golem. */
	public @Nullable AndesiteGolemChestBlockEntity getChest() {
		if (chestPos == null || !level().isLoaded(chestPos))
			return null;
		if (level().getBlockEntity(chestPos) instanceof AndesiteGolemChestBlockEntity chest && chest.isHomeOf(getUUID()))
			return chest;
		return null;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (AllBlocks.CLIPBOARD.isIn(held)) {
			if (!level().isClientSide()) {
				AndesiteGolemChestBlockEntity chest = getChest();
				if (chest != null)
					AndesiteGolemClipboardInfo.write(player, held, this, chest);
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() && AllItems.ANDESITE_ALLOY.isIn(held)
			&& getHealth() < getMaxHealth()) {
			if (!level().isClientSide()) {
				heal(getMaxHealth() * 0.05f);
				if (!player.hasInfiniteMaterials())
					held.shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	/**
	 * A golem from a spawn egg or dispenser gets its chest set down where it appears, and stands on
	 * top of it, as if it had been built.
	 */
	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
		EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		if (chestPos == null && (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER)) {
			BlockPos pos = blockPosition();
			if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()) {
				level.setBlock(pos, AllBlocks.ANDESITE_GOLEM_CHEST.getDefaultState()
					.setValue(AndesiteGolemChestBlock.FACING, getDirection().getOpposite())
					.setValue(AndesiteGolemChestBlock.ACTIVE, true), Block.UPDATE_ALL);
				setChest(pos);
				if (level.getBlockEntity(pos) instanceof AndesiteGolemChestBlockEntity chest)
					chest.linkGolem(getUUID());
				snapTo(pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, getYRot(), 0);
			}
		}
		playSpawnSound();
		return data;
	}

	// ---- carried items

	public List<ItemStack> getCarried() {
		return carried;
	}

	public void carry(ItemStack stack) {
		if (stack.isEmpty())
			return;
		carried.add(stack);
		if (getMainHandItem().isEmpty())
			setItemInHand(InteractionHand.MAIN_HAND, stack.copyWithCount(1));
	}

	public void updateHeldItem() {
		setItemInHand(InteractionHand.MAIN_HAND, carried.isEmpty() ? ItemStack.EMPTY : carried.get(0).copyWithCount(1));
	}

	// ---- animation

	public CopperGolemState getState() {
		return entityData.get(STATE);
	}

	public void setState(CopperGolemState state) {
		entityData.set(STATE, state);
	}

	public AnimationState getIdleAnimationState() {
		return idleAnimationState;
	}

	public AnimationState getWorkAnimationState() {
		return workAnimationState;
	}

	public AnimationState getDepositAnimationState() {
		return depositAnimationState;
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide() && !isNoAi())
			setupAnimationStates();
	}

	private void setupAnimationStates() {
		switch (getState()) {
		case GETTING_ITEM, GETTING_NO_ITEM -> {
			idleAnimationState.stop();
			idleAnimationStartTick = 0;
			depositAnimationState.stop();
			workAnimationState.startIfStopped(tickCount);
		}
		case DROPPING_ITEM, DROPPING_NO_ITEM -> {
			idleAnimationState.stop();
			idleAnimationStartTick = 0;
			workAnimationState.stop();
			depositAnimationState.startIfStopped(tickCount);
		}
		default -> {
			workAnimationState.stop();
			depositAnimationState.stop();
			// the copper golem's occasional head spin
			if (idleAnimationStartTick == tickCount)
				idleAnimationState.start(tickCount);
			else if (idleAnimationStartTick == 0)
				idleAnimationStartTick = tickCount + random.nextInt(200, 240);
			if (tickCount == idleAnimationStartTick + 10)
				idleAnimationStartTick = 0;
		}
		}
	}

	// ---- life and death

	@Override
	protected void actuallyHurt(ServerLevel level, DamageSource source, float amount) {
		super.actuallyHurt(level, source, amount);
		setState(CopperGolemState.IDLE);
	}

	@Override
	public void die(DamageSource source) {
		if (level() instanceof ServerLevel) {
			AndesiteGolemChestBlockEntity chest = getChest();
			if (chest != null)
				chest.releaseGolem();
		}
		super.die(source);
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		for (ItemStack stack : carried)
			spawnAtLocation(level, stack);
		carried.clear();
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (chestPos != null)
			output.store("Chest", BlockPos.CODEC, chestPos);
		output.store("Carried", ItemStack.CODEC.listOf(), carried);
	}

	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("Chest", BlockPos.CODEC).ifPresent(this::setChest);
		carried.clear();
		input.read("Carried", ItemStack.CODEC.listOf()).ifPresent(carried::addAll);
	}

	// ---- copper golem sounds

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.COPPER_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.COPPER_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.COPPER_GOLEM_STEP, 1.0F, 1.0F);
	}

	public void playSpawnSound() {
		playSound(SoundEvents.COPPER_GOLEM_SPAWN);
	}
}
