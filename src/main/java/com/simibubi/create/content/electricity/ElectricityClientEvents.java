package com.simibubi.create.content.electricity;

import com.simibubi.create.content.electricity.item.WireSpool;
import com.simibubi.create.content.electricity.packet.EnergyObservePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(Dist.CLIENT)
public class ElectricityClientEvents {

	public static boolean clientRenderHeldWire = false;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		EnergyObservePacket.tick();

		clientRenderHeldWire = false;
		if (Minecraft.getInstance().player == null)
			return;
		ItemStack stack = Minecraft.getInstance().player.getMainHandItem();
		if (stack.isEmpty() || WireSpool.isRemover(stack.getItem()))
			return;
		clientRenderHeldWire = ElectricityUtil.getWireNodeOfSpools(stack) != null;
	}
}
