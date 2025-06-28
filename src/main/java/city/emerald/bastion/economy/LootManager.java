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
    // Tier 1: Foundational Crafting (Probability: 0.05)
    COMMON_LOOT_TABLE.put(EntityType.ZOMBIE, Arrays.asList(
      new LootTableEntry(Material.LEATHER, 0.05, 2),
      new LootTableEntry(Material.IRON_NUGGET, 0.05, 4)
    ));
    COMMON_LOOT_TABLE.put(EntityType.SKELETON, Arrays.asList(
      new LootTableEntry(Material.OAK_LOG, 0.05, 2),
      new LootTableEntry(Material.COAL, 0.05, 3)
    ));
    COMMON_LOOT_TABLE.put(EntityType.SPIDER, Arrays.asList(
      new LootTableEntry(Material.SUGAR_CANE, 0.05, 3),
      new LootTableEntry(Material.COBBLESTONE, 0.05, 4)
    ));

    // Tier 2: Advanced Crafting & Enchanting (Probability: 0.02)
    COMMON_LOOT_TABLE.put(EntityType.CREEPER, Arrays.asList(
      new LootTableEntry(Material.DIAMOND, 0.02, 1),
      new LootTableEntry(Material.REDSTONE, 0.02, 5)
    ));
    COMMON_LOOT_TABLE.put(EntityType.ENDERMAN, Arrays.asList(
      new LootTableEntry(Material.OBSIDIAN, 0.02, 2),
      new LootTableEntry(Material.NETHER_WART, 0.02, 2)
    ));
    COMMON_LOOT_TABLE.put(EntityType.WITCH, Arrays.asList(
      new LootTableEntry(Material.LAPIS_LAZULI, 0.02, 6),
      new LootTableEntry(Material.BLAZE_ROD, 0.02, 1)
    ));

    // Tier 3: Bonus Drops (Probability: 0.03)
    // Note: This tier currently uses simple materials. A future enhancement
    // could allow for enchanted items or specific potions.
    BONUS_LOOT_TABLE.put(EntityType.ZOMBIE, Arrays.asList(
      new LootTableEntry(Material.GOLDEN_APPLE, 0.03, 1)
    ));
    BONUS_LOOT_TABLE.put(EntityType.SKELETON, Arrays.asList(
      new LootTableEntry(Material.EXPERIENCE_BOTTLE, 0.03, 3)
    ));
    BONUS_LOOT_TABLE.put(EntityType.SPIDER, Arrays.asList(
      new LootTableEntry(Material.VILLAGER_SPAWN_EGG, 0.03, 1)
    ));
    BONUS_LOOT_TABLE.put(EntityType.CREEPER, Arrays.asList(
      new LootTableEntry(Material.TNT, 0.03, 2)
    ));
    BONUS_LOOT_TABLE.put(EntityType.ENDERMAN, Arrays.asList(
      new LootTableEntry(Material.TOTEM_OF_UNDYING, 0.03, 1)
    ));
    BONUS_LOOT_TABLE.put(EntityType.WITCH, Arrays.asList(
      new LootTableEntry(Material.GHAST_TEAR, 0.03, 1)
    ));
  }

  public LootManager(Bastion plugin, WaveManager waveManager) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.random = new Random();
  }

  public void handleMobDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.getKiller() == null) {
      return; // Don't drop custom loot if not killed by a player
    }
  
    EntityType entityType = entity.getType();

    // For now, let's use a default multiplier and always check for bonus items.
    // This can be customized later based on game state or other factors.
    int currentWave = waveManager.getCurrentWave();
    double multiplier = 1.0 + (0.1 * currentWave);
    boolean includeBonusItems = currentWave % 5 == 0; // Include bonus items every 5 waves

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
