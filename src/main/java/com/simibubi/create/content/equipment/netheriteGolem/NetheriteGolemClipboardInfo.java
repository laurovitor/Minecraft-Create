package com.simibubi.create.content.equipment.netheriteGolem;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides.ClipboardType;
import com.simibubi.create.content.logistics.vault.ItemVaultClipboardInfo;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A snapshot of the live golem and its home inventory; never overwrites player notes. */
public class NetheriteGolemClipboardInfo {
	public static void write(Player player, ItemStack clipboard, NetheriteGolem golem,
							 NetheriteGolemChestBlockEntity chest) {
		ClipboardContent content = clipboard.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
		if (!ItemVaultClipboardInfo.isBlank(content)) {
			player.sendOverlayMessage(CreateLang.translate("clipboard.golem_info.not_blank")
				.style(ChatFormatting.RED).component());
			return;
		}

		int used = 0;
		for (int slot = 0; slot < chest.getContainer().getContainerSize(); slot++)
			if (!chest.getContainer().getItem(slot).isEmpty())
				used++;
		int total = chest.getContainer().getContainerSize();
		float health = golem.getHealth();
		float max = golem.getMaxHealth();
		int healthPercent = max <= 0 ? 0 : Math.round(health / max * 100);

		List<ClipboardEntry> page = new ArrayList<>();
		page.add(entry(Component.translatable("entity.create.netherite_golem")));
		page.add(entry(CreateLang.translate("clipboard.golem_info.health",
			String.format(java.util.Locale.ROOT, "%.1f", health),
			String.format(java.util.Locale.ROOT, "%.1f", max), healthPercent).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.days", golem.getDaysSinceCreation()).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.attack", golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.armor", golem.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.kills", golem.getMonsterKills()).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.capacity", total).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.used", used).component()));
		page.add(entry(CreateLang.translate("clipboard.golem_info.free", total - used).component()));
		clipboard.set(AllDataComponents.CLIPBOARD_CONTENT,
			content.setPages(List.of(page)).setType(ClipboardType.WRITTEN));
		player.sendOverlayMessage(CreateLang.translate("clipboard.golem_info.written")
			.style(ChatFormatting.GREEN).component());
	}

	private static ClipboardEntry entry(MutableComponent text) {
		return new ClipboardEntry(false, text);
	}
}
