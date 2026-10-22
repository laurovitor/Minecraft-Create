package com.simibubi.create.content.electricity.pump;

import java.util.List;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.electricity.energy.InternalEnergyStorage;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.electricity.ElectricityUtil;


import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.joml.Quaternionf;

public class ElectricPumpBlockEntity extends PumpBlockEntity {
	public static final float[][] PARTIAL_OFFSETS = {
			{8 / 16f, 4 / 16f, 8 / 16f},  // bottom face at y=2
			{8 / 16f, 12 / 16f, 8 / 16f}  // bottom face at y=10
	};

	public static final float[] PARTIAL_PHASE_OFFSETS = {6f, 0f};

	protected float pumpSpeed;
	protected ScrollValueBehaviour speedBehaviour;
	protected final InternalEnergyStorage energy;
	private final EnergyHandler capability;

	private boolean active = false;

	public ElectricPumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energy = new InternalEnergyStorage(AllConfigs.server().electricity.pumpCapacity.get(), AllConfigs.server().electricity.pumpMaxInput.get(), 0);
		capability = energy;
		setLazyTickRate(20);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Energy.BLOCK,
				AllBlockEntityTypes.ELECTRIC_PUMP.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		CenteredSideValueBoxTransform slot =
				new CenteredSideValueBoxTransform((pump, side) -> side.getAxis() != pump.getValue(ElectricPumpBlock.FACING).getAxis());

		speedBehaviour = new ScrollValueBehaviour(CreateLang.translateDirect("generic.speed"), this, slot);
		speedBehaviour.between(0, AllConfigs.server().electricity.pumpRpmRange.get());
		speedBehaviour.value = 32;
		behaviours.add(speedBehaviour);
	}

	protected void applySpeed() {
		float target = active ? pumpSpeed : 0;
		if (target == speed) return;
		float prevSpeed = speed;
		speed = target;
		onSpeedChanged(prevSpeed);

		sendData();
	}

	@Override
	public float getGeneratedSpeed() {
		return active ? pumpSpeed : 0;
	}

	@Override
	public void attachKinetics() {
		updateSpeed = false;
	}

	@Override
	public void setSource(BlockPos source) {
	}

	@Override
	public float calculateStressApplied() {
		return 0f;
	}

	@Override
	public float calculateAddedStressCapacity() {
		return 0f;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.consumption").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" " + ElectricityUtil.format(getEnergyConsumptionRate(pumpSpeed)) + "⚡/t ")
				.withStyle(ChatFormatting.AQUA).append(CreateLang.translateDirect("gui.goggles.at_current_speed").withStyle(ChatFormatting.DARK_GRAY))).forGoggles(tooltip);
		return true;
	}

	public static int getEnergyConsumptionRate(float speed) {
		float magnitude = Math.abs(speed);
		if (magnitude <= 0) return 0;
		float effectiveSpeed = Math.max(magnitude, 32);
		return (int) Math.max(1, Math.round(AllConfigs.server().electricity.pumpFeAtMaxRpm.get() * effectiveSpeed / 256d));
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.read(tag);
		active = tag.getBooleanOr("Active", false);
	}

	@Override
	protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(tag, registries, clientPacket);
		energy.write(tag);
		tag.putBoolean("Active", active);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		energy.write(tag);
		tag.putBoolean("Active", active);
	}

	@Override
	public void tick() {
		super.tick();

		float behaviourSpeed = speedBehaviour.getValue();
		if (behaviourSpeed != pumpSpeed) {
			pumpSpeed = behaviourSpeed;
			applySpeed();
		}

		if (level.isClientSide()) return;

		int con = getEnergyConsumptionRate(pumpSpeed);
		boolean powered = getBlockState().getValue(ElectricPumpBlock.POWERED);
		boolean shouldRun = !powered && con > 0 && energy.getEnergyStored() >= con;

		if (shouldRun) energy.internalConsumeEnergy(con);

		if (shouldRun != active) {
			active = shouldRun;
			applySpeed();
		}
	}

	public float getPumpSpeed() {
		return pumpSpeed;
	}

	public int getEnergyConsumption() {
		return getEnergyConsumptionRate(pumpSpeed);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		applySpeed();
	}


	public static float getPulseScale(boolean active, float pumpSpeed, float renderTime, float phaseOffsetTicks) {
		if (!active) return 1f;
		float rate = Math.max(32f, Math.abs(pumpSpeed)) / 32f;
		return 1f + Mth.sin((renderTime + phaseOffsetTicks) * 0.1f * rate) * 0.1f;
	}

	public static Quaternionf getFacingRotation(Direction facing) {
		return new Quaternionf().rotateTo(0, 1, 0, facing.getStepX(), facing.getStepY(), facing.getStepZ());
	}
}