package com.simibubi.create.content.electricity.energy;

import org.jetbrains.annotations.Nullable;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Bridges the simulate/execute style of the classic Forge Energy API, which the electricity code was
 * written against, to NeoForge's transactional {@link EnergyHandler}.
 * <p>
 * Each call runs in its own transaction, nested in the transaction already open on this thread if there is
 * one; simulated calls are rolled back, executed calls are committed.
 */
public final class EnergyTransfers {

	private EnergyTransfers() {}

	public static int insert(@Nullable EnergyHandler handler, int amount, boolean simulate) {
		if (handler == null || amount <= 0)
			return 0;
		try (Transaction tx = open()) {
			int inserted = handler.insert(amount, tx);
			if (!simulate)
				tx.commit();
			return inserted;
		}
	}

	public static int extract(@Nullable EnergyHandler handler, int amount, boolean simulate) {
		if (handler == null || amount <= 0)
			return 0;
		try (Transaction tx = open()) {
			int extracted = handler.extract(amount, tx);
			if (!simulate)
				tx.commit();
			return extracted;
		}
	}

	/** Whether the handler would give out energy right now (stand-in for Forge's {@code canExtract}). */
	public static boolean canExtract(@Nullable EnergyHandler handler) {
		return extract(handler, 1, true) > 0;
	}

	/** Whether the handler would accept energy right now (stand-in for Forge's {@code canReceive}). */
	public static boolean canReceive(@Nullable EnergyHandler handler) {
		return insert(handler, 1, true) > 0;
	}

	public static int stored(@Nullable EnergyHandler handler) {
		return handler == null ? 0 : handler.getAmountAsInt();
	}

	public static int capacity(@Nullable EnergyHandler handler) {
		return handler == null ? 0 : handler.getCapacityAsInt();
	}

	@SuppressWarnings("deprecation")
	private static Transaction open() {
		TransactionContext current = Transaction.getLifecycle() == Transaction.Lifecycle.OPEN
			? Transaction.getCurrentOpenedTransaction()
			: null;
		return Transaction.open(current);
	}
}
