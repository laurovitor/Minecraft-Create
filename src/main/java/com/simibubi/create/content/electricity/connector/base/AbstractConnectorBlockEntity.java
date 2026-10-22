package com.simibubi.create.content.electricity.connector.base;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.simibubi.create.Create;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.electricity.energy.*;
import com.simibubi.create.content.electricity.network.EnergyNetwork;
import com.simibubi.create.content.electricity.network.WireGraph;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.electricity.packet.EnergyNetworkPacketPayload;
import com.simibubi.create.content.electricity.packet.IObserveBlockEntity;
import com.simibubi.create.content.electricity.packet.ObservePacketPayload;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractConnectorBlockEntity extends SmartBlockEntity implements IWireNode, IObserveBlockEntity, IHaveGoggleInformation, IEnergyProvider {

	private final Set<LocalNode> wireCache = new HashSet<>();
	private final LocalNode[] localNodes;
	private final IWireNode[] nodeCache;
	private EnergyNetwork network;

	private boolean wasContraption = false;
	private boolean firstTick = true;

	public InterfaceEnergyHandler internal = new InterfaceEnergyHandler();
	protected BlockCapabilityCache<EnergyHandler, Direction> external;

	public AbstractConnectorBlockEntity(BlockEntityType<?> blockEntityTypeIn, BlockPos pos, BlockState state) {
		super(blockEntityTypeIn, pos, state);

		this.localNodes = new LocalNode[getNodeCount()];
		this.nodeCache = new IWireNode[getNodeCount()];
	}

	@Nullable
	@Override
	public EnergyHandler getEnergyStorage(@Nullable Direction direction) {
		if(isEnergyInput(direction) || isEnergyOutput(direction)) return internal;
		return null;
	}

	public abstract int getMaxIn();
	public abstract int getMaxOut();
	public int getCapacity() {
		return Math.min(getMaxIn(), getMaxOut());
	}

	public class InterfaceEnergyHandler extends DeferredEnergyHandler {
		@Override
		protected int simulateInsert(int maxReceive) {
			if(!AllConfigs.server().electricity.connectorAllowPassiveIo.get()) return 0;
			if(getMode() != ConnectorMode.Pull) return 0;
			if (network == null) return 0;
			return network.push(Math.min(maxReceive, getMaxIn()), true);
		}

		@Override
		protected int simulateExtract(int maxExtract) {
			if(!AllConfigs.server().electricity.connectorAllowPassiveIo.get()) return 0;
			if(getMode() != ConnectorMode.Push) return 0;
			if (network == null) return 0;
			return network.pull(Math.min(maxExtract, getMaxOut()), true);
		}

		@Override
		protected void applyInsert(int amount) {
			if (network != null) network.push(amount);
		}

		@Override
		protected void applyExtract(int amount) {
			if (network != null) network.pull(amount);
		}

		@Override
		public long getAmountAsLong() {
			if (network == null) return 0;
			return Math.min(getCapacity(), network.getBuff());
		}

		@Override
		public long getCapacityAsLong() {
			return getCapacity();
		}
	}

	@Override
	public @Nullable IWireNode getWireNode(int index) {
		return IWireNode.getWireNodeFrom(index, this, this.localNodes, this.nodeCache, level);
	}

	@Override
	public @Nullable LocalNode getLocalNode(int index) {
		return this.localNodes[index];
	}

	@Override
	public void setNode(int index, int other, BlockPos pos, WireType type) {
		this.localNodes[index] = new LocalNode(this, index, other, type, pos);

		notifyUpdate();

		WireGraph.nodeChanged(level, this);
	}

	@Override
	public void removeNode(int index, boolean dropWire) {
		LocalNode old = this.localNodes[index];
		this.localNodes[index] = null;
		this.nodeCache[index] = null;

		invalidateNodeCache();
		notifyUpdate();

		WireGraph.nodeChanged(level, this);
		// Drop wire next tick.
		if (dropWire && old != null) this.wireCache.add(old);
	}

	@Override
	public BlockPos getPos() {
		return getBlockPos();
	}

	@Override
	public void setNetwork(int node, EnergyNetwork network) {
		this.network = network;
	}

	@Override
	public EnergyNetwork getNetwork(int node) {
		return network;
	}

	public boolean isEnergyInput(Direction side) {
		return getBlockState().getValue(AbstractConnectorBlock.FACING) == side;
	}

	public boolean isEnergyOutput(Direction side) {
		return getBlockState().getValue(AbstractConnectorBlock.FACING) == side;
	}

	@Override
	public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(nbt, registries, clientPacket);
		// Convert old nbt data. x0, y0, z0, node0 & type0 etc.
		if (!clientPacket && nbt.contains("node0")) {
			convertOldNbt(nbt);
			setChanged();
		}

		// Read the nodes.
		invalidateLocalNodes();
		invalidateNodeCache();
		ListTag nodes = nbt.getList(LocalNode.NODES, Tag.TAG_COMPOUND);
		nodes.forEach(tag -> {
			LocalNode localNode = new LocalNode(this, (CompoundTag) tag);
			this.localNodes[localNode.getIndex()] = localNode;
		});

		// Check if this was a contraption.
		if (nbt.contains("contraption") && !clientPacket) {
			this.wasContraption = nbt.getBoolean("contraption");
			NodeRotation rotation = getBlockState().getValue(NodeRotation.ROTATION);
			if(level == null) return;
			if (rotation != NodeRotation.NONE)
				level.setBlock(getBlockPos(), getBlockState().setValue(NodeRotation.ROTATION, NodeRotation.NONE), 0);
			// Loop over all nodes and update their relative positions.
			for (LocalNode localNode : this.localNodes) {
				if (localNode == null) continue;
				localNode.updateRelative(rotation);
			}
		}

		// Only when already in the level (e.g. /data); on chunk load the level isn't set yet and onLoad registers instead.
		if (!clientPacket) WireGraph.nodeChanged(level, this);
	}

	@Override
	public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
		super.write(nbt, registries, clientPacket);
		// Write nodes.
		ListTag nodes = new ListTag();
		for (int i = 0; i < getNodeCount(); i++) {
			LocalNode localNode = this.localNodes[i];
			if (localNode == null) continue;
			CompoundTag tag = new CompoundTag();
			localNode.write(tag);
			nodes.add(tag);
		}
		nbt.put(LocalNode.NODES, nodes);
	}

	// Called after the tile entity has been part of a contraption
	private void validateNodes() {
		boolean changed = validateLocalNodes(this.localNodes);

		// Always set as changed if we were a contraption, as nodes might have been rotated.
		notifyUpdate();

		if (changed) {
			invalidateNodeCache();
			WireGraph.nodeChanged(level, this);
		}
	}

	public void firstTick() {
		this.firstTick = false;
		// Check if this blockentity was a part of a contraption.
		// If it was, then make sure all the nodes are valid.
		if(level == null) return;
		if (this.wasContraption && !level.isClientSide()) {
			this.wasContraption = false;
			validateNodes();
		}
	}

	protected void specialTick() {}

	@Override
	public void tick() {
		if (this.firstTick) firstTick();
		if (level == null) return;
		if (!level.isLoaded(getBlockPos())) return;

		// Check if we need to drop any wires due to contraption.
		if (!this.wireCache.isEmpty() && !isRemoved()) handleWireCache(level, this.wireCache);

		// Every mode needs its network: light connectors and passive IO use it too.
		if (!level.isClientSide()) awakeNetwork(level);

		specialTick();

		if (getMode() == ConnectorMode.None) return;
		super.tick();

		if(level == null) return;
		if(level.isClientSide()) return;

		networkTick(network);
	}

	private void networkTick(EnergyNetwork network) {
		ConnectorMode mode = getMode();
		if(level == null) return;
		if(level.isClientSide()) return;
		if(network == null) return;
		if(external == null) {
			updateExternalEnergyStorage();
			if(external == null) {
				return;
			}
		}
		EnergyHandler otherStorage = external.getCapability();
		if (otherStorage == null) return;

		if (mode == ConnectorMode.Push) {
			int pulled = network.pull(network.demand(EnergyTransfers.insert(otherStorage, getMaxOut(), true)));
			EnergyTransfers.insert(otherStorage, pulled, false);
		}

		if (mode == ConnectorMode.Pull) {
			int toPush = EnergyTransfers.extract(otherStorage, network.push(getMaxIn(), true), false);
			network.push(toPush);
		}
	}

	@Override
	public void onLoad() {
		super.onLoad();
		WireGraph.nodeLoaded(level, this);
	}

	// Chunk unloaded or block removed.
	@Override
	public void invalidate() {
		super.invalidate();
		WireGraph.nodeUnloaded(level, this);
	}

	@Override
	public void remove() {
		if(level == null) return;
		if (level.isClientSide()) return;
		// Remove all nodes.
		for (int i = 0; i < getNodeCount(); i++) {
			LocalNode localNode = getLocalNode(i);
			if (localNode == null) continue;
			IWireNode otherNode = getWireNode(i);
			if(otherNode == null) {
				// The other end is unloaded and lets go of the wire when it next loads, so the wire drops here.
				if (!localNode.isInvalid()) dropWire(level, localNode);
				continue;
			}

			int ourNode = localNode.getOtherIndex();
			LocalNode otherLocal = otherNode.getLocalNode(ourNode);
			if (otherLocal == null || !otherLocal.getPos().equals(getBlockPos())) continue;
			if (localNode.isInvalid())
				otherNode.removeNode(ourNode);
			else
				otherNode.removeNode(ourNode, true); // Make the other node drop the wires.
		}

		invalidateNodeCache();

		WireGraph.nodeRemoved(level, this);
	}

	public void invalidateLocalNodes() {
		for(int i = 0; i < getNodeCount(); i++)
			this.localNodes[i] = null;
	}

	@Override
	public void invalidateNodeCache() {
		for(int i = 0; i < getNodeCount(); i++)
			this.nodeCache[i] = null;
	}

	public ConnectorMode getMode() {
		return getBlockState().getValue(AbstractConnectorBlock.MODE);
	}

	@Override
	public void onObserved(ServerPlayer player, ObservePacketPayload pack) {
		if(isNetworkValid(0))
			EnergyNetworkPacketPayload.send(worldPosition, getNetwork(0).getPulled(), getNetwork(0).getPushed(), player);
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		ObservePacketPayload.send(worldPosition, 0);

		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.connector.info").withStyle(ChatFormatting.WHITE)).forGoggles(tooltip);

		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.mode").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" ")
				.append(getBlockState().getValue(AbstractConnectorBlock.MODE).getTooltip().withStyle(ChatFormatting.AQUA))).forGoggles(tooltip);

		CreateLang.builder().add(Component.translatable(Create.ID + ".tooltip.energy.usage").withStyle(ChatFormatting.GRAY)).forGoggles(tooltip);
		CreateLang.builder().add(Component.literal(" ").append(ElectricityUtil.format((int)EnergyNetworkPacketPayload.clientBuff)).append("⚡/t").withStyle(ChatFormatting.AQUA)).forGoggles(tooltip);

		return IHaveGoggleInformation.super.addToGoggleTooltip(tooltip, isPlayerSneaking);
	}

	public boolean ignoreCapSide() {
		return this.getBlockState().getValue(AbstractConnectorBlock.MODE).isActive();
	}

	public void updateExternalEnergyStorage() {
		if (level == null) return;
		if (!(level instanceof ServerLevel)) return;
		if (!level.isLoaded(getBlockPos())) return;
		var side = getBlockState().getValue(AbstractConnectorBlock.FACING);

		external = BlockCapabilityCache.create(
			Capabilities.Energy.BLOCK, // capability to cache
			(ServerLevel) level, // level
			getPos().relative(side),
			side.getOpposite(),
			() -> !this.isRemoved(), // validity check (because the cache might outlive the object it belongs to)
			() -> {} // invalidation listener
		);
	}
}
