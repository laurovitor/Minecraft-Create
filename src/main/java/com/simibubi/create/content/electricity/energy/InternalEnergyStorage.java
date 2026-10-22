package com.simibubi.create.content.electricity.energy;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

/**
 * Energy buffer of an electric block entity. Exposed to other mods as a transactional
 * {@link net.neoforged.neoforge.transfer.energy.EnergyHandler} (FE semantics), and to Create's own code
 * through the classic simulate-style helpers of {@link EnergyTransfers}.
 */
public class InternalEnergyStorage extends SimpleEnergyHandler {

	public InternalEnergyStorage(int capacity) {
		this(capacity, capacity, capacity);
	}

	public InternalEnergyStorage(int capacity, int maxTransfer) {
		this(capacity, maxTransfer, maxTransfer);
	}

	public InternalEnergyStorage(int capacity, int maxReceive, int maxExtract) {
		super(Math.max(0, capacity), Math.max(0, maxReceive), Math.max(0, maxExtract), 0);
	}

	public CompoundTag write(CompoundTag nbt) {
		nbt.putInt("energy", energy);
		return nbt;
	}

	public void read(CompoundTag nbt) {
		setEnergy(nbt.getIntOr("energy", 0));
	}

	public CompoundTag write(CompoundTag nbt, String name) {
		nbt.putInt("energy_" + name, energy);
		return nbt;
	}

	public void read(CompoundTag nbt, String name) {
		setEnergy(nbt.getIntOr("energy_" + name, 0));
	}

	public int getEnergyStored() {
		return energy;
	}

	public int getMaxEnergyStored() {
		return capacity;
	}

	public int getSpace() {
		return Math.max(capacity - energy, 0);
	}

	public boolean canExtract() {
		return maxExtract > 0;
	}

	public boolean canReceive() {
		return maxInsert > 0;
	}

	/** Inserts energy respecting the transfer limit, like Forge's {@code receiveEnergy}. */
	public int receiveEnergy(int amount, boolean simulate) {
		return EnergyTransfers.insert(this, amount, simulate);
	}

	/** Extracts energy respecting the transfer limit, like Forge's {@code extractEnergy}. */
	public int extractEnergy(int amount, boolean simulate) {
		return EnergyTransfers.extract(this, amount, simulate);
	}

	/** Consumes energy for the owner's own work, ignoring the transfer limit. */
	public int internalConsumeEnergy(int consume) {
		int old = energy;
		energy = Math.max(0, energy - Math.max(0, consume));
		return old - energy;
	}

	/** Produces energy from the owner's own work, ignoring the transfer limit. */
	public int internalProduceEnergy(int produce) {
		int old = energy;
		energy = Math.min(capacity, energy + Math.max(0, produce));
		return old - energy;
	}

	public void setEnergy(int energy) {
		this.energy = Math.max(0, energy);
	}

	public void setCapacity(int capacity) {
		this.capacity = Math.max(0, capacity);
	}

	@Override
	public String toString() {
		return energy + "/" + capacity + " <-" + maxExtract + " ->" + maxInsert;
	}
}
