package com.simibubi.create.content.electricity.portable;

import java.util.Random;

import com.simibubi.create.content.electricity.energy.EnergyTransfers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * The Portable Energy Interface's view of a contraption: all Modular Accumulator controllers on it act as one
 * FE buffer, and every committed change is written back into the moving block-entity NBT that is restored
 * when the contraption is disassembled. Run through run-energy-check.sh.
 */
public class PortableEnergyCheck {

	static void eq(long expected, long actual, String what) {
		assert expected == actual : what + ": expected " + expected + " got " + actual;
	}

	static CompoundTag controller(int energy, int capacity) {
		CompoundTag nbt = new CompoundTag();
		CompoundTag content = new CompoundTag();
		content.putInt("energy", energy);
		nbt.put("EnergyContent", content);
		nbt.putInt("EnergyCapacity", capacity);
		return nbt;
	}

	static int stored(CompoundTag nbt) {
		return nbt.getCompoundOrEmpty("EnergyContent")
			.getIntOr("energy", -1);
	}

	public static void main(String[] args) {
		boolean ea = false;
		assert ea = true;
		if (!ea)
			throw new IllegalStateException("run with java -ea");

		PortableEnergyManager.EnergyStorageHolder holder = new PortableEnergyManager.EnergyStorageHolder(500, 300);
		CompoundTag a = controller(100, 1000);
		CompoundTag b = controller(0, 2000);
		CompoundTag part = new CompoundTag(); // non-controller accumulator block: no energy of its own
		holder.addEnergySource(a, new BlockPos(0, 0, 0));
		holder.addEnergySource(b, new BlockPos(0, 1, 0));
		holder.addEnergySource(part, new BlockPos(0, 2, 0));
		holder.addEnergySource(a, new BlockPos(0, 0, 0)); // tracked again every 20 ticks: no double count
		eq(100, EnergyTransfers.stored(holder), "sum of controllers");
		eq(3000, EnergyTransfers.capacity(holder), "sum of capacities");

		eq(500, EnergyTransfers.insert(holder, 5000, false), "insert limited by accumulator max input");
		eq(600, stored(a) + stored(b), "inserted energy written back to NBT");
		eq(600, stored(a), "first controller filled first");
		eq(0, stored(b), "second untouched while the first has room");
		EnergyTransfers.insert(holder, 500, false);
		eq(1000, stored(a), "first controller full");
		eq(100, stored(b), "overflow goes to the next controller");
		EnergyTransfers.extract(holder, 300, false);
		EnergyTransfers.extract(holder, 300, false);
		EnergyTransfers.extract(holder, 200, false);
		eq(300, stored(a) + stored(b), "drained back");
		EnergyTransfers.insert(holder, 300, false);
		eq(300, EnergyTransfers.extract(holder, 5000, false), "extract limited by max output");
		eq(300, stored(a) + stored(b), "extracted energy written back to NBT");

		try (Transaction tx = Transaction.openRoot()) {
			holder.insert(400, tx);
		}
		eq(300, stored(a) + stored(b), "aborted transaction leaves NBT untouched");
		eq(0, EnergyTransfers.insert(holder, 0, false), "zero insert");

		Random r = new Random(11);
		long model = EnergyTransfers.stored(holder);
		for (int i = 0; i < 20000; i++) {
			int amount = r.nextInt(700);
			boolean sim = r.nextInt(4) == 0;
			int moved = r.nextBoolean() ? EnergyTransfers.insert(holder, amount, sim)
				: -EnergyTransfers.extract(holder, amount, sim);
			if (!sim)
				model += moved;
			int nbt = stored(a) + stored(b);
			assert nbt == model && model == EnergyTransfers.stored(holder) : "conservation at step " + i;
			assert stored(a) >= 0 && stored(a) <= 1000 && stored(b) >= 0 && stored(b) <= 2000 : "per-controller bounds";
		}
		System.out.println("PortableEnergyCheck OK");
	}
}
