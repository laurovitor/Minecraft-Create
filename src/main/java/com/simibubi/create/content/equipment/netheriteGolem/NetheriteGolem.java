package com.simibubi.create.content.equipment.netheriteGolem;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
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
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** A separate guardian: 25-block chest-centered territory, combat only, no Andesite work goals. */
public class NetheriteGolem extends AndesiteGolem {
	public static final int GUARD_RADIUS = 25;
	public static final double BASE_SPEED = 0.22; // 10% faster than healthy Andesite (0.20).
	private int monsterKills;

	public NetheriteGolem(EntityType<? extends net.minecraft.world.entity.animal.golem.AbstractGolem> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, BASE_SPEED)
			.add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.MAX_HEALTH, 80.0)
			.add(Attributes.ATTACK_DAMAGE, 12.0).add(Attributes.ARMOR, 12.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
		goalSelector.addGoal(5, new NetheriteGolemGuardGoal(this));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void setChest(BlockPos pos) {
		BlockPos old = getChestPos();
		super.setChest(pos);
		setHomeTo(pos, GUARD_RADIUS);
		if (old != null && !old.equals(pos)) monsterKills = 0;
	}

	@Override
	public @Nullable NetheriteGolemChestBlockEntity getChest() {
		return super.getChest() instanceof NetheriteGolemChestBlockEntity chest ? chest : null;
	}

	public int getMonsterKills() { return monsterKills; }
	public void setMonsterKills(int kills) { monsterKills = Math.max(0, kills); }
	public void countKill() { monsterKills++; }
	public boolean inTerritory(BlockPos pos) {
		BlockPos home = getChestPos();
		return home != null && NetheriteGolemRules.inTerritory(pos.getX()-home.getX(), pos.getY()-home.getY(), pos.getZ()-home.getZ());
	}

	@Override
	protected void updateMovementSpeed() {
		var speed = getAttribute(Attributes.MOVEMENT_SPEED);
		// Each full 10% max-health lost costs 5% of base speed; healed reversibly.
		if (speed != null) speed.setBaseValue(NetheriteGolemRules.movementSpeed(getHealth(), getMaxHealth()));
	}

	@Override
	public void tick() {
		super.tick();
		if (level().isClientSide()) return;
		if (getChest() == null) { setTarget(null); getNavigation().stop(); return; }
		if (getTarget() != null && (!getTarget().isAlive() || !(getTarget() instanceof Enemy)
			|| !inTerritory(getTarget().blockPosition()))) setTarget(null);
		if (tickCount % 10 == 0) {
			BlockPos center = getChestPos();
			Mob nearest = level().getEntitiesOfClass(Mob.class,
				new net.minecraft.world.phys.AABB(center).inflate(GUARD_RADIUS),
				m -> NetheriteGolemRules.shouldTarget(m instanceof Enemy, m.isAlive(), inTerritory(m.blockPosition()))).stream()
				.min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
			setTarget(nearest); // No filter lookup: all hostile Monsters, including creepers.
		}
		if (!inTerritory(blockPosition())) {
			setTarget(null);
			getNavigation().stop();
			BlockPos home = getChestPos();
			getNavigation().moveTo(home.getX() + .5, home.getY() + 1, home.getZ() + .5, 1);
		}
		long time = Math.floorMod(level().getOverworldClockTime(), 24000);
		boolean day = NetheriteGolemRules.isDay(time);
		setSleeping(day && getTarget() == null);
		if (isAsleep()) getNavigation().stop();
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.ANCIENT_DEBRIS) && player.isShiftKeyDown() && getHealth() < getMaxHealth()) {
			if (!level().isClientSide()) {
				heal(10.0f); // One ancient debris restores 10 HP, capped at 80.
				if (!player.hasInfiniteMaterials()) held.shrink(1);
			}
			return InteractionResult.SUCCESS;
		}
		if (held.is(com.simibubi.create.AllItems.ANDESITE_ALLOY.get())) return InteractionResult.PASS;
		if (AllBlocks.CLIPBOARD.isIn(held)) {
			if (!level().isClientSide() && getChest() != null)
				NetheriteGolemClipboardInfo.write(player, held, this, getChest());
			return InteractionResult.SUCCESS;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
		EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		// Do not call Andesite finalizeSpawn: its egg places an Andesite chest.
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, EntitySpawnReason.TRIGGERED, data);
		if (getChestPos() == null && (reason == EntitySpawnReason.SPAWN_ITEM_USE || reason == EntitySpawnReason.DISPENSER)) {
			BlockPos pos = blockPosition();
			if (level.getBlockState(pos).canBeReplaced() && level.getBlockState(pos.above()).canBeReplaced()) {
				level.setBlock(pos, AllBlocks.NETHERITE_GOLEM_CHEST.getDefaultState()
					.setValue(NetheriteGolemChestBlock.FACING, getDirection().getOpposite())
					.setValue(NetheriteGolemChestBlock.ACTIVE, true), Block.UPDATE_ALL);
				setChest(pos);
				if (level.getBlockEntity(pos) instanceof NetheriteGolemChestBlockEntity chest) chest.linkGolem(getUUID());
				snapTo(pos.getX() + .5, pos.getY() + .9, pos.getZ() + .5, getYRot(), 0);
			}
		}
		return result;
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
		for (ItemStack stack : getCarried()) spawnAtLocation(level, stack);
		getCarried().clear();
		// Actual 50% chance of 1 or 2 ingots; dead chest remains pressable for six.
		int ingots = NetheriteGolemRules.deathIngots(random.nextBoolean(), random.nextBoolean());
		if (ingots > 0) spawnAtLocation(level, new ItemStack(Items.NETHERITE_INGOT, ingots));
	}

	@Override
	public void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("MonsterKills", monsterKills);
	}
	@Override
	public void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		monsterKills = Math.max(0, input.getIntOr("MonsterKills", 0));
	}
}
