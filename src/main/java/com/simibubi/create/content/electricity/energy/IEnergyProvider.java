package com.simibubi.create.content.electricity.energy;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

public interface IEnergyProvider {
	@Nullable
	EnergyHandler getEnergyStorage(@Nullable Direction direction);
}
