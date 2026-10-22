package com.simibubi.create.content.electricity.packet;

import com.simibubi.create.AllPackets;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.api.client.network.ClientNetworkHelper;
import net.createmod.catnip.api.network.SelfHandlingPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Sent while a player looks at an electric block with goggles, asking the server for the live energy
 * figures that are not part of the block entity's regular sync.
 */
public record EnergyObservePacket(BlockPos pos, int node) implements SelfHandlingPayload {

	public static final StreamCodec<ByteBuf, EnergyObservePacket> STREAM_CODEC = StreamCodec.composite(
		BlockPos.STREAM_CODEC, EnergyObservePacket::pos,
		ByteBufCodecs.VAR_INT, EnergyObservePacket::node,
		EnergyObservePacket::new);

	private static int cooldown = 0;

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return AllPackets.ENERGY_OBSERVE.getType();
	}

	@Override
	public void handle(ServerPlayer player) {
		if (!player.level()
			.isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64 * 64)
			return;
		BlockEntity be = player.level()
			.getBlockEntity(pos);
		if (!(be instanceof IObserveBlockEntity observed))
			return;
		observed.onObserved(player, this);
		Packet<ClientGamePacketListener> update = be.getUpdatePacket();
		if (update != null)
			player.connection.send(update);
	}

	/** Client tick: limits observe requests to two per second. */
	public static void tick() {
		if (cooldown > 0)
			cooldown--;
	}

	public static boolean send(BlockPos pos, int node) {
		if (cooldown > 0)
			return false;
		cooldown = 10;
		ClientNetworkHelper.INSTANCE.sendToServer(new EnergyObservePacket(pos, node));
		return true;
	}
}
