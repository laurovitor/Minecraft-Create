package com.simibubi.create.content.equipment.sandPaper;

/**
 * Sandpaper made with diamond grit: polishes like regular sandpaper but lasts far longer.
 * <p>
 * Durability is an item component fixed at registration, so it is not a config option
 * (Crafts &amp; Additions used to read it from config).
 */
public class DiamondGritSandpaperItem extends SandPaperItem {

	public static final int USES = 1024;

	public DiamondGritSandpaperItem(Properties properties) {
		super(properties, USES);
	}
}
