package com.simibubi.create.content.electricity.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Transactional {@link EnergyHandler} for storages that can only be changed in one go (for example an energy
 * network buffer). Inserts and extracts are recorded as pending amounts that roll back with their transaction
 * and are applied with {@link #applyInsert}/{@link #applyExtract} once the outermost transaction commits.
 */
public abstract class DeferredEnergyHandler implements EnergyHandler {

	private int pendingInsert;
	private int pendingExtract;
	private final Journal journal = new Journal();

	/** How much of {@code amount} the storage would accept now (Forge {@code receiveEnergy(amount, true)}). */
	protected abstract int simulateInsert(int amount);

	/** How much of {@code amount} the storage would give now (Forge {@code extractEnergy(amount, true)}). */
	protected abstract int simulateExtract(int amount);

	protected abstract void applyInsert(int amount);

	protected abstract void applyExtract(int amount);

	@Override
	public int insert(int amount, TransactionContext transaction) {
		if (amount <= 0)
			return 0;
		int accepted = Math.max(0, simulateInsert(pendingInsert + amount) - pendingInsert);
		accepted = Math.min(accepted, amount);
		if (accepted > 0) {
			journal.updateSnapshots(transaction);
			pendingInsert += accepted;
		}
		return accepted;
	}

	@Override
	public int extract(int amount, TransactionContext transaction) {
		if (amount <= 0)
			return 0;
		int given = Math.max(0, simulateExtract(pendingExtract + amount) - pendingExtract);
		given = Math.min(given, amount);
		if (given > 0) {
			journal.updateSnapshots(transaction);
			pendingExtract += given;
		}
		return given;
	}

	private class Journal extends SnapshotJournal<long[]> {
		@Override
		protected long[] createSnapshot() {
			return new long[] { pendingInsert, pendingExtract };
		}

		@Override
		protected void revertToSnapshot(long[] snapshot) {
			pendingInsert = (int) snapshot[0];
			pendingExtract = (int) snapshot[1];
		}

		@Override
		protected void onRootCommit(long[] originalState) {
			int in = pendingInsert;
			int out = pendingExtract;
			pendingInsert = 0;
			pendingExtract = 0;
			if (in > 0)
				applyInsert(in);
			if (out > 0)
				applyExtract(out);
		}
	}
}
