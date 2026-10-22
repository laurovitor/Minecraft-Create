package com.simibubi.create.content.electricity.connector;

import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlock;
import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

public class LargeConnectorBlockEntity extends AbstractConnectorBlockEntity {

    private final static float OFFSET_HEIGHT = 1f;
    public final static Vec3 OFFSET_DOWN = new Vec3(0f, -OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_UP = new Vec3(0f, OFFSET_HEIGHT/16f, 0f);
    public final static Vec3 OFFSET_NORTH = new Vec3(0f, 0f, -OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_WEST = new Vec3(-OFFSET_HEIGHT/16f, 0f, 0f);
    public final static Vec3 OFFSET_SOUTH = new Vec3(0f, 0f, OFFSET_HEIGHT/16f);
    public final static Vec3 OFFSET_EAST = new Vec3(OFFSET_HEIGHT/16f, 0f, 0f);

    public LargeConnectorBlockEntity(BlockEntityType<?> blockEntityTypeIn, BlockPos pos, BlockState state) {
        super(blockEntityTypeIn, pos, state);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.Energy.BLOCK,
                AllBlockEntityTypes.LARGE_CONNECTOR.get(),
                (be, context) -> be.internal
        );
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> list) {}

    @Override
    public int getMaxIn() {
        return AllConfigs.server().electricity.largeConnectorMaxInput.get();
    }

    @Override
    public int getMaxOut() {
        return AllConfigs.server().electricity.largeConnectorMaxOutput.get();
    }

    @Override
    public int getNodeCount() {
        return 6;
    }

    @Override
    public Vec3 getNodeOffset(int node) {
        return switch (getBlockState().getValue(AbstractConnectorBlock.FACING)) {
            case DOWN -> OFFSET_DOWN;
            case UP -> OFFSET_UP;
            case NORTH -> OFFSET_NORTH;
            case WEST -> OFFSET_WEST;
            case SOUTH -> OFFSET_SOUTH;
            case EAST -> OFFSET_EAST;
        };
    }

    @Override
    public ConnectorType getConnectorType() {
        return ConnectorType.Large;
    }

    public int getMaxWireLength() {
        return AllConfigs.server().electricity.largeConnectorMaxLength.get();
    }
}
