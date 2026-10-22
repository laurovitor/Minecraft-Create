package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Lauro edition: whether a golem chest is part of a Redstone collection network, for the latch tint.
 * Same rule the Redstone golem uses (RedstoneGolemNetwork#sources): a Redstone golem chest whose
 * network has at least one source, or a chest that some Redstone golem chest's network lists as a
 * source (a tuned Stock Link on it, in range, loaded, not powered at 15). Visual only.
 */
public final class GolemChestLinks {
	private GolemChestLinks() {}

	/** Blockstate of all golem chests: the front latch shows a redstone tint while true. */
	public static final BooleanProperty LINKED = BooleanProperty.create("linked");

	public static boolean isConnected(Level level, AndesiteGolemChestBlockEntity chest) {
		if (chest instanceof RedstoneGolemChestBlockEntity home && !RedstoneGolemNetwork.sources(level, home).isEmpty())
			return true;
		BlockPos pos = chest.getBlockPos();
		Set<UUID> checked = new HashSet<>();
		for (Direction side : Direction.values()) {
			if (!(level.getBlockEntity(pos.relative(side)) instanceof PackagerLinkBlockEntity link) || link.behaviour == null)
				continue;
			BlockPos attached = link.getBlockPos().relative(PackagerLinkBlock.getConnectedDirection(link.getBlockState()).getOpposite());
			if (!attached.equals(pos) || !checked.add(link.behaviour.freqId))
				continue;
			for (LogisticallyLinkedBehaviour member : LogisticallyLinkedBehaviour.getAllPresent(link.behaviour.freqId, false))
				if (member.blockEntity instanceof RedstoneGolemChestBlockEntity home && home != chest && home.getLevel() == level
					&& RedstoneGolemNetwork.sources(level, home).stream().anyMatch(s -> s.pos().equals(pos)))
					return true;
		}
		return false;
	}
}
