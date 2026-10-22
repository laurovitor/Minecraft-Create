package com.simibubi.create.content.electricity.tesla;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllDamageTypes;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.electricity.AllElectricEffects;
import com.simibubi.create.content.electricity.ElectricityUtil;
import com.simibubi.create.content.electricity.energy.AbstractElectricBlockEntity;
import com.simibubi.create.content.electricity.energy.EnergyTransfers;
import com.simibubi.create.content.electricity.recipe.ChargingRecipe;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.sound.SoundScapes;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CElectricity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

/**
 * FE-powered belt charger (energy items and {@code create:charging} recipes) and redstone-triggered shock coil.
 * Ported from Create Crafts &amp; Additions (MIT, by MRH0).
 */
public class TeslaCoilBlockEntity extends AbstractElectricBlockEntity implements IHaveGoggleInformation {

	private static final EquipmentSlot[] ARMOR =
		{ EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

	private BeltProcessingBehaviour processing;
	private ItemStack lastInput = ItemStack.EMPTY;
	private Optional<RecipeHolder<ChargingRecipe>> recipe = Optional.empty();
	private int accumulated;
	private int poweredTimer;
	private int fireTimer;
	private int zapTimer = 200;

	public TeslaCoilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.Energy.BLOCK, AllBlockEntityTypes.TESLA_COIL.get(),
			(be, side) -> be.localEnergy);
	}

	private static CElectricity config() {
		return AllConfigs.server().electricity;
	}

	@Override
	public int getCapacity() {
		// Must hold at least one tick of either charge rate, as upstream does.
		return Math.max(config().teslaCapacity.get(),
			Math.max(config().teslaChargeRate.get(), config().teslaRecipeChargeRate.get()));
	}

	@Override
	public int getMaxIn() {
		return config().teslaMaxInput.get();
	}

	@Override
	public int getMaxOut() {
		return 0;
	}

	@Override
	public boolean isEnergyInput(Direction side) {
		return side != getBlockState().getValue(TeslaCoilBlock.FACING)
			.getOpposite();
	}

	@Override
	public boolean isEnergyOutput(Direction side) {
		return false;
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		processing = new BeltProcessingBehaviour(this).whenItemEnters((stack, handler) -> getBlockState()
				.getValue(TeslaCoilBlock.FACING) == Direction.UP ? ProcessingResult.HOLD : ProcessingResult.PASS)
			.whileItemHeld(this::charge);
		behaviours.add(processing);
	}

	private ProcessingResult charge(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		ItemStack stack = transported.stack;
		if (stack.isEmpty())
			return ProcessingResult.PASS;
		if (chargeItem(stack)) {
			poweredTimer = 10;
			return ProcessingResult.HOLD;
		}
		return chargeRecipe(transported, handler) ? ProcessingResult.HOLD : ProcessingResult.PASS;
	}

	/** Fills an item that exposes an energy capability, like the Capacitor. */
	private boolean chargeItem(ItemStack stack) {
		EnergyHandler itemEnergy = Capabilities.Energy.ITEM.getCapability(stack, ItemAccess.forStack(stack));
		if (itemEnergy == null || !EnergyTransfers.canReceive(itemEnergy))
			return false;
		if (localEnergy.getEnergyStored() < stack.getCount())
			return false;
		int amount = Math.min(config().teslaChargeRate.get(), localEnergy.getEnergyStored());
		localEnergy.internalConsumeEnergy(EnergyTransfers.insert(itemEnergy, amount, false));
		return true;
	}

	private boolean chargeRecipe(TransportedItemStack transported, TransportedItemStackHandlerBehaviour handler) {
		ItemStack stack = transported.stack;
		// Components matter: every sequenced assembly step uses the same transitional item
		if (!ItemStack.isSameItemSameComponents(lastInput, stack)) {
			lastInput = stack.copy();
			recipe = findRecipe(stack);
			accumulated = 0;
		}
		if (recipe.isEmpty())
			return false;

		ChargingRecipe active = recipe.get()
			.value();
		int rate = Math.min(config().teslaRecipeChargeRate.get(), active.getMaxChargeRate());
		int taken = localEnergy.internalConsumeEnergy(Math.min(rate, Math.max(0, active.getEnergy() - accumulated)));
		accumulated += taken;
		if (taken > 0)
			poweredTimer = 10;
		if (accumulated < active.getEnergy())
			return true;

		// Sequenced assembly steps are shared recipe instances whose next result is bound to the last
		// stack looked up, possibly by another machine, so look it up again for this stack.
		active = findRecipe(stack).map(RecipeHolder::value)
			.orElse(active);
		TransportedItemStack remaining = transported.copy();
		remaining.stack.shrink(1);
		List<TransportedItemStack> outputs = new ArrayList<>();
		for (ItemStack result : active.rollResults(level.getRandom())) {
			TransportedItemStack out = transported.copy();
			out.stack = result;
			outputs.add(out);
		}
		handler.handleProcessingOnItem(transported, TransportedResult.convertToAndLeaveHeld(outputs, remaining));
		accumulated = 0;
		lastInput = ItemStack.EMPTY;
		AllSoundEvents.LITTLE_ZAP.playOnServer(level, worldPosition, .1f, 1);
		return true;
	}

	private Optional<RecipeHolder<ChargingRecipe>> findRecipe(ItemStack stack) {
		Optional<RecipeHolder<ChargingRecipe>> assembly =
			SequencedAssemblyRecipe.getRecipe(level, stack, AllRecipeTypes.CHARGING.getType(), ChargingRecipe.class);
		if (assembly.isPresent())
			return assembly;
		return AllRecipeTypes.CHARGING.find(new SingleRecipeInput(stack), level);
	}

	@Override
	public void tick() {
		super.tick();
		if (level == null)
			return;
		if (level.isClientSide()) {
			if (getBlockState().getValue(TeslaCoilBlock.POWERED))
				SoundScapes.play(SoundScapes.AmbienceGroup.TESLA, worldPosition, 1);
			return;
		}

		int fireEnergy = config().teslaFireEnergy.get();
		boolean signal = level.getBestNeighborSignal(worldPosition) > 0;
		if (signal && localEnergy.getEnergyStored() >= fireEnergy)
			poweredTimer = 10;
		if (++fireTimer >= Math.max(1, config().teslaFireCooldown.get())) {
			fireTimer = 0;
			if (signal && localEnergy.getEnergyStored() >= fireEnergy) {
				localEnergy.internalConsumeEnergy(fireEnergy);
				shockEntities();
			}
		}

		if (poweredTimer > 0) {
			if (--zapTimer <= 0) {
				AllSoundEvents.LITTLE_ZAP.playOnServer(level, worldPosition, .1f, 1);
				zapTimer = 100 + level.getRandom()
					.nextInt(200);
			}
			poweredTimer--;
		}
		TeslaCoilBlock.setPowered(level, worldPosition, poweredTimer > 0);
	}

	private void shockEntities() {
		BlockPos front = worldPosition.relative(getBlockState().getValue(TeslaCoilBlock.FACING)
			.getOpposite());
		AABB area = new AABB(front).inflate(config().teslaHurtRange.get());
		DamageSource source = new DamageSource(level.registryAccess()
			.lookupOrThrow(Registries.DAMAGE_TYPE)
			.getOrThrow(AllDamageTypes.TESLA_COIL));
		boolean zapped = false;
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
			if (wearsFullChainmail(entity))
				continue;
			boolean player = entity instanceof Player;
			int damage = player ? config().teslaPlayerDamage.get() : config().teslaMobDamage.get();
			int duration = player ? config().teslaPlayerEffectTime.get() : config().teslaMobEffectTime.get();
			if (damage > 0) {
				entity.hurt(source, damage);
				if (!zapped) {
					AllSoundEvents.LOUD_ZAP.playOnServer(level, worldPosition, .6f, 1);
					zapped = true;
				}
			}
			if (duration > 0)
				entity.addEffect(new MobEffectInstance(AllElectricEffects.SHOCKING, duration));
		}
	}

	/** A full set of chainmail works as a Faraday suit. */
	private static boolean wearsFullChainmail(LivingEntity entity) {
		for (EquipmentSlot slot : ARMOR) {
			ItemStack armor = entity.getItemBySlot(slot);
			if (!armor.is(Items.CHAINMAIL_BOOTS) && !armor.is(Items.CHAINMAIL_LEGGINGS)
				&& !armor.is(Items.CHAINMAIL_CHESTPLATE) && !armor.is(Items.CHAINMAIL_HELMET))
				return false;
		}
		return true;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		CreateLang.builder()
			.add(Component.translatable(Create.ID + ".tooltip.energy.consumption")
				.withStyle(ChatFormatting.GRAY))
			.forGoggles(tooltip);
		CreateLang.builder()
			.add(Component.literal(" " + ElectricityUtil.format(config().teslaRecipeChargeRate.get()) + "⚡/t")
				.withStyle(ChatFormatting.AQUA))
			.forGoggles(tooltip);
		return true;
	}
}
