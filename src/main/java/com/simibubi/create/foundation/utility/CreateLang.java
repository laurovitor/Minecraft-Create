package com.simibubi.create.foundation.utility;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.Create;

import net.createmod.catnip.api.lang.Lang;
import net.createmod.catnip.api.lang.LangBuilder;
import net.createmod.catnip.api.client.lang.LangNumberFormat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

public class CreateLang extends Lang {

	/**
	 * legacy-ish. Use CreateLang.translate and other builder methods where possible
	 *

	 */
	public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(Create.ID + "." + key, args1);
    }

	public static List<Component> translatedOptions(String prefix, String... keys) {
		List<Component> result = new ArrayList<>(keys.length);
		for (String key : keys)
			result.add(translate((prefix != null ? prefix + "." : "") + key).component());
		return result;
	}

	//

	public static CreateLangBuilder builder() {
		return new CreateLangBuilder(Create.ID);
	}

	public static CreateLangBuilder blockName(BlockState state) {
		return builder().add(state.getBlock()
				.getName());
	}

	public static CreateLangBuilder itemName(ItemStack stack) {
		return builder().add(stack.getHoverName()
				.copy());
	}

	public static CreateLangBuilder fluidName(FluidStack stack) {
		return builder().add(stack.getHoverName()
				.copy());
	}

	public static CreateLangBuilder number(double d) {
		return builder().text(LangNumberFormat.format(d));
	}

	public static CreateLangBuilder translate(String langKey, Object... args) {
		return builder().translate(langKey, args);
	}

	public static CreateLangBuilder text(String text) {
		return builder().text(text);
	}

	@Deprecated // Use while implementing and replace all references with Lang.translate
	public static CreateLangBuilder temporaryText(String text) {
		return builder().text(text);
	}

}
