package com.simibubi.create.content.equipment.redstoneGolem;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;

/**
 * Lauro edition: Redstone Golem. Built on a block of redstone; its only job is carrying items from
 * the chests on its network to its own chest (see RedstoneGolemCollectGoal). The fastest and most
 * fragile golem. No work goals, sleep, repair or combat of its own.
 */
public class RedstoneGolem extends AndesiteGolem {

	/** What the golem is doing, for the clipboard. Lang: create.clipboard.golem_info.task.<key>. */
	public enum Task {
		IDLE("idle"), COLLECTING("collecting"), RETURNING("returning"), CHEST_FULL("chest_full"), NO_SOURCE("no_source");

		public final String key;

		Task(String key) {
			this.key = key;
		}
	}

	private Task task = Task.IDLE;
	/** Network chests found by the last search (loaded and in range), for the clipboard. */
	private int networkChests;
	private final RedstoneGolemBarriers barriers = new RedstoneGolemBarriers(this);

	public RedstoneGolem(EntityType<? extends AbstractGolem> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MOVEMENT_SPEED, RedstoneGolemRules.BASE_SPEED)
			.add(Attributes.STEP_HEIGHT, 1.0)
			.add(Attributes.FOLLOW_RANGE, 96.0)
			.add(Attributes.MAX_HEALTH, RedstoneGolemRules.MAX_HEALTH);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		return new RedstoneGolemNavigation(this, level);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!level().isClientSide()) barriers.tick();
	}

	public void closeOpenedBarriers() { barriers.stop(); }

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new RedstoneGolemCollectGoal(this));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void setChest(BlockPos pos) {
		super.setChest(pos);
		setHomeTo(pos, RedstoneGolemRules.MAX_SOURCE_DISTANCE);
	}

	@Override
	public @Nullable RedstoneGolemChestBlockEntity getChest() {
		return super.getChest() instanceof RedstoneGolemChestBlockEntity chest ? chest : null;
	}

	/** Constant speed: no health-based slowdown. */
	@Override
	protected void updateMovementSpeed() {
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && speed.getBaseValue() != RedstoneGolemRules.BASE_SPEED)
			speed.setBaseValue(RedstoneGolemRules.BASE_SPEED);
	}

	/** No repair item: neither by hand nor from the chest. */
	@Override
	public boolean isRepairItem(ItemStack stack) {
		return false;
	}

	public Task getTask() { return task; }
	public void setTask(Task task) { this.task = task; }
	public int getNetworkChests() { return networkChests; }
	public void setNetworkChests(int count) { networkChests = count; }

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (AllBlocks.CLIPBOARD.isIn(held)) {
			if (!level().isClientSide() && getChest() != null)
				RedstoneGolemClipboardInfo.write(player, held, this, getChest());
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
		EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		// Do not let Andesite finalizeSpawn run its egg logic: it places an Andesite chest.
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, data);
		if (getChestPos() == null && (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER)) {
			BlockPos pos = blockPosition();
			if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()) {
				level.setBlock(pos, AllBlocks.REDSTONE_GOLEM_CHEST.getDefaultState()
					.setValue(RedstoneGolemChestBlock.FACING, getDirection().getOpposite())
					.setValue(RedstoneGolemChestBlock.ACTIVE, true), Block.UPDATE_ALL);
				setChest(pos);
				if (level.getBlockEntity(pos) instanceof RedstoneGolemChestBlockEntity chest)
					chest.linkGolem(getUUID());
				snapTo(pos.getX() + .5, pos.getY() + .9, pos.getZ() + .5, getYRot(), 0);
			}
		}
		return result;
	}

	/** Show Create's real cardboard box while the internal inventory carries any stacks. */
	@Override
	public void carry(ItemStack stack) {
		super.carry(stack);
		updateHeldItem();
	}

	@Override
	public void updateHeldItem() {
		setItemInHand(InteractionHand.MAIN_HAND,
			getCarried().isEmpty() ? ItemStack.EMPTY : PackageStyles.getDefaultBox());
	}

	@Override
	public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
		super.readAdditionalSaveData(input);
		updateHeldItem();
	}

	/** What it carried, and two redstone dust from its body (the Andesite golem leaves two alloy). */
	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		if (!getCarried().isEmpty()) {
			// The carried list is the only inventory of record. The hand box is visual only.
			// Create unpacks PACKAGE_CONTENTS through PackageItem.getContents. Preserve the
			// four carried stacks exactly, rather than relying on a lossy insertion attempt.
			List<ItemStack> contents = getCarried().stream().filter(stack -> !stack.isEmpty())
				.map(ItemStack::copy).toList();
			for (int i = 0; i < contents.size(); i += PackageItem.SLOTS) {
				// Normally one cardboard box: the golem can carry at most four of nine slots.
				// Split only an oversized list restored from external/old data, never lose stacks.
				ItemStack box = PackageStyles.getDefaultBox();
				box.set(AllDataComponents.PACKAGE_CONTENTS,
					ItemContainerContents.fromItems(contents.subList(i, Math.min(i + PackageItem.SLOTS, contents.size()))));
				spawnAtLocation(level, box);
			}
			getCarried().clear();
			updateHeldItem();
		}
		spawnAtLocation(level, new ItemStack(Items.REDSTONE, 2));
	}
}
