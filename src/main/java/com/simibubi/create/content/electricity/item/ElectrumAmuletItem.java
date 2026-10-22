package com.simibubi.create.content.electricity.item;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.electricity.AllElectricEffects;
import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;

/** Passive offhand/main-hand charger from Crafts & Additions (MIT, by MRH0). */
public class ElectrumAmuletItem extends Item {
	private static final int CHARGE_CHANCE = 300;

	public ElectrumAmuletItem(Properties properties) {
		super(properties);
	}

	public static void tick(Player player) {
		if (player.level().isClientSide() || !AllConfigs.server().electricity.amuletEffectEnabled.get())
			return;
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		ItemStack charge = AllItems.ELECTRUM_AMULET.isIn(main) ? off
			: AllItems.ELECTRUM_AMULET.isIn(off) ? main : ItemStack.EMPTY;
		if (charge.isEmpty())
			return;
		var energy = Capabilities.Energy.ITEM.getCapability(charge, ItemAccess.forStack(charge));
		if (EnergyTransfers.insert(energy, 1, true) <= 0 || player.getRandom().nextInt(CHARGE_CHANCE) != 0)
			return;
		EnergyTransfers.insert(energy, CHARGE_CHANCE * AllConfigs.server().electricity.amuletChargeRate.get(), false);
		player.addEffect(new MobEffectInstance(AllElectricEffects.SHOCKING, 40));
		AllSoundEvents.LITTLE_ZAP.playOnServer(player.level(), player.blockPosition(), .4f, 1f);
	}
}
