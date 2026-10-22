package com.simibubi.create.content.electricity.energy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum WireConnectResult {

	LINKED(Component.translatable("statusbar.create.wire.linked")),
	LINKED_IN(Component.translatable("statusbar.create.wire.linked_in")),
	LINKED_OUT(Component.translatable("statusbar.create.wire.linked_out")),

	CONNECT(Component.translatable("statusbar.create.wire.connect")),
	CONNECT_IN(Component.translatable("statusbar.create.wire.connect_in")),
	CONNECT_OUT(Component.translatable("statusbar.create.wire.connect_out")),

	LONG(Component.translatable("statusbar.create.wire.long").withStyle(ChatFormatting.RED)),
	OBSTRUCTED(Component.translatable("statusbar.create.wire.obstructed").withStyle(ChatFormatting.RED)),
	COUNT(Component.translatable("statusbar.create.wire.count").withStyle(ChatFormatting.RED)),
	REMOVED(Component.translatable("statusbar.create.wire.removed")),
	EXISTS(Component.translatable("statusbar.create.wire.exists").withStyle(ChatFormatting.RED)),
	NO_CONNECTION(Component.translatable("statusbar.create.wire.no_connection").withStyle(ChatFormatting.RED)),
	INVALID(Component.translatable("statusbar.create.wire.invalid").withStyle(ChatFormatting.RED)),
	REQUIRES_HIGH_CURRENT(Component.translatable("statusbar.create.wire.requires_high_current").withStyle(ChatFormatting.RED));

	private final Component message;

	WireConnectResult(Component message) {
		this.message = message;
	}

	public Component getMessage() {
		return message;
	}

	public boolean isLinked() {
		return this == LINKED || this == LINKED_IN || this == LINKED_OUT;
	}

	public boolean isConnect() {
		return this == CONNECT || this == CONNECT_IN || this == CONNECT_OUT;
	}

	public static WireConnectResult getLink(boolean in, boolean out) {
		if(in && !out)
			return LINKED_IN;
		if(!in && out)
			return LINKED_OUT;
		return LINKED;
	}

	public static WireConnectResult getConnect(boolean in, boolean out) {
		if(in && !out)
			return CONNECT_IN;
		if(!in && out)
			return CONNECT_OUT;
		return CONNECT;
	}
}
