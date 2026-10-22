package com.simibubi.create.content.electricity.packet;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.network.NetworkHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Answer to {@link EnergyObservePacket}: demand and buffer of the observed energy network, for goggles. */
public record EnergyNetworkPacket(BlockPos pos, int demand, int buff) implements CustomPacketPayload {

	public static double clientSaturation = 0;
	public static int clientDemand = 0;
	public static int clientBuff = 0;

	public static final StreamCodec<ByteBuf, EnergyNetworkPacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, EnergyNetworkPacket::pos,
		ByteBufCodecs.VAR_INT, EnergyNetworkPacket::demand,
		ByteBufCodecs.VAR_INT, EnergyNetworkPacket::buff,
		EnergyNetworkPacket::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.ENERGY_NETWORK.getType();
	}

	public void handle(Player player) {
		clientDemand = demand;
		clientBuff = buff;
		clientSaturation = buff - demand;
	}

	public static void send(BlockPos pos, int demand, int buff, ServerPlayer player) {
		NetworkHelper.INSTANCE.sendToClient(player, new EnergyNetworkPacket(pos, demand, buff));
	}
}
