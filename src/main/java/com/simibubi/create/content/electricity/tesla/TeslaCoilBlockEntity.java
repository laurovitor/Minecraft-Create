package com.simibubi.create.content.electricity.tesla;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.electricity.AllElectricEffects;
import com.simibubi.create.content.electricity.energy.AbstractElectricBlockEntity;
import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import com.simibubi.create.content.electricity.recipe.ChargingRecipe;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Native 26.3 port of CC&A's FE-powered belt charger and redstone shock coil. */
public class TeslaCoilBlockEntity extends AbstractElectricBlockEntity {
    private BeltProcessingBehaviour processing;
    private ItemStack lastInput = ItemStack.EMPTY;
    private Optional<RecipeHolder<ChargingRecipe>> recipe = Optional.empty();
    private int accumulated;
    private int poweredTimer;
    private int fireTimer;

    public TeslaCoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type,pos,state); }
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Energy.BLOCK, AllBlockEntityTypes.TESLA_COIL.get(), (be,side) -> be.localEnergy);
    }
    @Override public int getCapacity() { return AllConfigs.server().electricity.teslaCapacity.get(); }
    @Override public int getMaxIn() { return AllConfigs.server().electricity.teslaMaxInput.get(); }
    @Override public int getMaxOut() { return 0; }
    @Override public boolean isEnergyInput(Direction side) { return side != getBlockState().getValue(TeslaCoilBlock.FACING).getOpposite(); }
    @Override public boolean isEnergyOutput(Direction side) { return false; }
    @Override public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        processing = new BeltProcessingBehaviour(this)
            .whenItemEnters((stack,handler) -> getBlockState().getValue(TeslaCoilBlock.FACING) == Direction.UP
                ? BeltProcessingBehaviour.ProcessingResult.HOLD : BeltProcessingBehaviour.ProcessingResult.PASS)
            .whileItemHeld(this::charge);
        behaviours.add(processing);
    }
    private BeltProcessingBehaviour.ProcessingResult charge(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
        ItemStack stack = transported.stack;
        if (stack.isEmpty()) return BeltProcessingBehaviour.ProcessingResult.PASS;
        EnergyHandler itemEnergy = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
        if (itemEnergy != null && EnergyTransfers.canReceive(itemEnergy) && localEnergy.getEnergyStored() > 0) {
            int moved = EnergyTransfers.insert(itemEnergy, Math.min(AllConfigs.server().electricity.teslaChargeRate.get(), localEnergy.getEnergyStored()), false);
            localEnergy.internalConsumeEnergy(moved);
            if (moved > 0) { poweredTimer = 10; return BeltProcessingBehaviour.ProcessingResult.HOLD; }
        }
        if (!ItemStack.isSameItemSameComponents(lastInput, stack)) {
            lastInput = stack.copy();
            recipe = findRecipe(stack);
            accumulated = 0;
        }
        if (recipe.isEmpty()) return BeltProcessingBehaviour.ProcessingResult.PASS;
        ChargingRecipe active = recipe.get().value();
        int needed = Math.max(0, active.getEnergy() - accumulated);
        int rate = Math.min(AllConfigs.server().electricity.teslaRecipeChargeRate.get(), active.getMaxChargeRate());
        int taken = localEnergy.internalConsumeEnergy(Math.min(needed, rate));
        accumulated += taken;
        if (taken > 0) poweredTimer = 10;
        if (accumulated >= active.getEnergy()) {
            TransportedItemStack remaining = transported.copy();
            remaining.stack.shrink(1);
            List<TransportedItemStack> outputs = new ArrayList<>();
            for (ItemStack result : active.rollResults(level.getRandom())) {
                TransportedItemStack out = transported.copy(); out.stack = result; outputs.add(out);
            }
            handler.handleProcessingOnItem(transported,
                TransportedItemStackHandlerBehaviour.TransportedResult.convertToAndLeaveHeld(outputs, remaining));
            accumulated = 0;
            lastInput = ItemStack.EMPTY;
        }
        return BeltProcessingBehaviour.ProcessingResult.HOLD;
    }
    private Optional<RecipeHolder<ChargingRecipe>> findRecipe(ItemStack stack) {
        Optional<RecipeHolder<ChargingRecipe>> assembly = SequencedAssemblyRecipe.getRecipe(level, stack, AllRecipeTypes.CHARGING.getType(), ChargingRecipe.class);
        if (assembly.isPresent()) return assembly;
        return AllRecipeTypes.CHARGING.find(new SingleRecipeInput(stack), level);
    }
    @Override public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (poweredTimer > 0) poweredTimer--;
        int cooldown = Math.max(1, AllConfigs.server().electricity.teslaFireCooldown.get());
        if (level.getBestNeighborSignal(worldPosition) > 0 && ++fireTimer >= cooldown
            && localEnergy.getEnergyStored() >= AllConfigs.server().electricity.teslaFireEnergy.get()) {
            fireTimer = 0;
            localEnergy.internalConsumeEnergy(AllConfigs.server().electricity.teslaFireEnergy.get());
            shockEntities();
            poweredTimer = 10;
        }
        TeslaCoilBlock.setPowered(level, worldPosition, poweredTimer > 0);
    }
    private void shockEntities() {
        BlockPos front = worldPosition.relative(getBlockState().getValue(TeslaCoilBlock.FACING).getOpposite());
        int range = AllConfigs.server().electricity.teslaHurtRange.get();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(AllDamageTypes.TESLA_COIL));
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(front).inflate(range))) {
            boolean fullChain = true;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                ItemStack armor = entity.getItemBySlot(slot); if (!armor.is(Items.CHAINMAIL_BOOTS) && !armor.is(Items.CHAINMAIL_LEGGINGS)
                && !armor.is(Items.CHAINMAIL_CHESTPLATE) && !armor.is(Items.CHAINMAIL_HELMET)) { fullChain = false; break; }
            }
            if (fullChain) continue;
            boolean player = entity instanceof Player;
            int damage = player ? AllConfigs.server().electricity.teslaPlayerDamage.get() : AllConfigs.server().electricity.teslaMobDamage.get();
            int duration = player ? AllConfigs.server().electricity.teslaPlayerEffectTime.get() : AllConfigs.server().electricity.teslaMobEffectTime.get();
            if (damage > 0) entity.hurt(source, damage);
            if (duration > 0) entity.addEffect(new MobEffectInstance(AllElectricEffects.SHOCKING, duration));
        }
    }
}
