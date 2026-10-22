package com.simibubi.create.infrastructure.config;

/**
 * Electricity settings, merged from Create Crafts &amp; Additions (MIT, by MRH0).
 * Defaults match the values Crafts &amp; Additions shipped with.
 */
public class CElectricity extends ConfigBase {

	public final ConfigInt feAtMaxRpm = i(480, 0, "feAtMaxRpm", Comments.feAtMaxRpm);

	public final ConfigGroup motor = group(1, "electricMotor", "Electric Motor");
	public final ConfigInt motorRpmRange = i(256, 1, "motorRpmRange", Comments.motorRpmRange);
	public final ConfigInt motorMinConsumption = i(8, 0, "motorMinConsumption", Comments.motorMinConsumption);
	public final ConfigInt motorMaxInput = i(5000, 0, "motorMaxInput", Comments.maxInput);
	public final ConfigInt motorCapacity = i(5000, 0, "motorCapacity", Comments.capacity);

	public final ConfigGroup alternator = group(1, "alternator", "Alternator");
	public final ConfigInt alternatorMaxOutput = i(5000, 0, "alternatorMaxOutput", Comments.maxOutput);
	public final ConfigInt alternatorCapacity = i(5000, 0, "alternatorCapacity", Comments.capacity);
	public final ConfigFloat alternatorEfficiency = f(.75f, .01f, 1, "alternatorEfficiency", Comments.alternatorEfficiency);

	public final ConfigGroup liquidBlazeBurner = group(1, "liquidBlazeBurner", "Liquid Blaze Burner");
	public final ConfigInt liquidBurnerCapacity = i(1000, 100, "liquidBurnerCapacity", "Fuel capacity [mB].");
	public final ConfigInt liquidBurnerHeatCapacity = i(10000, 0, "liquidBurnerHeatCapacity", "Heat capacity [ticks].");

	public final ConfigGroup portableEnergyInterface = group(1, "portableEnergyInterface", "Portable Energy Interface");
	public final ConfigInt peiMaxInput = i(5000, 0, "peiMaxInput", Comments.maxInput);
	public final ConfigInt peiMaxOutput = i(5000, 0, "peiMaxOutput", Comments.maxOutput);

	public final ConfigGroup tesla = group(1, "teslaCoil", "Tesla Coil");
	public final ConfigInt teslaMaxInput = i(10000, 0, "teslaMaxInput", Comments.maxInput);
	public final ConfigInt teslaChargeRate = i(5000, 0, "teslaChargeRate", "Item charge rate [FE/t].");
	public final ConfigInt teslaRecipeChargeRate = i(2000, 0, "teslaRecipeChargeRate", "Recipe charge rate [FE/t].");
	public final ConfigInt teslaCapacity = i(40000, 0, "teslaCapacity", Comments.capacity);
	public final ConfigInt teslaFireEnergy = i(1000, 0, "teslaFireEnergy", "Energy spent per redstone shock.");
	public final ConfigInt teslaHurtRange = i(3, 0, "teslaHurtRange", "Shock range [blocks].");
	public final ConfigInt teslaMobDamage = i(3, 0, "teslaMobDamage", "Damage to mobs.");
	public final ConfigInt teslaPlayerDamage = i(2, 0, "teslaPlayerDamage", "Damage to players.");
	public final ConfigInt teslaMobEffectTime = i(20, 0, "teslaMobEffectTime", "Shock duration for mobs [ticks].");
	public final ConfigInt teslaPlayerEffectTime = i(20, 0, "teslaPlayerEffectTime", "Shock duration for players [ticks].");
	public final ConfigInt teslaFireCooldown = i(20, 1, "teslaFireCooldown", "Shock interval [ticks].");

	public final ConfigGroup electricPump = group(1, "electricPump", "Electric Pump");
	public final ConfigInt pumpRpmRange = i(256, 1, "pumpRpmRange", "Pump strength range in equivalent Mechanical Pump RPM.");
	public final ConfigInt pumpFeAtMaxRpm = i(120, 1, "pumpFeAtMaxRpm", "Electric Pump consumption at 256 RPM [FE/t].");
	public final ConfigInt pumpMaxInput = i(5000, 0, "pumpMaxInput", Comments.maxInput);
	public final ConfigInt pumpCapacity = i(5000, 0, "pumpCapacity", Comments.capacity);

	public final ConfigGroup rollingMill = group(1, "rollingMill", "Rolling Mill");
	public final ConfigInt rollingMillDuration = i(120, 0, "rollingMillProcessingDuration", Comments.rollingMillDuration);

