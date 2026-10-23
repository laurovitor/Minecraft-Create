package com.simibubi.create.content.electricity.energy;

import com.simibubi.create.AllItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;;

public enum WireType {
	COPPER(0, 256, 188, 101, 65, AllItems.COPPER_WIRE.asStack(4), AllItems.COPPER_SPOOL.asStack()),
	GOLD(1, 1024, 235, 191, 65, AllItems.GOLD_WIRE.asStack(4), AllItems.GOLD_SPOOL.asStack()),
	ELECTRUM(2, 8196, 218, 178, 115, AllItems.ELECTRUM_WIRE.asStack(4), AllItems.ELECTRUM_SPOOL.asStack()),
	FESTIVE(3, 256, 36, 133, 46, AllItems.COPPER_WIRE.asStack(4), AllItems.FESTIVE_SPOOL.asStack()),
	IRON(4, 256, 172, 183, 192, AllItems.IRON_WIRE.asStack(4), AllItems.IRON_SPOOL.asStack());

	private final int ID, TRANSFER, CR, CG, CB;
	private final ItemStack DROP;
	private final ItemStack SOURCE_DROP;

	WireType(int id, int transfer, int red, int green, int blue, ItemStack drop, ItemStack source) {
		ID = id;
		TRANSFER = transfer;
		CR = red;
		CG = green;
		CB = blue;
		DROP = drop;
		SOURCE_DROP = source;
	}

	public static WireType fromIndex(int index) {
		return switch (index) {
			case 0 -> COPPER;
			case 1 -> GOLD;
			case 2 -> ELECTRUM;
			case 3 -> FESTIVE;
			case 4 -> IRON;
			default -> null;
		};
	}

	public int getIndex() {
		return ID;
	}

	public ItemStack getDrop() {
		return DROP.copy();
	}

	public ItemStack getSourceDrop() {
		return SOURCE_DROP.copy();
	}

	public int transfer() {
		return TRANSFER;
	}

	public int getRed() {
		return CR;
	}

	public int getGreen() {
		return CG;
	}

	public int getBlue() {
		return CB;
	}

	public boolean isFestive( ) {
		return this == FESTIVE;
	}

	public static WireType of(Item item) {
		if(item == AllItems.COPPER_SPOOL.get())
			return WireType.COPPER;
		if(item == AllItems.GOLD_SPOOL.get())
			return WireType.GOLD;
		if(item == AllItems.FESTIVE_SPOOL.get())
			return WireType.FESTIVE;
		if(item == AllItems.IRON_SPOOL.get())
			return WireType.IRON;
		if(item == AllItems.ELECTRUM_SPOOL.get())
			return WireType.ELECTRUM;
		return WireType.COPPER;
	}
}
