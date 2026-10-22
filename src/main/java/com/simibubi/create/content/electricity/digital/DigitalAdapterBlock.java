package com.simibubi.create.content.electricity.digital;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class DigitalAdapterBlock extends Block implements IBE<DigitalAdapterBlockEntity>, IWrenchable {
    public DigitalAdapterBlock(Properties props) { super(props); }
    @Override public Class<DigitalAdapterBlockEntity> getBlockEntityClass() { return DigitalAdapterBlockEntity.class; }
    @Override public BlockEntityType<? extends DigitalAdapterBlockEntity> getBlockEntityType() { return AllBlockEntityTypes.DIGITAL_ADAPTER.get(); }
}
