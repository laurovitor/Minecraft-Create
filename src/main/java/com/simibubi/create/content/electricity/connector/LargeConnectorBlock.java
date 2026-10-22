package com.simibubi.create.content.electricity.connector;

import com.simibubi.create.Create;
import com.simibubi.create.content.electricity.connector.base.AbstractConnectorBlock;
import com.simibubi.create.content.electricity.connector.base.ConnectorMode;
import com.simibubi.create.content.electricity.connector.base.ConnectorVariant;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.electricity.ElectricShapes;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

public class LargeConnectorBlock extends AbstractConnectorBlock<LargeConnectorBlockEntity> {
    public static final VoxelShaper CONNECTOR_SHAPE = ElectricShapes.shape(5, 0, 5, 11, 7, 11).forDirectional();
    public LargeConnectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<LargeConnectorBlockEntity> getBlockEntityClass() {
        return LargeConnectorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LargeConnectorBlockEntity> getBlockEntityType() {
        return AllBlockEntityTypes.LARGE_CONNECTOR.get();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return AllBlockEntityTypes.LARGE_CONNECTOR.create(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return CONNECTOR_SHAPE.get(state.getValue(FACING).getOpposite());
    }

    public static void makeBlockState(DataGenContext<Block, LargeConnectorBlock> ctx, RegistrateBlockstateProvider provider) {
        BlockModelProvider models = provider.models();
        String basePath = "block/connector/";
        String basePathLarge = basePath + "large/";
        MultiPartBlockStateBuilder builder = provider.getMultipartBuilder(ctx.get());

        ModelFile.ExistingModelFile noneModel = models.getExistingFile(ResourceLocation.fromNamespaceAndPath(Create.ID, basePathLarge + "none"));
        ModelFile.ExistingModelFile pushModel = models.getExistingFile(ResourceLocation.fromNamespaceAndPath(Create.ID, basePathLarge + "push"));
        ModelFile.ExistingModelFile pullModel = models.getExistingFile(ResourceLocation.fromNamespaceAndPath(Create.ID, basePathLarge + "pull"));
        ModelFile.ExistingModelFile girderBaseModel = models.getExistingFile(ResourceLocation.fromNamespaceAndPath(Create.ID, basePath + "girder_base"));

        for(Direction direction: Direction.values()) {
            int horizontalAngle = direction == Direction.UP ? 180 : direction == Direction.DOWN ? 0 : 90;
            int  verticalAngle = ((int) direction.toYRot() + (direction.getAxis()
                    .isVertical() ? 180 : 0))% 360;
            builder.part()
                    .modelFile(noneModel)
                    .rotationX(horizontalAngle)
                    .rotationY(verticalAngle)
                    .addModel()
                    .condition(FACING, direction)
                    .condition(MODE, ConnectorMode.None)
                    .end()
                    .part()
                    .modelFile(pushModel)
                    .rotationX(horizontalAngle)
                    .rotationY(verticalAngle)
                    .addModel()
                    .condition(FACING, direction)
                    .condition(MODE, ConnectorMode.Push)
                    .end()
                    .part()
                    .modelFile(pullModel)
                    .rotationX(horizontalAngle)
                    .rotationY(verticalAngle)
                    .addModel()
                    .condition(FACING, direction)
                    .condition(MODE, ConnectorMode.Pull)
                    .end();

            builder.part()
                    .modelFile(girderBaseModel)
                    .rotationX(horizontalAngle)
                    .rotationY(verticalAngle)
                    .addModel()
                    .condition(FACING, direction)
                    .condition(VARIANT, ConnectorVariant.Girder)
                    .end();

        }
    }
}