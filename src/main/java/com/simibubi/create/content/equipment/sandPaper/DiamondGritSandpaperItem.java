package com.simibubi.create.content.equipment.sandPaper;

import java.util.function.Consumer;

import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItemRenderer;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;


public class DiamondGritSandpaperItem extends SandPaperItem {
	public DiamondGritSandpaperItem(Properties properties) {
		super(properties);
	}
	
	@Override
	public int getMaxDamage(ItemStack stack) {
		return AllConfigs.server().electricity.diamondGritSandpaperUses.get();
	}

	// This needs to be redone OnlyIn is never recommended
	@Override
	@OnlyIn(Dist.CLIENT)
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(SimpleCustomRenderer.create(this, new SandPaperItemRenderer()));
	}
}
