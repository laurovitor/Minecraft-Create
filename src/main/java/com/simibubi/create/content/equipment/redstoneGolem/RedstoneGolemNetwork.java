package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.content.equipment.andesiteGolem.AndesiteGolemChestBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlock;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lauro edition: the chests on a Redstone Golem's network. The golem chest carries a
 * LogisticallyLinkedBehaviour whose frequency roots the network: right-clicking the chest with a
 * Stock Link tunes the link to it, as on any Create logistics block. Each tuned Stock Link placed on
 * a chest registers on that frequency; the chest it is attached to (the block behind its connected
 * face, as PackagerLinkBlockEntity#getPackager resolves a packager) is a source. Create only keeps
 * links in loaded chunks, and drops links powered at full redstone strength.
 */
public final class RedstoneGolemNetwork {
	private RedstoneGolemNetwork() {}

	/** A source chest: the position to walk to and its inventory (a double chest counts once). */
	public record Source(BlockPos pos, Container container) {}

	public static List<Source> sources(Level level, RedstoneGolemChestBlockEntity home) {
		List<Source> sources = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		BlockPos homePos = home.getBlockPos();
		seen.add(homePos); // its own chest is never a source
		for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(home.link.freqId, false)) {
			if (!(link.blockEntity instanceof PackagerLinkBlockEntity stockLink) || stockLink.getLevel() != level)
				continue;
			BlockPos attached = stockLink.getBlockPos()
				.relative(PackagerLinkBlock.getConnectedDirection(stockLink.getBlockState()).getOpposite());
			BlockPos offset = attached.subtract(homePos);
			if (!level.isLoaded(attached) || !RedstoneGolemRules.inRange(offset.getX(), offset.getY(), offset.getZ()))
				continue;
			if (!seen.add(attached))
				continue;
			Container container = inventoryAt(level, attached, seen);
			if (container != null)
				sources.add(new Source(attached, container));
		}
		return sources;
	}

	/** Golem chests (any golem) and plain chests, double chests as one; anything else is ignored. */
	private static @Nullable Container inventoryAt(Level level, BlockPos pos, Set<BlockPos> seen) {
		if (level.getBlockEntity(pos) instanceof AndesiteGolemChestBlockEntity golemChest)
			return golemChest.getContainer();
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof ChestBlock chest))
			return null;
		if (state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE)
			if (!seen.add(pos.relative(ChestBlock.getConnectedDirection(state))))
				return null; // the other half was already listed
		return ChestBlock.getContainer(chest, state, level, pos, true);
	}

	public static int occupiedSlots(Container container) {
		int used = 0;
		for (int i = 0; i < container.getContainerSize(); i++)
			if (!container.getItem(i).isEmpty())
				used++;
		return used;
	}
}
