package com.simibubi.create.content.electricity.packet;

import net.minecraft.server.level.ServerPlayer;

public interface IObserveBlockEntity {
	public void onObserved(ServerPlayer player, ObservePacketPayload pkt);
}
