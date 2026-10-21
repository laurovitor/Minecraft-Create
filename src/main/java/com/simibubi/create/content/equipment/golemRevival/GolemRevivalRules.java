package com.simibubi.create.content.equipment.golemRevival;

/** Lauro edition: side-effect-free decision for a revival pumpkin being placed; checked offline. */
public final class GolemRevivalRules {
	private GolemRevivalRules() {}

	public enum Outcome {
		/** Not on top of a golem chest: an ordinary decorative block. */
		DECORATIVE,
		/** On an unbound chest of its own material: becomes that chest's new golem. */
		REVIVE,
		/** The chest is bound to a golem (alive, or possibly alive in an unloaded chunk): refused. */
		REFUSE_HAS_GOLEM,
		/** A golem chest of another material: refused. */
		REFUSE_WRONG_MATERIAL
	}

	/**
	 * @param onGolemChest   the block below is a chest of any golem kind
	 * @param sameKind       ... of the pumpkin's own kind
	 * @param boundGolem     the chest holds a golem UUID (synced to clients)
	 * @param boundKnownDead that UUID is recorded as died while the chest was unloaded (server only)
	 */
	public static Outcome decide(boolean onGolemChest, boolean sameKind, boolean boundGolem, boolean boundKnownDead) {
		if (!onGolemChest)
			return Outcome.DECORATIVE;
		if (!sameKind)
			return Outcome.REFUSE_WRONG_MATERIAL;
		return boundGolem && !boundKnownDead ? Outcome.REFUSE_HAS_GOLEM : Outcome.REVIVE;
	}
}