	public final ConfigGroup wires = group(1, "wires", "Connectors and wires");
	public final ConfigInt networkBuffer = i(80000, 80000, "connectorNetworkInternalBuffer", Comments.networkBuffer);
	public final ConfigInt smallConnectorMaxInput = i(1000, 0, "smallConnectorMaxInput", Comments.maxInput);
	public final ConfigInt smallConnectorMaxOutput = i(1000, 0, "smallConnectorMaxOutput", Comments.maxOutput);
	public final ConfigInt smallConnectorMaxLength = i(16, 0, 256, "smallConnectorWireLength", Comments.wireLength);
	public final ConfigInt smallLightConnectorConsumption =
		i(1, 0, "smallLightConnectorConsumption", Comments.lightConsumption);
	public final ConfigInt largeConnectorMaxInput = i(5000, 0, "largeConnectorMaxInput", Comments.maxInput);
	public final ConfigInt largeConnectorMaxOutput = i(5000, 0, "largeConnectorMaxOutput", Comments.maxOutput);
	public final ConfigInt largeConnectorMaxLength = i(32, 0, 256, "largeConnectorWireLength", Comments.wireLength);
	public final ConfigBool connectorIgnoreFaceCheck = b(true, "connectorIgnoreFaceCheck", Comments.ignoreFaceCheck);
	public final ConfigBool connectorAllowPassiveIo = b(true, "connectorAllowPassiveIO", Comments.passiveIo);

	public final ConfigGroup accumulator = group(1, "accumulator", "Modular Accumulator");
	public final ConfigInt accumulatorMaxInput = i(5000, 0, "accumulatorMaxInput", Comments.maxInput);
	public final ConfigInt accumulatorMaxOutput = i(5000, 0, "accumulatorMaxOutput", Comments.maxOutput);
	public final ConfigInt accumulatorCapacity = i(2_000_000, 0, "accumulatorCapacity", Comments.accumulatorCapacity);
	public final ConfigInt accumulatorMaxHeight = i(5, 1, 8, "accumulatorMaxHeight", Comments.accumulatorMaxHeight);
	public final ConfigInt accumulatorMaxWidth = i(3, 1, 8, "accumulatorMaxWidth", Comments.accumulatorMaxWidth);

	public final ConfigGroup barbedWire = group(1, "barbedWire", "Barbed Wire");
	public final ConfigInt barbedWireDamage = i(2, 0, "barbedWireDamage", "Barbed Wire damage on contact.");

	public final ConfigGroup items = group(1, "items", "Electric items");
	public final ConfigBool amuletEffectEnabled = b(true, "amuletEffectEnabled", "Whether the Electrum Amulet passively charges an item held in the other hand.");
	public final ConfigInt amuletChargeRate = i(1, 0, "electrumAmuletChargeRate", "Average charge rate [FE/t]. The amulet transfers this many FE times 300, once per 300 ticks on average.");
	public final ConfigInt capacitorCapacity = i(5000, 1, "capacitorCapacity", Comments.capacitorCapacity);
	public final ConfigInt capacitorChargeRate = i(500, 1, "capacitorChargeRate", Comments.capacitorChargeRate);

	@Override
	public String getName() {
		return "electricity";
	}

	private static class Comments {
		static String feAtMaxRpm =
			"Energy conversion rate: FE/t generated by an Alternator and consumed by an Electric Motor at 256 RPM.";
		static String motorRpmRange = "Electric Motor min/max RPM.";
		static String motorMinConsumption = "Minimum energy an active Electric Motor consumes [in FE/t].";
		static String maxInput = "Max energy accepted per tick [in FE/t]. Energy transfer, not consumption.";
		static String maxOutput = "Max energy given out per tick [in FE/t]. Energy transfer, not generation.";
		static String capacity = "Internal energy buffer [in FE].";
		static String alternatorEfficiency = "Alternator efficiency relative to the base conversion rate.";
		static String rollingMillDuration = "Rolling Mill processing duration [in ticks].";
		static String networkBuffer = "Max energy stored in the internal buffer of one connector network [in FE].";
		static String wireLength = "Max wire length [in blocks].";
		static String lightConsumption = "Energy used by a Small Connector with Light while lit [in FE/t].";
		static String ignoreFaceCheck = "Allow connectors on faces that could not normally support them.";
		static String passiveIo =
			"Allow blocks attached to a connector to freely pass energy to and from the connector network.";
		static String accumulatorCapacity = "Modular Accumulator capacity per block [in FE].";
		static String accumulatorMaxHeight = "Modular Accumulator max multiblock height.";
		static String accumulatorMaxWidth = "Modular Accumulator max multiblock width.";
		static String capacitorCapacity = "Max energy a Capacitor can store [in FE].";
		static String capacitorChargeRate = "Max energy per transfer for the Capacitor [in FE].";
	}

}
