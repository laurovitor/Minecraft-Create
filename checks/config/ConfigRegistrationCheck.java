package com.simibubi.create.infrastructure.config;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Regression for a grouped nested config followed by another section. */
public class ConfigRegistrationCheck {
	public static void main(String[] args) {
		Pair<CServer, ModConfigSpec> result = new ModConfigSpec.Builder().configure(builder -> {
			CServer config = new CServer();
			config.registerAll(builder);
			return config;
		});
		ModConfigSpec spec = result.getRight();
		assertPath(spec, "recipes", "bulkPressing");
		assertPath(spec, "kinetics", "maxBeltLength");
		assertPath(spec, "fluids", "fluidTankCapacity");
		assertPath(spec, "schematics", "schematicannon", "schematicannonDelay");
		assertPath(spec, "equipment", "maxSymmetryWandRange");
		assertPath(spec, "trains", "trainStats", "trainTopSpeed");
		assertPath(spec, "electricity", "feAtMaxRpm");
		assertPath(spec, "electricity", "electricMotor", "motorCapacity");
		assertPath(spec, "electricity", "items", "capacitorCapacity");
		assertPath(spec, "electricity", "liquidBlazeBurner", "liquidBurnerCapacity");
		assertPath(spec, "electricity", "portableEnergyInterface", "peiMaxInput");
		assertPath(spec, "electricity", "teslaCoil", "teslaFireCooldown");
		assertPath(spec, "electricity", "electricPump", "pumpCapacity");
		assertPath(spec, "electricity", "barbedWire", "barbedWireDamage");
		System.out.println("Config registration passed: all server sections are separate and electricity is complete");
	}

	private static void assertPath(ModConfigSpec spec, String... path) {
		if (!spec.getSpec().contains(List.of(path)))
			throw new AssertionError("Missing config path: " + String.join(".", path));
	}
}
