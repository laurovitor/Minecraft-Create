import com.simibubi.create.content.equipment.netheriteGolem.NetheriteGolemRules;
public class NetheriteGolemRulesCheck {
 public static void main(String[] args) {
  var r = NetheriteGolemRules.class;
  assert NetheriteGolemRules.inTerritory(25,0,0);
  assert !NetheriteGolemRules.inTerritory(25,0,1);
  assert !NetheriteGolemRules.inTerritory(0,26,0);
  assert NetheriteGolemRules.isDay(6000) && !NetheriteGolemRules.isDay(18000);
  assert NetheriteGolemRules.shouldTarget(true,true,true);
  assert !NetheriteGolemRules.shouldTarget(true,true,false);
  assert !NetheriteGolemRules.shouldTarget(false,true,true);
  assert NetheriteGolemRules.acceptsLoot(true,false); // No filter: every kill drop accepted.
  assert NetheriteGolemRules.acceptsLoot(false,true);
  assert !NetheriteGolemRules.acceptsLoot(false,false); // Filter limits loot, not target.
  assert NetheriteGolemRules.movementSpeed(80,80)==.22;
  assert Math.abs(NetheriteGolemRules.movementSpeed(72,80)-.209)<1e-9;
  assert Math.abs(NetheriteGolemRules.movementSpeed(0,80)-.11)<1e-9;
  assert NetheriteGolemRules.movementSpeed(80,80)>.2;
  assert NetheriteGolemRules.killsAfterMove(9,true)==9;
  assert NetheriteGolemRules.killsAfterMove(9,false)==0;
  assert NetheriteGolemRules.deathIngots(false,true)==0;
  assert NetheriteGolemRules.deathIngots(true,false)==1;
  assert NetheriteGolemRules.deathIngots(true,true)==2;
  // Sleep regeneration: only asleep; 0 -> 80 HP needs more than one maximal day of sleep.
  float max = 80f;
  assert NetheriteGolemRules.sleepRegen(false, max) == 0f : "no regen while awake";
  double perTick = NetheriteGolemRules.sleepRegen(true, max) / NetheriteGolemRules.REGEN_INTERVAL;
  assert Math.abs(perTick - 80.0 / 22000) < 1e-9;
  double oneDay = perTick * NetheriteGolemRules.DAY_WINDOW_TICKS;
  assert oneDay < max : "a perfect single day of sleep must not fully heal: " + oneDay;
  assert perTick * 2 * NetheriteGolemRules.DAY_WINDOW_TICKS >= max : "two full day windows must suffice";
  assert Math.abs(perTick * 2 * 11000 - max) < 1e-3 : "two realistic ~11000-tick sleeps = full heal";
  float hp = 0.5f; int asleepTicks = 0;
  while (hp < max) { asleepTicks += NetheriteGolemRules.REGEN_INTERVAL; hp = Math.min(max, hp + NetheriteGolemRules.sleepRegen(true, max)); }
  assert asleepTicks > NetheriteGolemRules.DAY_WINDOW_TICKS && asleepTicks <= 22000 : "ticks " + asleepTicks;
  assert NetheriteGolemRules.movementSpeed(hp, max) == .22 : "speed follows healed health";
  // Rest/wake policy
  assert NetheriteGolemRules.wantsRest(true, false, 0);
  assert !NetheriteGolemRules.wantsRest(true, true, 0) : "hostile in area keeps it awake";
  assert !NetheriteGolemRules.wantsRest(true, false, 1) : "recent damage keeps it awake";
  assert !NetheriteGolemRules.wantsRest(false, false, 0) : "night patrol";
  // Away timer and teleport threshold (one full day, strictly more than 24000 ticks)
  assert NetheriteGolemRules.nearChest(0, 1, 0) && NetheriteGolemRules.nearChest(1, 0, -1);
  assert !NetheriteGolemRules.nearChest(2, 0, 0) && !NetheriteGolemRules.nearChest(0, -2, 0) && !NetheriteGolemRules.nearChest(0, 3, 0);
  long away = 0;
  for (int t = 0; t < 24000; t++) away = NetheriteGolemRules.nextAwayTicks(away, false);
  assert away == 24000 && !NetheriteGolemRules.shouldTeleportHome(away) : "exactly one day is not yet over";
  away = NetheriteGolemRules.nextAwayTicks(away, false);
  assert NetheriteGolemRules.shouldTeleportHome(away);
  assert NetheriteGolemRules.nextAwayTicks(away, true) == 0 : "visiting the chest resets the timer";
  assert !NetheriteGolemRules.shouldTeleportHome(NetheriteGolemRules.AWAY_TELEPORT_TICKS - NetheriteGolemRules.TELEPORT_RETRY_TICKS)
   : "failed teleport retries later, not every tick";
  // Eyes: closed asleep, purple fighting, white calm
  assert NetheriteGolemRules.eyes(true, false) == NetheriteGolemRules.Eyes.CLOSED;
  assert NetheriteGolemRules.eyes(true, true) == NetheriteGolemRules.Eyes.CLOSED;
  assert NetheriteGolemRules.eyes(false, true) == NetheriteGolemRules.Eyes.PURPLE;
  assert NetheriteGolemRules.eyes(false, false) == NetheriteGolemRules.Eyes.WHITE;
  assert NetheriteGolemRules.inCombat(true, -1);
  assert NetheriteGolemRules.inCombat(false, 99) && !NetheriteGolemRules.inCombat(false, 100);
  assert !NetheriteGolemRules.inCombat(false, -1) : "never attacked = calm";
  // Repair: one item = 25% of max health; Andesite alloy 3 of 12, netherite ingot 20 of 80.
  assert com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.repairAmount(12f) == 3f;
  assert com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.repairAmount(80f) == 20f;
  // Auto-repair from the chest: needs a full step missing, out of combat, 100 ticks calm, no cooldown.
  assert com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(60f, 80f, false, 100, 0);
  assert !com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(61f, 80f, false, 100, 0) : "would waste an ingot";
  assert !com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(10f, 80f, true, 1000, 0) : "never during combat";
  assert !com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(10f, 80f, false, 99, 0) : "just hit";
  assert !com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(10f, 80f, false, 1000, 1) : "cooldown";
  assert com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.shouldAutoRepair(9f, 12f, false, 1000, 0) : "Andesite: 3 HP missing";
  // Andesite sleep regen: same two-sleeps rule; a full 11000-tick night heals half of 12 HP.
  double andesitePerTick = com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.sleepRegen(true, 12f)
   / com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.REGEN_INTERVAL;
  assert Math.abs(andesitePerTick * 11000 - 6.0) < 1e-3 && andesitePerTick * 11000 < 12;
  assert com.simibubi.create.content.equipment.andesiteGolem.GolemCareRules.sleepRegen(false, 12f) == 0f;
  // Combat exit: unseen target dropped after 100 ticks, so eyes return to white.
  assert NetheriteGolemRules.keepUnseenTarget(true, 99) && !NetheriteGolemRules.keepUnseenTarget(true, 100);
  assert !NetheriteGolemRules.keepUnseenTarget(false, 0);
  assert NetheriteGolemRules.eyes(false, NetheriteGolemRules.inCombat(false, -1)) == NetheriteGolemRules.Eyes.WHITE;
  System.out.println("Netherite policy checks passed");
 }
}
