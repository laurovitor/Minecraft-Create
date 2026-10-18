package com.simibubi.create.foundation.utility;

import java.util.List;

import net.createmod.catnip.api.lang.LangBuilder;
import net.createmod.catnip.api.theme.Color;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

/** Create-specific tooltip formatting removed from Catnip's common LangBuilder. */
public class CreateLangBuilder extends LangBuilder {

	public CreateLangBuilder(String namespace) {
		super(namespace);
	}

	@Override
	public CreateLangBuilder add(Component component) {
		super.add(component);
		return this;
	}

	@Override
	public CreateLangBuilder add(MutableComponent component) {
		super.add(component);
		return this;
	}

	@Override
	public CreateLangBuilder add(LangBuilder builder) {
		super.add(builder);
		return this;
	}

	@Override
	public CreateLangBuilder text(String text) {
		super.text(text);
		return this;
	}

	@Override
	public CreateLangBuilder text(ChatFormatting format, String text) {
		super.text(format, text);
		return this;
	}

	@Override
	public CreateLangBuilder text(int color, String text) {
		super.text(color, text);
		return this;
	}

	@Override
	public CreateLangBuilder translate(String key, Object... args) {
		super.translate(key, args);
		return this;
	}

	@Override
	public CreateLangBuilder style(ChatFormatting format) {
		super.style(format);
		return this;
	}

	@Override
	public CreateLangBuilder color(int color) {
		super.color(color);
		return this;
	}

	@Override
	public CreateLangBuilder color(Color color) {
		super.color(color);
		return this;
	}

	@Override
	public CreateLangBuilder space() {
		super.space();
		return this;
	}

	@Override
	public CreateLangBuilder newLine() {
		super.newLine();
		return this;
	}

	public void forGoggles(List<? super MutableComponent> tooltip) {
		forGoggles(tooltip, 0);
	}

	public void forGoggles(List<? super MutableComponent> tooltip, int indents) {
		int spaceWidth = Minecraft.getInstance().font.width(" ");
		int spaces = spaceWidth == 0 ? 4 + indents : Mth.ceil(4.0f * (4 + indents) / spaceWidth);
		tooltip.add(Component.literal(" ".repeat(Math.max(0, spaces))).append(component()));
	}
}
