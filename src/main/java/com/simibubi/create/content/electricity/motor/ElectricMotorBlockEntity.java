package com.simibubi.create.content.electricity.motor;

import java.util.List;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.electricity.energy.InternalEnergyStorage;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.KineticScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public class ElectricMotorBlockEntity extends GeneratingKineticBlockEntity {
	protected float motorSpeed;
	protected ScrollValueBehaviour generatedSpeed;
	protected final InternalEnergyStorage energy;
	private final EnergyHandler capability;

	private boolean cc_update_rpm = false;
	private float cc_new_rpm = 32.0f;

	private boolean active = false;

	public ElectricMotorBlockEntity(BlockEntityType<? extends ElectricMotorBlockEntity> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energy = new InternalEnergyStorage(AllConfigs.server().electricity.motorCapacity.get(), AllConfigs.server().electricity.motorMaxInput.get(), 0);
		capability = energy;
		setLazyTickRate(20);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Energy.BLOCK,
				AllBlockEntityTypes.ELECTRIC_MOTOR.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);

		CenteredSideValueBoxTransform slot =
			new CenteredSideValueBoxTransform((motor, side) -> motor.getValue(ElectricMotorBlock.FACING) == side.getOpposite());

		generatedSpeed = new KineticScrollValueBehaviour(CreateLang.translateDirect("generic.speed"), this, slot);
		generatedSpeed.between(-AllConfigs.server().electricity.motorRpmRange.get(), AllConfigs.server().electricity.motorRpmRange.get());
		generatedSpeed.value = 32;
		//generatedSpeed.withUnit(i -> Lang.translateDirect("generic.unit.rpm"));
		generatedSpeed.withCallback(this::updateGeneratedRotation);
		//generatedSpeed.withStepFunction(ElectricMotorTileEntity::step);
		behaviours.add(generatedSpeed);
	}

	public static int step(ScrollValueBehaviour.StepContext context) {
		int current = context.currentValue;
		int step = 1;

		if (!context.shift) {
			int magnitude = Math.abs(current) - (context.forward == current > 0 ? 0 : 1);

			if (magnitude >= 4) step *= 4;
			if (magnitude >= 32) step *= 4;
			if (magnitude >= 128) step *= 4;
		}

		return step;
	}


	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.consumption").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" " + ElectricityUtil.format(getEnergyConsumptionRate(generatedSpeed.getValue())) + "⚡/t ")
				.withStyle(ChatFormatting.AQUA).append(CreateLang.translateDirect("gui.goggles.at_current_speed").withStyle(ChatFormatting.DARK_GRAY))).forGoggles(tooltip);
		return true;
	}

	// This is the callback that is called by the ScrollValueBehaviour!
	public void updateGeneratedRotation(int rpm) {
		motorSpeed = rpm;
		super.updateGeneratedRotation();
	}

	@Override
	public void initialize() {
		super.initialize();
		if (!hasSource() || getGeneratedSpeed() > getTheoreticalSpeed())
			updateGeneratedRotation();
	}

	// This is the method that determines the absolute true output speed!
	@Override
	public float getGeneratedSpeed() {
		if (!AllBlocks.ELECTRIC_MOTOR.has(getBlockState()))
			return 0;
		return convertToDirection(active ? motorSpeed : 0, getBlockState().getValue(ElectricMotorBlock.FACING));
	}

	@Override
	protected Block getStressConfigKey() {
		return AllBlocks.ELECTRIC_MOTOR.get();
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.read(tag);
		active = tag.getBooleanOr("active", false);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		energy.write(tag);
		tag.putBoolean("active", active);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
	}

	public static int getEnergyConsumptionRate(float rpm) {
		return Math.abs(rpm) > 0 ? (int)Math.max((double) AllConfigs.server().electricity.feAtMaxRpm.get() * ((double)Math.abs(rpm) / 256d), (double) AllConfigs.server().electricity.motorMinConsumption.get()) : 0;
	}

	// CC
	boolean first = true;

	@Override
	public void tick() {
		super.tick();
		if(first) {
			motorSpeed = generatedSpeed.getValue();
			updateGeneratedRotation();
			first = false;
		}

		if(cc_update_rpm) {
			generatedSpeed.setValue(Math.round(cc_new_rpm));
			motorSpeed = cc_new_rpm;
			cc_update_rpm = false;
			updateGeneratedRotation();
		}

		//Old Lazy
		if(level.isClientSide()) return;
		int con = getEnergyConsumptionRate(motorSpeed);
		if(!active) {
			if(energy.getEnergyStored() > con * 2 && !getBlockState().getValue(ElectricMotorBlock.POWERED)) {
				active = true;
				updateGeneratedRotation();
			}
		}
		else {
			int ext = energy.internalConsumeEnergy(con);
			if(ext < con || getBlockState().getValue(ElectricMotorBlock.POWERED)) {
				active = false;
				updateGeneratedRotation();
			}
		}
	}

	@Override
	public void tickAudio() {
		super.tickAudio();
		if (!active) return;
		SoundScapes.play(SoundScapes.AmbienceGroup.DYNAMO, worldPosition, 1);
	}

	
	public static float getDurationAngle(float deg, float initialProgress, float speed) {
		speed = Math.abs(speed);
		deg = Math.abs(deg);
		if(speed < 0.1f) return 0;
		double degreesPerTick = (speed * 360) / 60 / 20;
		return (float) ((1 - initialProgress) * deg / degreesPerTick + 1);
	}

	public static float getDurationDistance(float dis, float initialProgress, float speed) {
		speed = Math.abs(speed);
		dis = Math.abs(dis);
		if(speed < 0.1f) return 0;
		double metersPerTick = speed / 512;
		return (float) ((1 - initialProgress) * dis / metersPerTick);
	}

	// Programmatic RPM setter (was the ComputerCraft peripheral callback upstream; CC compat is not ported).
	public boolean setRPM(float rpm) {
		if (!Float.isFinite(rpm)) return false;
		rpm = Math.max(Math.min(rpm, AllConfigs.server().electricity.motorRpmRange.get()), -AllConfigs.server().electricity.motorRpmRange.get());
		cc_new_rpm = rpm;
		cc_update_rpm = true;
		return true;
	}

	public float getRPM() {
		return motorSpeed;
	}

	public int getGeneratedStress() {
		return (int) calculateAddedStressCapacity();
	}

	public int getEnergyConsumption() {
		return getEnergyConsumptionRate(motorSpeed);
	}

	public boolean isPoweredState() {
		return getBlockState().getValue(ElectricMotorBlock.POWERED);
	}
}
