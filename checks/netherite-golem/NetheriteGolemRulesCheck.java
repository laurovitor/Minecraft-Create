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
  System.out.println("Netherite policy checks passed");
 }
}
