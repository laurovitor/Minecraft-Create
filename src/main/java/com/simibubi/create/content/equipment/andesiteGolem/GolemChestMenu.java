package com.simibubi.create.content.equipment.andesiteGolem;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.equipment.redstoneGolem.RedstoneGolemChestBlockEntity;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.utility.ClientAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Lauro edition: the golem chest window (9 columns x 3 rows, see GolemChestLayout). Storage slots
 * behave like a chest; personal slots take only this golem's maintenance items; the Redstone
 * chest's distribution column takes only items some golem uses.
 */
public class GolemChestMenu extends MenuBase<AndesiteGolemChestBlockEntity> {

	/** Window x of slot column {@code col} (vanilla chest: 8 + col * 18). */
	public static final int STORAGE_X_STANDARD = 8, PERSONAL_X = 152;
	/** Redstone: distribution at 8, half-column gap (9 px), storage from 35, half gap, personal at 152. */
	public static final int DISTRIBUTION_X = 8, STORAGE_X_REDSTONE = 35;
	public static final int SLOTS_Y = 18;
	public static final Identifier PERSONAL_ICON = com.simibubi.create.Create.asResource("container/slot/golem_personal");
	public static final Identifier DISTRIBUTION_ICON = com.simibubi.create.Create.asResource("container/slot/golem_distribution");

	public int storageSlots, personalStart, distributionStart = -1, chestSlots;

	public GolemChestMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	public GolemChestMenu(MenuType<?> type, int id, Inventory inv, AndesiteGolemChestBlockEntity be) {
		super(type, id, inv, be);
	}

	public static void open(Player player, AndesiteGolemChestBlockEntity be, Component title) {
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.openMenu(new SimpleMenuProvider((id, inv, p) -> new GolemChestMenu(AllMenuTypes.GOLEM_CHEST.get(), id, inv, be),
				title), be.getBlockPos());
	}

	public boolean isRedstone() {
		return contentHolder instanceof RedstoneGolemChestBlockEntity;
	}

	@Override
	protected @Nullable AndesiteGolemChestBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
		BlockPos pos = extraData.readBlockPos();
		return ClientAccess.level().getBlockEntity(pos) instanceof AndesiteGolemChestBlockEntity be ? be : null;
	}

	@Override
	protected void initAndReadInventory(AndesiteGolemChestBlockEntity contentHolder) {}

	@Override
	protected void addSlots() {
		if (contentHolder == null)
			return;
		AndesiteGolemChestBlockEntity be = contentHolder;
		boolean redstone = isRedstone();
		int cols = redstone ? GolemChestLayout.REDSTONE_STORAGE_COLUMNS : GolemChestLayout.STANDARD_STORAGE_COLUMNS;
		int x0 = redstone ? STORAGE_X_REDSTONE : STORAGE_X_STANDARD;
		for (int row = 0; row < GolemChestLayout.ROWS; row++)
			for (int col = 0; col < cols; col++)
				addSlot(new Slot(be.getContainer(), row * cols + col, x0 + col * 18, SLOTS_Y + row * 18));
		storageSlots = slots.size();
		personalStart = slots.size();
		for (int row = 0; row < GolemChestLayout.ROWS; row++)
			addSlot(new RestrictedSlot(be.getPersonal(), row, PERSONAL_X, SLOTS_Y + row * 18, PERSONAL_ICON, false));
		if (redstone) {
			distributionStart = slots.size();
			for (int row = 0; row < GolemChestLayout.ROWS; row++)
				addSlot(new RestrictedSlot(((RedstoneGolemChestBlockEntity) be).getDistribution(), row, DISTRIBUTION_X,
					SLOTS_Y + row * 18, DISTRIBUTION_ICON, true));
		}
		chestSlots = slots.size();
		addPlayerSlots(8, 85);
	}

	@Override
	protected void saveData(AndesiteGolemChestBlockEntity contentHolder) {}

	@Override
	public boolean stillValid(Player player) {
		return contentHolder != null && Container.stillValidBlockEntity(contentHolder, player);
	}

	/** Chest -> player; player -> personal slots (if this golem uses the item), then storage. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < chestSlots) {
			if (!moveItemStackTo(stack, chestSlots, slots.size(), true))
				return ItemStack.EMPTY;
		} else if (!moveItemStackTo(stack, personalStart, personalStart + GolemChestLayout.PERSONAL_SIZE, false)
			&& !moveItemStackTo(stack, 0, storageSlots, false))
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		return copy;
	}

	/** Personal or distribution slot: limited items, an icon while empty. */
	public class RestrictedSlot extends Slot {
		private final Identifier icon;
		public final boolean distribution;

		RestrictedSlot(Container container, int index, int x, int y, Identifier icon, boolean distribution) {
			super(container, index, x, y);
			this.icon = icon;
			this.distribution = distribution;
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return distribution ? RedstoneGolemChestBlockEntity.distributable(stack) : contentHolder.usesPersonally(stack);
		}

		@Override
		public Identifier getNoItemIcon() {
			return icon;
		}
	}
}
