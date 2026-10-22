package com.simibubi.create.content.electricity.item;

import java.util.function.Consumer;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;

/** Portable energy cell: stores FE in the {@link AllDataComponents#STORED_ENERGY} component. */
public class CapacitorItem extends Item {

	public CapacitorItem(Properties props) {
		super(props.stacksTo(16));
	}

	public static int getEnergy(ItemStack stack) {
		return stack.getOrDefault(AllDataComponents.STORED_ENERGY, 0);
	}

	private static int capacity() {
		return AllConfigs.server().electricity.capacitorCapacity.get();
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
		Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(ElectricityUtil.getTextComponent(getEnergy(stack), "⚡")
			.append(Component.literal(" / " + ElectricityUtil.format(capacity()) + "⚡")));
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return getEnergy(stack) > 0;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return Math.round(Math.min(1f, (float) getEnergy(stack) / capacity()) * 13f);
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return 0x00BFFF;
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.Energy.ITEM,
			(stack, access) -> new ItemAccessEnergyHandler(access, AllDataComponents.STORED_ENERGY, capacity(),
				AllConfigs.server().electricity.capacitorChargeRate.get()),
			AllItems.CAPACITOR.get());
	}
}
