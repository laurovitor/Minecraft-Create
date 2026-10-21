package com.simibubi.create.content.logistics.vault;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;

/**
 * Lauro edition: right-clicking an item vault (regular or reinforced) with a blank clipboard writes
 * a snapshot of the vault's size and fill level onto it. Items are not listed, only totals, so the
 * player can tell when a new vault is needed. Called from ClipboardValueSettingsHandler.
 */
public class ItemVaultClipboardInfo {

	/**
	 * Blank means nothing written and no copied machine settings. Opening and closing the
	 * clipboard without typing leaves it blank.
	 */
	public static boolean isBlank(ClipboardContent content) {
		if (content.copiedValues().isPresent())
			return false;
		for (List<ClipboardEntry> page : content.pages())
			if (!page.isEmpty())
				return false;
		return true;
	}

	/**
	 * Server side only: client vaults do not hold their inventory. Writes the info and tells the
	 * player; a non-blank clipboard is left untouched.
	 */
	public static void write(Level level, BlockPos pos, ItemVaultBlockEntity vault, Player player, ItemStack clipboard) {
		ClipboardContent content = clipboard.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
		if (!isBlank(content)) {
			player.sendOverlayMessage(CreateLang.translate("clipboard.vault_info.not_blank")
				.style(ChatFormatting.RED)
				.component());
			return;
		}

		ItemVaultBlockEntity controller = vault.getControllerBE();
		ResourceHandler<ItemResource> inv = level.getCapability(Capabilities.Item.BLOCK, pos, null);
		if (controller == null || inv == null)
			return;

		int totalSlots = inv.size();
		int usedSlots = 0;
		// same measure as the comparator output: how full each slot is, averaged over all slots
		float fill = 0;
		for (int slot = 0; slot < totalSlots; slot++) {
			ItemStack stack = ItemUtil.getStack(inv, slot);
			if (stack.isEmpty())
				continue;
			usedSlots++;
			int limit = Math.min(inv.getCapacityAsInt(slot, ItemResource.EMPTY), stack.getMaxStackSize());
			fill += limit > 0 ? (float) stack.getCount() / limit : 1;
		}
		int fillPercent = totalSlots == 0 ? 0 : Math.round(fill / totalSlots * 100);

		int width = controller.getWidth();
		String size = width + "x" + width + "x" + controller.getHeight();

		List<ClipboardEntry> page = new ArrayList<>();
		page.add(entry(level.getBlockState(pos).getBlock().getName().copy()));
		page.add(entry(CreateLang.translate("clipboard.vault_info.size", size).component()));
		page.add(entry(CreateLang.translate("clipboard.vault_info.capacity", totalSlots).component()));
		page.add(entry(CreateLang.translate("clipboard.vault_info.slots_free", totalSlots - usedSlots).component()));
		page.add(entry(CreateLang.translate("clipboard.vault_info.fill", fillPercent + "%").component()));

		List<List<ClipboardEntry>> pages = new ArrayList<>();
		pages.add(page);
		clipboard.set(AllDataComponents.CLIPBOARD_CONTENT, content.setPages(pages).setType(ClipboardType.WRITTEN));

		player.sendOverlayMessage(CreateLang.translate("clipboard.vault_info.written")
			.style(ChatFormatting.GREEN)
			.component());
	}

	private static ClipboardEntry entry(MutableComponent text) {
		return new ClipboardEntry(false, text);
	}
}
