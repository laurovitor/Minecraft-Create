package com.simibubi.create.content.equipment.andesiteGolem;

/** Position checks used at the final work transition, independent of nav path node precision. */
final class GolemWorkGeometry {

	private GolemWorkGeometry() {}

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
