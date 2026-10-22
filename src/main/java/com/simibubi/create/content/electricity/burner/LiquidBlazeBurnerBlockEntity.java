package com.simibubi.create.content.electricity.burner;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.electricity.recipe.FluidRecipeInput;
import com.simibubi.create.content.electricity.recipe.LiquidBurningRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;

/** Reuses Create's burner heat/animation state and adds a transactional fuel tank. */
public class LiquidBlazeBurnerBlockEntity extends BlazeBurnerBlockEntity implements IHaveGoggleInformation {
    private final SmartFluidTank tank;
    private Optional<RecipeHolder<LiquidBurningRecipe>> fuelRecipe = Optional.empty();
    private boolean recipeDirty = true;
    public LiquidBlazeBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type,pos,state);
        tank = new SmartFluidTank(AllConfigs.server().electricity.liquidBurnerCapacity.get(), fluid -> { recipeDirty = true; notifyUpdate(); });
    }
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, AllBlockEntityTypes.LIQUID_BLAZE_BURNER.get(), (be,dir) -> be.tank);
    }
    public SmartFluidTank getTank() { return tank; }
    private void updateRecipe() {
        if (level == null || level.isClientSide() || !recipeDirty) return;
        recipeDirty = false;
        FluidStack fluid = tank.getFluid();
        fuelRecipe = fluid.isEmpty() ? Optional.empty() : AllRecipeTypes.LIQUID_BURNING.find(new FluidRecipeInput(fluid),level);
    }
    private void burnFluid() {
        updateRecipe();
        if (fuelRecipe.isEmpty() || tank.getFluidAmount() < 100 || remainingBurnTime > AllConfigs.server().electricity.liquidBurnerHeatCapacity.get()) return;
        LiquidBurningRecipe recipe = fuelRecipe.get().value();
        FluidResource resource = tank.getResource(0);
        try (Transaction tx = Transaction.open(null)) {
            int drained = tank.extract(0,resource,100,tx);
            if (drained != 100) return;
            tx.commit();
        }
        FuelType next = recipe.isSuperheated() ? FuelType.SPECIAL : FuelType.NORMAL;
        int time = Math.max(1,recipe.getBurnTime()/10);
        remainingBurnTime = activeFuel == next ? Math.min(AllConfigs.server().electricity.liquidBurnerHeatCapacity.get(),remainingBurnTime+time) : time;
        activeFuel = next;
        updateBlockState();
    }
    @Override public void tick() {
        if (level != null && !level.isClientSide() && !isCreative) burnFluid();
        super.tick();
    }
    @Override public void write(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.write(tag,registries,clientPacket);
        tag.put("LiquidFuel",tank.writeToNBT(registries,new CompoundTag()));
    }
    @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean clientPacket) {
        super.read(tag,registries,clientPacket);
        tank.readFromNBT(registries,tag.getCompoundOrEmpty("LiquidFuel"));
        recipeDirty = true;
    }
    @Override public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        return containedFluidTooltip(tooltip, isPlayerSneaking, tank);
    }
}
