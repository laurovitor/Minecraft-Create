package com.simibubi.create.content.electricity.alternator;

import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.electricity.energy.IEnergyProvider;
import com.simibubi.create.content.electricity.energy.InternalEnergyStorage;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import javax.annotation.Nullable;

public class AlternatorBlockEntity extends KineticBlockEntity implements IEnergyProvider {

	protected final InternalEnergyStorage energy;
	private final EnergyHandler capability;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<EnergyHandler, Direction>> cache = new EnumMap<>(Direction.class);

	public AlternatorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
		super(typeIn, pos, state);
		energy = new InternalEnergyStorage(AllConfigs.server().electricity.alternatorCapacity.get(), 0, AllConfigs.server().electricity.alternatorMaxOutput.get());
		capability = energy;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Energy.BLOCK,
				AllBlockEntityTypes.ALTERNATOR.get(),
				(be, context) -> be.capability
		);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
		// CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.alternator.info").withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);
		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.production").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" " + ElectricityUtil.format(getEnergyProductionRate((int) (isSpeedRequirementFulfilled() ? getSpeed() : 0))) + "⚡/t ")
				.withStyle(ChatFormatting.AQUA).append(CreateLang.translateDirect("gui.goggles.at_current_speed").withStyle(ChatFormatting.DARK_GRAY))).forGoggles(tooltip);
		return true;
	}


	public boolean isEnergyInput(Direction side) {
		return false;
	}

	public boolean isEnergyOutput(Direction side) {
		return true; //side != getBlockState().getValue(AlternatorBlock.FACING);
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		energy.read(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		energy.write(tag);
	}

	private boolean firstTickState = true;

	@Override
	public void tick() {
		super.tick();
		if (level == null) return;
		if (level.isClientSide()) return;
		if (firstTickState) firstTick();
		firstTickState = false;

		if (Math.abs(getSpeed()) > 0 && isSpeedRequirementFulfilled())
			energy.internalProduceEnergy(getEnergyProductionRate((int)getSpeed()));

		for (Direction d : Direction.values()) {
			if(!isEnergyOutput(d)) continue;
			EnergyHandler ies = cache.get(d).getCapability();
			if(ies == null) continue;
			int ext = energy.extractEnergy(EnergyTransfers.insert(ies, AllConfigs.server().electricity.alternatorMaxOutput.get(), true), false);
			EnergyTransfers.insert(ies, ext, false);
		}
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void tickAudio() {
		super.tickAudio();

		float componentSpeed = Math.abs(getSpeed());
		if (componentSpeed == 0 || !isSpeedRequirementFulfilled())
			return;

		float pitch = Mth.clamp((componentSpeed / 256f) + .5f, .5f, 1.5f);
		SoundScapes.play(SoundScapes.AmbienceGroup.DYNAMO, worldPosition, pitch);
	}

	public static int getEnergyProductionRate(int rpm) {
		rpm = Math.abs(rpm);
		return (int)((double) AllConfigs.server().electricity.feAtMaxRpm.get() * ((double)Math.abs(rpm) / 256d) * AllConfigs.server().electricity.alternatorEfficiency.get());//return (int)((double)Config.FE_TO_SU.get() * ((double)Math.abs(rpm)/256d) * EFFICIENCY);
	}

	@Override
	protected Block getStressConfigKey() {
		return AllBlocks.ALTERNATOR.get();
	}

	public void firstTick() {
		updateCache();
	}

	public void updateCache() {
		if (level == null) return;
		if (level.isClientSide()) return;
		for (Direction side : Direction.values()) {
			cache.put(side, BlockCapabilityCache.create(
					Capabilities.Energy.BLOCK,
					(ServerLevel) level,
					getBlockPos().relative(side),
					side.getOpposite(),
					() -> !this.isRemoved(),
					() -> invalidSides.add(side)
			));
		}
	}

	@Override
	public EnergyHandler getEnergyStorage(@Nullable Direction direction) {
		return energy;
	}
}
