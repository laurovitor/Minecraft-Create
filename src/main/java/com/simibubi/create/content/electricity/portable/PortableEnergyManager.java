package com.simibubi.create.content.electricity.portable;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;

/** Contraption accumulator graph, committed to moving block-entity NBT on transaction commit. */
public final class PortableEnergyManager {
    private static final Map<UUID, EnergyStorageHolder> CONTRAPTIONS = new ConcurrentHashMap<>();
    private PortableEnergyManager() {}
    public static void track(MovementContext context) {
        if (context.contraption.entity == null) return;
        cleanup();
        CONTRAPTIONS.computeIfAbsent(context.contraption.entity.getUUID(), id -> new EnergyStorageHolder())
            .addEnergySource(context.blockEntityData, context.localPos);
    }
    public static void untrack(MovementContext context) {
        if (context.contraption.entity != null) CONTRAPTIONS.remove(context.contraption.entity.getUUID());
    }
    public static @Nullable EnergyHandler get(Contraption contraption) {
        if (contraption.entity == null) return null;
        cleanup();
        return CONTRAPTIONS.get(contraption.entity.getUUID());
    }
    private static void cleanup() {
        long now = System.currentTimeMillis();
        CONTRAPTIONS.values().removeIf(holder -> now - holder.heartbeat > 5000);
    }
    public static class EnergyStorageHolder extends SimpleEnergyHandler {
        private long heartbeat = System.currentTimeMillis();
        private final Map<BlockPos, EnergyData> holders = new LinkedHashMap<>();
        EnergyStorageHolder() {
            super(0, AllConfigs.server().electricity.accumulatorMaxInput.get(),
                AllConfigs.server().electricity.accumulatorMaxOutput.get(), 0);
        }
        void addEnergySource(CompoundTag nbt, BlockPos pos) {
            heartbeat = System.currentTimeMillis();
            if (!nbt.contains("EnergyContent") || holders.containsKey(pos)) return;
            EnergyData data = new EnergyData(nbt);
            holders.put(pos, data);
            capacity += data.capacity;
            energy += data.energy;
        }
        @Override protected void onEnergyChanged(int previousAmount) {
            int delta = energy - previousAmount;
            if (delta > 0) {
                for (EnergyData data : holders.values()) { delta -= data.receive(delta); if (delta <= 0) break; }
            } else if (delta < 0) {
                int remaining = -delta;
                for (EnergyData data : holders.values()) { remaining -= data.extract(remaining); if (remaining <= 0) break; }
            }
        }
    }
    private static class EnergyData {
        final CompoundTag content;
        final int capacity;
        int energy;
        EnergyData(CompoundTag nbt) {
            content = nbt.getCompoundOrEmpty("EnergyContent");
            capacity = Math.max(0, nbt.getIntOr("EnergyCapacity", 0));
            energy = Math.max(0, content.getIntOr("energy", 0));
        }
        int receive(int amount) {
            int moved = Math.min(Math.max(0, capacity - energy), amount);
            energy += moved; content.putInt("energy", energy); return moved;
        }
        int extract(int amount) {
            int moved = Math.min(energy, amount);
            energy -= moved; content.putInt("energy", energy); return moved;
        }
    }
}
