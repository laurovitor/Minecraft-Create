package com.simibubi.create.content.equipment.andesiteGolem;

/** Position checks used at the final work transition, independent of nav path node precision. */
final class GolemWorkGeometry {

	private GolemWorkGeometry() {}

	/** Species with a supported complete 2x2 trunk; ordinary lone logs stay single-trunk.
	 * Bark-on-all-sides wood blocks count too: giant/mod trees often build trunks from them. */
	static boolean canHaveWideTrunk(String blockPath) {
		String wood = woodOf(blockPath);
		return wood != null && switch (wood) {
		case "oak", "dark_oak", "pale_oak", "spruce", "jungle" -> true;
		default -> false;
		};
	}

	/** dark_oak_log / dark_oak_wood -> dark_oak; crimson_stem / crimson_hyphae -> crimson; else null. */
	static String woodOf(String blockPath) {
		for (String suffix : new String[] { "_log", "_wood", "_stem", "_hyphae" })
			if (blockPath.endsWith(suffix) && blockPath.length() > suffix.length())
				return blockPath.substring(0, blockPath.length() - suffix.length());
		return null;
	}

	/** A navigation path may report a partial route despite ending within resting reach. */
	static boolean closeToBed(double x, double y, double z, int bedX, int bedY, int bedZ) {
		return Math.abs(y - bedY) <= 1.0
			&& Math.pow(x - (bedX + 0.5), 2) + Math.pow(z - (bedZ + 0.5), 2) <= 1.2 * 1.2;
	}

	static boolean atOuterTreeFace(double golemX, double golemY, double golemZ,
		int trunkX, int trunkY, int trunkZ) {
		if (golemY > trunkY + 2 || golemY < trunkY - 2)
			return false;
		double dx = Math.abs(golemX - (trunkX + 0.5));
		double dz = Math.abs(golemZ - (trunkZ + 0.5));
		return Math.max(dx, dz) >= 0.8 && Math.max(dx, dz) <= 2.0
			&& Math.min(dx, dz) <= 1.0;
	}

	static boolean onPaveColumn(double golemX, double golemY, double golemZ,
		int feetX, int feetY, int feetZ) {
		return Math.floor(golemX) == feetX && Math.floor(golemZ) == feetZ
			&& Math.abs(golemY - feetY) <= 1.0;
	}
}
