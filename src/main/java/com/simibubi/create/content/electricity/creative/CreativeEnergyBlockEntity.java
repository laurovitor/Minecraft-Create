package com.simibubi.create.content.electricity.creative;

import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import com.simibubi.create.content.electricity.energy.CreativeEnergyStorage;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.logistics.crate.CrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

import java.util.EnumMap;
import java.util.EnumSet;

public class CreativeEnergyBlockEntity extends CrateBlockEntity {

	protected final CreativeEnergyStorage capability;

	private final EnumSet<Direction> invalidSides = EnumSet.allOf(Direction.class);
	private final EnumMap<Direction, BlockCapabilityCache<EnergyHandler, Direction>> cache = new EnumMap<>(Direction.class);
	
	public CreativeEnergyBlockEntity(BlockEntityType<?> tileEntityTypeIn, BlockPos pos, BlockState state) {
		super(tileEntityTypeIn, pos, state);
		capability = new CreativeEnergyStorage();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.Energy.BLOCK,
				AllBlockEntityTypes.CREATIVE_ENERGY.get(),
				(be, context) -> be.capability
		);
	}
	
	private boolean firstTickState = true;
	
	@Override
	public void tick() {
		super.tick();
		if (level == null) return;
		if (level.isClientSide()) return;
		if (firstTickState) firstTick();
		firstTickState = false;
		
		for (Direction d : Direction.values()) {
			EnergyHandler ies = cache.get(d).getCapability();
			if (ies == null) continue;
			EnergyTransfers.insert(ies, Integer.MAX_VALUE, false);
		}
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
}
