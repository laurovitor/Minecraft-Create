package com.simibubi.create.content.equipment.andesiteGolem;

/** Standalone deterministic regression for the two arrival gates; run with java -ea.
 * Compile this file together with GolemWorkGeometry.java using javac, then run this main class.
 * This checks the transition predicates; it does not simulate Minecraft navigation or gameplay.
 */
public class GolemWorkGeometryCheck {
	private static boolean at2x2(double x, double y, double z) {
		for (int tx = 0; tx < 2; tx++)
			for (int tz = 0; tz < 2; tz++)
				if (GolemWorkGeometry.atOuterTreeFace(x, y, z, tx, 64, tz))
					return true;
		return false;
	}

	public static void main(String[] args) {
		assert GolemWorkGeometry.closeToBed(1.5, 65, 0.5, 1, 65, 0) : "partial path at bedside must be usable";
		assert !GolemWorkGeometry.closeToBed(4.5, 65, 0.5, 1, 65, 0) : "distant partial route must not qualify";
		assert GolemWorkGeometry.canHaveWideTrunk("oak_log") : "complete planted giant oak must use 2x2 arrival";
		assert GolemWorkGeometry.canHaveWideTrunk("dark_oak_log") : "dark oak retains 2x2 arrival";
		assert !GolemWorkGeometry.canHaveWideTrunk("birch_log") : "ordinary species must not merge adjacent trees";
		assert at2x2(-0.45, 64, 0.5) : "standing at dark-oak west face must enter WORK";
		assert at2x2(1.5, 64, 2.45) : "standing at south face must enter WORK";
		assert !at2x2(1.0, 64, 1.0) : "inside trunk is not a usable perimeter";
		assert !at2x2(-5.0, 64, 0.5) : "distant golem must not cut";
		assert !at2x2(-0.45, 69, 0.5) : "wrong elevation must not cut";
		assert GolemWorkGeometry.onPaveColumn(1.45, 65, 0.55, 1, 65, 0)
			: "golem on adjacent dirt at chest height must pave";
		assert GolemWorkGeometry.onPaveColumn(1.45, 64, 0.55, 1, 64, 0)
			: "golem on dirt one below chest must pave";
		assert !GolemWorkGeometry.onPaveColumn(0.5, 65, 0.5, 1, 65, 0)
			: "golem atop chest must walk to perimeter first";
		assert !GolemWorkGeometry.onPaveColumn(2.5, 65, 0.5, 1, 65, 0)
			: "distant ground must remain untouched";
	}
}
