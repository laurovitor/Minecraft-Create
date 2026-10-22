package com.simibubi.create.content.electricity.portable;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class PortableEnergyInterfaceBlockEntity extends PortableStorageInterfaceBlockEntity {
    private EnergyHandler capability = new InterfaceEnergyHandler(new SimpleEnergyHandler(0));
    public PortableEnergyInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, AllBlockEntityTypes.PORTABLE_ENERGY_INTERFACE.get(), (be,side) -> be.capability);
    }
    @Override public void startTransferringTo(Contraption contraption, float distance) {
        EnergyHandler handler = PortableEnergyManager.get(contraption);
        capability = new InterfaceEnergyHandler(handler != null ? handler : new SimpleEnergyHandler(0));
        invalidateCapability();
        super.startTransferringTo(contraption, distance);
    }
    @Override protected void invalidateCapability() { invalidateCapabilities(); }
    @Override protected void stopTransferring() {
        capability = new InterfaceEnergyHandler(new SimpleEnergyHandler(0));
        invalidateCapability();
        super.stopTransferring();
    }
    private class InterfaceEnergyHandler implements EnergyHandler {
        private final EnergyHandler wrapped;
        InterfaceEnergyHandler(EnergyHandler wrapped) { this.wrapped = wrapped; }
        @Override public long getAmountAsLong() { return wrapped.getAmountAsLong(); }
        @Override public long getCapacityAsLong() { return wrapped.getCapacityAsLong(); }
        @Override public int insert(int amount, TransactionContext tx) {
            if (!canTransfer()) return 0;
            int inserted = wrapped.insert(Math.min(amount, AllConfigs.server().electricity.peiMaxInput.get()), tx);
            if (inserted > 0) onContentTransferred();
            return inserted;
        }
        @Override public int extract(int amount, TransactionContext tx) {
            if (!canTransfer()) return 0;
            int extracted = wrapped.extract(Math.min(amount, AllConfigs.server().electricity.peiMaxOutput.get()), tx);
            if (extracted > 0) onContentTransferred();
            return extracted;
        }
    }
}
