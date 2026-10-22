package com.simibubi.create.content.electricity.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Endless energy source: gives any amount, accepts nothing. */
public class CreativeEnergyStorage implements EnergyHandler {

	@Override
	public long getAmountAsLong() {
		return Integer.MAX_VALUE;
	}

	@Override
	public long getCapacityAsLong() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int insert(int amount, TransactionContext transaction) {
		return 0;
	}

	@Override
	public int extract(int amount, TransactionContext transaction) {
		return Math.max(0, amount);
	}
}
