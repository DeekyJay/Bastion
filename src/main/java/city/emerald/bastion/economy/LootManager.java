package city.emerald.bastion.economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.wave.WaveManager;

public class LootManager {

  private final Bastion plugin;
  private final WaveManager waveManager;
  private final Random random;

  // Helper class for loot table entries
  private static class LootTableEntry {
    final Material material;
    final double probability;
    final int maxAmount;

    LootTableEntry(Material material, double probability, int maxAmount) {
      this.material = material;
      this.probability = probability;
      this.maxAmount = maxAmount;
    }
  }

  // New loot tables using LootTableEntry helper class
  // The items in these tables are in-addition to the default drops
  // Common drops are always available, bonus drops are rarer
  private static final Map<EntityType, List<LootTableEntry>> COMMON_LOOT_TABLE = new HashMap<>();
  private static final Map<EntityType, List<LootTableEntry>> BONUS_LOOT_TABLE = new HashMap<>();

  static {
    // Common Loot
    COMMON_LOOT_TABLE.put(EntityType.ZOMBIE, Arrays.asList(
      new LootTableEntry(Material.IRON_NUGGET, 0.50, 3), // 50% chance for 1-3 iron nuggets
      new LootTableEntry(Material.COAL, 0.25, 2)         // 25% chance for 1-2 coal
    ));
    COMMON_LOOT_TABLE.put(EntityType.SKELETON, Arrays.asList(
      new LootTableEntry(Material.ARROW, 0.80, 5),      // 80% chance for 1-5 arrows
      new LootTableEntry(Material.GOLD_NUGGET, 0.10, 2)  // 10% chance for 1-2 gold nuggets
    ));
    COMMON_LOOT_TABLE.put(EntityType.SPIDER, Arrays.asList(
      new LootTableEntry(Material.STRING, 0.75, 3)       // 75% chance for 1-3 string
    ));
    COMMON_LOOT_TABLE.put(EntityType.CREEPER, Arrays.asList(
      new LootTableEntry(Material.GUNPOWDER, 0.90, 4)   // 90% chance for 1-4 gunpowder
    ));

    // Bonus Loot (rarer items)
    BONUS_LOOT_TABLE.put(EntityType.ZOMBIE, Arrays.asList(
      new LootTableEntry(Material.IRON_INGOT, 0.10, 1),  // 10% chance for 1 iron ingot
      new LootTableEntry(Material.GOLD_NUGGET, 0.05, 5)  // 5% chance for 1-5 gold nuggets
    ));
    BONUS_LOOT_TABLE.put(EntityType.SKELETON, Arrays.asList(
      new LootTableEntry(Material.TIPPED_ARROW, 0.15, 2), // 15% chance for 1-2 tipped arrows
      new LootTableEntry(Material.BONE_BLOCK, 0.05, 1)    // 5% chance for 1 bone block
    ));
    BONUS_LOOT_TABLE.put(EntityType.SPIDER, Arrays.asList(
      new LootTableEntry(Material.COBWEB, 0.10, 2),       // 10% chance for 1-2 cobwebs
      new LootTableEntry(Material.SPIDER_EYE, 0.20, 1)    // 20% chance for 1 spider eye
    ));
    BONUS_LOOT_TABLE.put(EntityType.CREEPER, Arrays.asList(
      new LootTableEntry(Material.TNT, 0.05, 1),          // 5% chance for 1 TNT
      new LootTableEntry(Material.DIAMOND, 0.01, 1)       // 1% chance for 1 diamond
    ));
  }

  public LootManager(Bastion plugin, WaveManager waveManager) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.random = new Random();
  }

  public void handleMobDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    EntityType entityType = entity.getType();

    // For now, let's use a default multiplier and always check for bonus items.
    // This can be customized later based on game state or other factors.
    double multiplier = 1.0; 
    boolean includeBonusItems = true;

    List<ItemStack> customLoot = generateLoot(entityType, multiplier, includeBonusItems);
    event.getDrops().addAll(customLoot);
  }

  /**
   * Generates a list of custom items to add to a mob's drops based on probability.
   * @param entityType The type of the mob.
   * @param multiplier A multiplier for the amount of loot.
   * @param includeBonusItems Whether to include a chance for bonus loot.
   * @return A list of ItemStacks to be added to the drops.
   */
  public List<ItemStack> generateLoot(
    EntityType entityType,
    double multiplier,
    boolean includeBonusItems
  ) {
    List<ItemStack> loot = new ArrayList<>();

    // Process a given loot table
    processLootTable(loot, COMMON_LOOT_TABLE.get(entityType), multiplier);
    
    if (includeBonusItems) {
      processLootTable(loot, BONUS_LOOT_TABLE.get(entityType), multiplier);
    }

    return loot;
  }

  private void processLootTable(List<ItemStack> loot, List<LootTableEntry> table, double multiplier) {
    if (table == null) {
      return;
    }

    for (LootTableEntry entry : table) {
      // Check if the item should be dropped based on probability
      if (random.nextDouble() < entry.probability) {
        // Determine a random quantity from 1 to maxAmount
        int baseAmount = random.nextInt(entry.maxAmount) + 1;
        
        // Apply the multiplier and round to the nearest whole number
        int finalAmount = (int) Math.round(baseAmount * multiplier);

        if (finalAmount > 0) {
          loot.add(new ItemStack(entry.material, finalAmount));
        }
      }
    }
  }

  /**
   * Calculates bonus emeralds based on current wave
   */
  public int calculateWaveBonus() {
    int baseEmeralds = 5;
    int waveBonus = waveManager.getCurrentWave();
    return baseEmeralds + (int) (waveBonus * 1.5);
  }
}
