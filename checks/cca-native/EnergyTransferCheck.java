package com.simibubi.create.content.electricity.energy;

import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * FE semantics of the imported electricity storages on NeoForge's transactional EnergyHandler.
 * Run with java -ea against the built classes and the NeoForge universal jar:
 *
 * <pre>
 * CP=build/classes/java/main:$(find ~/.gradle/caches -name 'neoforge-26.3.0.89-universal.jar' | head -1)
 * javac -d /tmp/cca-energy -cp "$CP" checks/cca-native/EnergyTransferCheck.java
 * java -ea -cp "/tmp/cca-energy:$CP" com.simibubi.create.content.electricity.energy.EnergyTransferCheck
 * </pre>
 */
public class EnergyTransferCheck {

	static void eq(long expected, long actual, String what) {
		assert expected == actual : what + ": expected " + expected + " got " + actual;
	}

	static void internalStorage() {
		InternalEnergyStorage s = new InternalEnergyStorage(1000, 100, 50);
		eq(100, s.receiveEnergy(500, true), "simulated insert capped by maxReceive");
		eq(0, s.getEnergyStored(), "simulation does not change the buffer");
		eq(100, s.receiveEnergy(500, false), "insert capped by maxReceive");
		eq(100, s.getEnergyStored(), "insert committed");
		eq(50, s.extractEnergy(500, false), "extract capped by maxExtract");
		eq(50, s.getEnergyStored(), "extract committed");
		for (int i = 0; i < 20; i++)
			s.receiveEnergy(100, false);
		eq(1000, s.getEnergyStored(), "never above capacity");
		eq(0, s.receiveEnergy(1, false), "full buffer accepts nothing");
		eq(0, s.getSpace(), "no space when full");
		eq(1000, s.internalConsumeEnergy(5000), "own work ignores transfer limit, stops at zero");
		eq(0, s.getEnergyStored(), "consumed to zero");
		s.internalProduceEnergy(5000);
		eq(1000, s.getEnergyStored(), "own production ignores transfer limit, stops at capacity");
		eq(0, s.receiveEnergy(-5, false), "negative insert is a no-op");
		eq(0, s.extractEnergy(0, false), "zero extract is a no-op");

		InternalEnergyStorage noExtract = new InternalEnergyStorage(500, 500, 0);
		noExtract.receiveEnergy(300, false);
		assert !noExtract.canExtract() && noExtract.canReceive();
		eq(0, noExtract.extractEnergy(100, false), "motor-like buffer refuses extraction");
		assert !EnergyTransfers.canExtract(noExtract);
		assert EnergyTransfers.canReceive(noExtract);
		eq(300, EnergyTransfers.stored(noExtract), "stored via the generic handler view");
		eq(500, EnergyTransfers.capacity(noExtract), "capacity via the generic handler view");
		eq(0, EnergyTransfers.insert(null, 10, false), "null handler");

		var nbt = new net.minecraft.nbt.CompoundTag();
		noExtract.write(nbt);
		InternalEnergyStorage copy = new InternalEnergyStorage(500, 500, 0);
		copy.read(nbt);
		eq(300, copy.getEnergyStored(), "NBT round trip");
	}

	static void transactions() {
		InternalEnergyStorage s = new InternalEnergyStorage(1000);
		// Another mod pushing energy in an outer transaction that is aborted: nothing sticks,
		// even though our helpers commit their own nested transaction.
		try (Transaction outer = Transaction.openRoot()) {
			eq(400, EnergyTransfers.insert(s, 400, false), "nested insert");
			eq(400, s.getEnergyStored(), "visible inside the outer transaction");
		}
		eq(0, s.getEnergyStored(), "outer abort rolls the nested commit back");
		try (Transaction outer = Transaction.openRoot()) {
			s.insert(250, outer);
			outer.commit();
		}
		eq(250, s.getEnergyStored(), "direct transactional insert committed");
		try (Transaction outer = Transaction.openRoot()) {
			eq(250, s.extract(1000, outer), "extract what is stored");
		}
		eq(250, s.getEnergyStored(), "aborted extract restored");
	}

	static void creative() {
		EnergyHandler c = new CreativeEnergyStorage();
		eq(12345, EnergyTransfers.extract(c, 12345, false), "creative gives anything");
		eq(0, EnergyTransfers.insert(c, 100, false), "creative accepts nothing");
		eq(Integer.MAX_VALUE, EnergyTransfers.stored(c), "creative reports full");
	}

	static class Buffer extends DeferredEnergyHandler {
		int stored, cap;
		int applies;

		Buffer(int stored, int cap) {
			this.stored = stored;
			this.cap = cap;
		}

		@Override
		protected int simulateInsert(int amount) {
			return Math.min(amount, cap - stored);
		}

		@Override
		protected int simulateExtract(int amount) {
			return Math.min(amount, stored);
		}

		@Override
		protected void applyInsert(int amount) {
			stored += amount;
			applies++;
		}

		@Override
		protected void applyExtract(int amount) {
			stored -= amount;
			applies++;
		}

		@Override
		public long getAmountAsLong() {
			return stored;
		}

		@Override
		public long getCapacityAsLong() {
			return cap;
		}
	}

	static void deferred() {
		Buffer b = new Buffer(0, 100);
		try (Transaction tx = Transaction.openRoot()) {
			eq(60, b.insert(60, tx), "first insert");
			eq(40, b.insert(60, tx), "pending inserts count against capacity");
			eq(0, b.insert(1, tx), "full including pending");
			eq(0, b.stored, "not applied before the root commit");
		}
		eq(0, b.stored, "aborted root applies nothing");
		eq(0, b.applies, "no apply on abort");
		try (Transaction tx = Transaction.openRoot()) {
			b.insert(70, tx);
			try (Transaction inner = Transaction.open(tx)) {
				b.insert(20, inner);
			} // inner aborted
			tx.commit();
		}
		eq(70, b.stored, "only the committed part is applied");
		eq(1, b.applies, "single apply per root commit");
		eq(70, EnergyTransfers.extract(b, 500, false), "extract limited by stored");
		eq(0, b.stored, "applied extract");

		java.util.Random r = new java.util.Random(7);
		Buffer f = new Buffer(500, 1000);
		long model = 500;
		for (int i = 0; i < 20000; i++) {
			int amount = r.nextInt(300) - 20;
			boolean sim = r.nextBoolean();
			int moved = r.nextBoolean() ? EnergyTransfers.insert(f, amount, sim) : -EnergyTransfers.extract(f, amount, sim);
			if (!sim)
				model += moved;
			assert f.stored == model && f.stored >= 0 && f.stored <= 1000 : "random walk conserves energy at step " + i;
		}
	}

	public static void main(String[] args) {
		boolean ea = false;
		assert ea = true;
		if (!ea)
			throw new IllegalStateException("run with java -ea");
		internalStorage();
		transactions();
		creative();
		deferred();
		System.out.println("EnergyTransferCheck OK");
	}
}
