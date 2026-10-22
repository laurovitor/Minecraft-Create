package com.simibubi.create.content.equipment.redstoneGolem;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lauro edition: which golems are heading to which source chest. Pure (no Minecraft types), so
 * the offline simulation exercises the same code. A claim expires RESERVATION_TTL ticks after its
 * last renewal; a clock that went backwards (another world loaded in the same JVM) also expires it.
 */
public final class SourceReservations<K> {
	private final Map<K, Map<UUID, Long>> claims = new HashMap<>();

	/** Claim or renew {@code key} for {@code golem}; one claim per golem (a new key drops the old). */
	public void claim(K key, UUID golem, long now) {
		release(golem);
		claims.computeIfAbsent(key, k -> new HashMap<>()).put(golem, now + RedstoneGolemRules.RESERVATION_TTL);
	}

	/** Every claim of this golem. */
	public void release(UUID golem) {
		claims.values().forEach(m -> m.remove(golem));
		claims.values().removeIf(Map::isEmpty);
	}

	/** Live claims on {@code key} by golems other than {@code self}. */
	public int othersOn(K key, UUID self, long now) {
		Map<UUID, Long> m = claims.get(key);
		if (m == null)
			return 0;
		m.values().removeIf(until -> until < now || until - now > RedstoneGolemRules.RESERVATION_TTL);
		int n = 0;
		for (UUID id : m.keySet())
			if (!id.equals(self))
				n++;
		return n;
	}
}
