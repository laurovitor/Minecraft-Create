package com.simibubi.create.content.electricity.energy;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public abstract class AbstractElectricBlockEntity extends SmartBlockEntity {

	protected final InternalEnergyStorage localEnergy;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<EnergyHandler, Direction>> escacheMap = new EnumMap<>(Direction.class);

	public AbstractElectricBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);
		localEnergy = new InternalEnergyStorage(getCapacity(), getMaxIn(), getMaxOut());
		setLazyTickRate(20);
	}

	public abstract int getCapacity();
	public abstract int getMaxIn();
	public abstract int getMaxOut();

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

	public abstract boolean isEnergyInput(Direction side);
	public abstract boolean isEnergyOutput(Direction side);

	@Override
	protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(tag, registries, clientPacket);
		localEnergy.read(tag);
	}

	@Override
	public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
		super.writeSafe(tag, registries);
		localEnergy.write(tag);
	}


	@Override
	public void tick() {
		super.tick();
		if(!invalidSides.isEmpty()) {
			invalidSides.forEach(this::updateCache);
			invalidSides.clear();
		}
	}

	public void updateCache(Direction side) {
		if (level == null) return;
		if (level.isClientSide()) return;
		if (!level.isLoaded(worldPosition.relative(side))) {
			escacheMap.put(side, null);
			return;
		}

		var cache = BlockCapabilityCache.create(
				Capabilities.Energy.BLOCK, // capability to cache
				(ServerLevel) level, // level
				getBlockPos().relative(side),
				side.getOpposite(),
				() -> !this.isRemoved(), // validity check (because the cache might outlive the object it belongs to)
				() -> { invalidSides.add(side); } // invalidation listener
		);
		escacheMap.put(side, cache);
	}
}
