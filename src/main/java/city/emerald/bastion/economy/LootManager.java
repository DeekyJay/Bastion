package city.emerald.bastion.economy;

import java.util.ArrayList;
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
import city.emerald.bastion.game.GameStateManager;

public class LootManager {

  private final Bastion plugin;
  private final GameStateManager gameStateManager;
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
  private final Map<EntityType, List<LootTableEntry>> commonLootTable;
  private final Map<EntityType, List<LootTableEntry>> bonusLootTable;


  public LootManager(Bastion plugin, GameStateManager gameStateManager) {
    this.plugin = plugin;
    this.gameStateManager = gameStateManager;
    this.random = new Random();

    // Retrieve configuration values
    double commonLootProbability = plugin.getDoubleSafe("loot_table_settings.common_loot_probability", 0.05);
    int commonLootMaxItems = plugin.getIntSafe("loot_table_settings.common_loot_max_items", 5);
    double bonusLootProbability = plugin.getDoubleSafe("loot_table_settings.bonus_loot_probability", 0.01);
    int bonusLootMaxItems = plugin.getIntSafe("loot_table_settings.bonus_loot_max_items", 2);

    // Load loot tables from config
    commonLootTable = buildLootTable("common_loot", commonLootProbability, commonLootMaxItems);
    bonusLootTable = buildLootTable("bonus_loot", bonusLootProbability, bonusLootMaxItems);
  }

  public void handleMobDeath(EntityDeathEvent event) {
    LivingEntity entity = event.getEntity();
    if (entity.getKiller() == null) {
      return; // Don't drop custom loot if not killed by a player
    }
  
    EntityType entityType = entity.getType();

    // For now, let's use a default multiplier and always check for bonus items.
    // This can be customized later based on game state or other factors.
    int currentWave = gameStateManager.getCurrentWaveNumber();
    double scalingBase = plugin.getDoubleSafe("loot_table_settings.wave_scaling_base", 0.05);
    double multiplier = Math.pow(1.0 + scalingBase, currentWave);
    boolean includeBonusItems = true; // Allow bonus loot every wave instead of only every 5th wave

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
    processLootTable(loot, commonLootTable.get(entityType), multiplier);
    
    if (includeBonusItems) {
      processLootTable(loot, bonusLootTable.get(entityType), multiplier);
    }

    return loot;
  }

  private Map<EntityType, List<LootTableEntry>> buildLootTable(String configSectionName, double probability, int maxItemCount) {
    Map<EntityType, List<LootTableEntry>> lootTable = new HashMap<>();

    // Get the specified section from the config
    if (plugin.getConfig().getConfigurationSection(configSectionName) == null) {
        plugin.getLogger().warning("Configuration section not found: " + configSectionName);
        return lootTable; // Return empty table if section is missing
    }

    Map<String, Object> configLoot = plugin.getConfig().getConfigurationSection(configSectionName).getValues(false);
    for (Map.Entry<String, Object> entry : configLoot.entrySet()) {
        try {
            // Convert the key to EntityType
            EntityType entityType = EntityType.valueOf(entry.getKey().toUpperCase());

            // Parse the loot items
            List<LootTableEntry> entries = new ArrayList<>();
            for (String item : ((String) entry.getValue()).split(",")) {
                try {
                    Material material = Material.valueOf(item.toUpperCase());
                    entries.add(new LootTableEntry(material, probability, maxItemCount));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name in config: " + item);
                }
            }

            lootTable.put(entityType, entries);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type name in config: " + entry.getKey());
        }
    }

    return lootTable;
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
    int waveBonus = gameStateManager.getCurrentWaveNumber();
    return baseEmeralds + (int) (waveBonus * 1.5);
  }
}
