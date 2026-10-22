package com.simibubi.create.content.electricity.accumulator;

import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.electricity.energy.IMultiTileEnergyContainer;
import com.simibubi.create.content.electricity.energy.InternalEnergyStorage;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.content.electricity.packet.EnergyNetworkPacketPayload;
import com.simibubi.create.content.electricity.packet.IObserveBlockEntity;
import com.simibubi.create.content.electricity.packet.ObservePacketPayload;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.redstone.thresholdSwitch.ThresholdSwitchObservable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public class ModularAccumulatorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiTileEnergyContainer, IObserveBlockEntity, ThresholdSwitchObservable {
	protected InternalEnergyStorage energyCapability;
	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected int width;
	protected int height;
	protected boolean updateCapability;

	private static final int SYNC_RATE = 8;
	protected int syncCooldown;
	protected boolean queuedSync;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<EnergyHandler, Direction>> cache = new EnumMap<>(Direction.class);
	// protected LazyOptional<ModularAccumulatorPeripheral> peripheral;

	public ModularAccumulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		energyCapability = createEnergyStorage();
		updateConnectivity = false;
		height = 1;
		width = 1;
		updateCapability = false;
		refreshCapability();

		// if (CreateAddition.CC_ACTIVE) this.peripheral = LazyOptional.of(() -> Peripherals.createModularAccumulatorPeripheral(this));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Energy.BLOCK,
				AllBlockEntityTypes.MODULAR_ACCUMULATOR.get(),
				(be, context) -> {
					if (be.energyCapability == null)
						be.refreshCapability();
					return be.energyCapability;
				}
		);
	}

	@Override
	public void onChunkUnloaded() {}

	protected InternalEnergyStorage createEnergyStorage() {
		return new InternalEnergyStorage(getCapacityMultiplier(), AllConfigs.server().electricity.accumulatorMaxInput.get(), AllConfigs.server().electricity.accumulatorMaxOutput.get());
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

	protected void updateConnectivity() {
		updateConnectivity = false;
		if (level == null) return;
		if (level.isClientSide) return;
		if (!level.isLoaded(getBlockPos())) return;
		if (!isController()) return;
		AccumulatorConnectivityHandler.formMulti(this);
	}

	public LerpedFloat gauge = LerpedFloat.linear();

	int lastEnergy = 0;
	int energyChangeTick = 0;
	@Override
	public void tick() {
		super.tick();
		if(!invalidSides.isEmpty()) {
			updateCache();
			invalidSides.clear();
		}

		tickOutput();

		if (syncCooldown > 0) {
			syncCooldown--;
			if (syncCooldown == 0 && queuedSync) sendData();
		}

		if (lastKnownPos == null) lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
			onPositionChanged();
			return;
		}

		if (updateCapability) {
			updateCapability = false;
			refreshCapability();
		}

		if (updateConnectivity) updateConnectivity();

		// Tick Logic:
		if (!isController()) return;

		if(Math.abs(lastEnergy - energyCapability.getEnergyStored()) > 256) {
			lastEnergy = energyCapability.getEnergyStored();
			onEnergyChanged();
		}

		if (energyChangeTick > 0) energyChangeTick--;

		if (level == null) return;
		if (level.isClientSide()) {
			CatnipServices.PLATFORM.executeOnClientOnly(() -> this::tickAudio);
			gauge.tickChaser();
			float current = gauge.getValue(1);
			if (current > 1 && Create.RANDOM.nextFloat() < 1 / 2f)
				gauge.setValueNoUpdate(current + Math.min(-(current - 1) * Create.RANDOM.nextFloat(), 0));
		}
	}

	public void tickOutput() {
		if (getControllerBE() == null) return;
		BlockState state = this.getBlockState();
		if (state.getValue(ModularAccumulatorBlock.TOP)) tickOutputSide(Direction.UP);
		if (state.getValue(ModularAccumulatorBlock.BOTTOM)) tickOutputSide(Direction.DOWN);
	}

	public void tickOutputSide(Direction side) {
		if (level == null) return;
		if (!level.isLoaded(getBlockPos())) return;
		if (!level.isLoaded(getBlockPos().relative(side))) return;
		var sideCache = cache.get(side);
		if (sideCache == null) return;
		EnergyHandler ies = sideCache.getCapability();
		if(ies == null) return;
		int ext = getControllerBE().energyCapability.extractEnergy(EnergyTransfers.insert(ies, AllConfigs.server().electricity.accumulatorMaxOutput.get(), true), false);
		EnergyTransfers.insert(ies, ext, false);
	}

	public void tickAudio() {
		if (energyChangeTick == 0) return;
		int sizeInBlocks = getTotalAccumulatorSize();
		float pitch = 0.75f;
		if (sizeInBlocks < 4) pitch = 1.25f;
		if (sizeInBlocks < 9) pitch = 1f;
		SoundScapes.play(SoundScapes.AmbienceGroup.CHARGE, worldPosition, pitch);
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.getX() == controller.getX()
			&& worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
	}

	@Override
	public void initialize() {
		super.initialize();
		sendData();
		if (level == null) return;
		if (level.isClientSide) invalidateRenderBoundingBox();
	}

	private void onPositionChanged() {
		removeController(true);
		lastKnownPos = worldPosition;
	}

	protected void onEnergyChanged() {
		if (level == null) return;
		if (!level.isLoaded(getBlockPos())) return;
		if (!hasLevel()) return;

		energyChangeTick = 20;

		for (int yOffset = 0; yOffset < height; yOffset++) {
			for (int xOffset = 0; xOffset < width; xOffset++) {
				for (int zOffset = 0; zOffset < width; zOffset++) {
					BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
					if (!level.isLoaded(pos)) return;
					ModularAccumulatorBlockEntity acc = AccumulatorConnectivityHandler.partAt(getType(), level, pos);
					if (acc == null) continue;
					level.updateNeighbourForOutputSignal(pos, acc.getBlockState().getBlock());
				}
			}
		}

		if (!level.isClientSide) {
			setChanged();
			sendData();
		}
	}


	@SuppressWarnings("unchecked")
	@Override
	public @Nullable ModularAccumulatorBlockEntity getControllerBE() {
		if (isController()) return this;
		if (level == null) return this;
		if (!level.isLoaded(getBlockPos())) return this;
		BlockEntity tileEntity = level.getBlockEntity(controller);
		if (tileEntity instanceof ModularAccumulatorBlockEntity)
			return (ModularAccumulatorBlockEntity) tileEntity;
		return null;
	}

	public void applySize(int blocks) {
		energyCapability.setCapacity(blocks * getCapacityMultiplier());
		int overflow = energyCapability.getEnergyStored() - energyCapability.getMaxEnergyStored();
		if (overflow > 0) energyCapability.extractEnergy(overflow, false);
	}

	public void removeController(boolean keepEnergy) {
		if (level == null) return;
		if (level.isClientSide) return;
		if (!level.isLoaded(getBlockPos())) return;
		updateConnectivity = true;
		if (!keepEnergy) applySize(1);
		controller = null;
		width = 1;
		height = 1;
		//boiler.clear();
		onEnergyChanged();

		BlockState state = getBlockState();
		if (ModularAccumulatorBlock.isAccumulator(state)) {
			state = state.setValue(ModularAccumulatorBlock.BOTTOM, true);
			state = state.setValue(ModularAccumulatorBlock.TOP, true);
			getLevel().setBlock(worldPosition, state, 22);
		}

		refreshCapability();
		setChanged();
		sendData();
	}

	public void sendDataImmediately() {
		syncCooldown = 0;
		queuedSync = false;
		sendData();
	}

	@Override
	public void sendData() {
		if (syncCooldown > 0) {
			queuedSync = true;
			return;
		}
		super.sendData();
		queuedSync = false;
		syncCooldown = SYNC_RATE;
	}

	@Override
	public void setController(BlockPos controller) {
		if (level == null) return;
		if (level.isClientSide && !isVirtual()) return;
		if (controller.equals(this.controller)) return;
		this.controller = controller;
		refreshCapability();
		setChanged();
		sendData();
	}

	void refreshCapability() {
		energyCapability = handlerForCapability();
		invalidateCapabilities();
	}

	private InternalEnergyStorage handlerForCapability() {
		if (isController()) return energyCapability;
		ModularAccumulatorBlockEntity controllerBE = getControllerBE();
		if (controllerBE != null && controllerBE != this) {
			return controllerBE.handlerForCapability();
		}
		return new InternalEnergyStorage(0, AllConfigs.server().electricity.accumulatorMaxInput.get(), AllConfigs.server().electricity.accumulatorMaxOutput.get());
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}


	@Override
	protected AABB createRenderBoundingBox() {
		if (isController()) return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
		else return super.createRenderBoundingBox();
	}

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);

		BlockPos controllerBefore = controller;
		int prevSize = width;
		int prevHeight = height;

		updateConnectivity = tag.contains("Uninitialized");
		controller = null;
		lastKnownPos = null;

		if (tag.contains("LastKnownPos")) lastKnownPos = NbtUtils.readBlockPos(tag, "LastKnownPos").orElse(BlockPos.ZERO);
		if (tag.contains("Controller")) controller = NbtUtils.readBlockPos(tag, "Controller").orElse(BlockPos.ZERO);

		if (isController()) {
			width = tag.getInt("Size");
			height = tag.getInt("Height");
			energyCapability.setCapacity(getTotalAccumulatorSize() * getCapacityMultiplier());
			energyCapability.read(tag.getCompound("EnergyContent"));
			if (energyCapability.getSpace() < 0) energyCapability.extractEnergy(-energyCapability.getSpace(), true);
		}

		updateCapability = true;

		if (!clientPacket) return;

		boolean changeOfController = controllerBefore == null ? controller != null : !controllerBefore.equals(controller);
		if (changeOfController || prevSize != width || prevHeight != height) {
			if (hasLevel()) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
			if (isController()) energyCapability.setCapacity(getCapacityMultiplier() * getTotalAccumulatorSize());
			invalidateRenderBoundingBox();
		}

		if (isController()) gauge.chase(getFillState(), 0.125f, LerpedFloat.Chaser.EXP);
	}

	public float getFillState() {
		return (float) energyCapability.getEnergyStored() / energyCapability.getMaxEnergyStored();
	}

	@Override
	public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		if (updateConnectivity) tag.putBoolean("Uninitialized", true);
		if (lastKnownPos != null) tag.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController()) tag.put("Controller", NbtUtils.writeBlockPos(controller));
		if (isController()) {
			tag.put("EnergyContent", energyCapability.write(new CompoundTag()));
			// Used by contraptions.
			tag.putInt("EnergyCapacity", getTotalAccumulatorSize() * getCapacityMultiplier());
			tag.putInt("Size", width);
			tag.putInt("Height", height);
		}
		super.writeSafe(tag, registries);

		if (!clientPacket) return;
		if (queuedSync) tag.putBoolean("LazySync", true);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		if (isController()) {
			tag.putInt("Size", width);
			tag.putInt("Height", height);
		}
	}

	public int getTotalAccumulatorSize() {
		return width * width * height;
	}

	public static int getCapacityMultiplier() {
		return AllConfigs.server().electricity.accumulatorCapacity.get();
	}

	public static int getMaxHeight() {
		return AllConfigs.server().electricity.accumulatorMaxHeight.get();
	}

	@Override
	public int getMaxWidth() {
		return AllConfigs.server().electricity.accumulatorMaxWidth.get();
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = this.getBlockState();
		if (ModularAccumulatorBlock.isAccumulator(state)) { // safety
			state = state.setValue(ModularAccumulatorBlock.BOTTOM, getController().getY() == getBlockPos().getY());
			state = state.setValue(ModularAccumulatorBlock.TOP, getController().getY() + height - 1 == getBlockPos().getY());
			if (level == null) return;
			level.setBlock(getBlockPos(), state, Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
		}
		setChanged();
		if (isController()) sendDataImmediately();
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y) return getMaxHeight();
		return getMaxWidth();
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ModularAccumulatorBlockEntity controllerTE = getControllerBE();
		if (controllerTE == null) return false;

		ObservePacketPayload.send(getController(), 0);

		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.accumulator.info").withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);
		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.stored").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" ")
				.append(ElectricityUtil.format((int)EnergyNetworkPacketPayload.clientBuff)).append("⚡").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip);

		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.capacity").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" ")
				.append(ElectricityUtil.format((int)controllerTE.energyCapability.getMaxEnergyStored())).append("⚡").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip);
		return true;
	}

	public void observe() {}

	@Override
	public void onObserved(ServerPlayer player, ObservePacketPayload pack) {
		ModularAccumulatorBlockEntity controllerTE = getControllerBE();
		if (controllerTE == null) return;

		EnergyNetworkPacketPayload.send(worldPosition, 0, controllerTE.energyCapability.getEnergyStored(), player);
	}

	public boolean hasAccumulator() {
		return true;
	}

	public int getSize(int accumulator) {
		return getCapacityMultiplier();
	}

	public void setSize(int accumulator, int blocks) {
		applySize(blocks);
	}

	public InternalEnergyStorage getEnergy() {
		return energyCapability;
	}

	@Override
	public int getMaxValue() {
		return 100;
	}

	@Override
	public int getMinValue() {
		return 0;
	}

	@Override
	public int getCurrentValue() {
		ModularAccumulatorBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null) return 0;
		return (int)((float)controllerBE.energyCapability.getEnergyStored() / (float)controllerBE.energyCapability.getMaxEnergyStored() * 100f);
	}

	@Override
	public MutableComponent format(int i) {
		return Component.literal(i + "%");
	}
}
