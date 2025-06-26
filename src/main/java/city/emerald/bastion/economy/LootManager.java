package city.emerald.bastion.economy;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.wave.WaveManager;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class LootManager {

  private final Bastion plugin;
  private final WaveManager waveManager;
  private final Random random;

  // Drop tables for different mob types
  private static final Map<EntityType, Map<Material, Double>> MOB_DROP_TABLES = new HashMap<>();
  private static final Map<EntityType, Map<Material, Double>> ELITE_DROP_TABLES = new HashMap<>();
  private static final Map<Material, Double> BOSS_DROP_TABLE = new HashMap<>();

  static {
    // Basic mob drops
    Map<Material, Double> zombieDrops = new HashMap<>();
    zombieDrops.put(Material.ROTTEN_FLESH, 0.8);
    zombieDrops.put(Material.EMERALD, 0.1);
    zombieDrops.put(Material.IRON_INGOT, 0.05);

    Map<Material, Double> skeletonDrops = new HashMap<>();
    skeletonDrops.put(Material.BONE, 0.8);
    skeletonDrops.put(Material.ARROW, 0.6);
    skeletonDrops.put(Material.EMERALD, 0.1);
    skeletonDrops.put(Material.BOW, 0.05);

    MOB_DROP_TABLES.put(EntityType.ZOMBIE, zombieDrops);
    MOB_DROP_TABLES.put(EntityType.SKELETON, skeletonDrops);

    // Elite mob drops (higher value items)
    Map<Material, Double> eliteZombieDrops = new HashMap<>();
    eliteZombieDrops.put(Material.EMERALD_BLOCK, 0.3);
    eliteZombieDrops.put(Material.DIAMOND, 0.1);
    eliteZombieDrops.put(Material.ENCHANTED_GOLDEN_APPLE, 0.05);

    Map<Material, Double> eliteSkeletonDrops = new HashMap<>();
    eliteSkeletonDrops.put(Material.EMERALD_BLOCK, 0.3);
    eliteSkeletonDrops.put(Material.DIAMOND, 0.1);
    eliteSkeletonDrops.put(Material.ENCHANTED_BOOK, 0.15);

    ELITE_DROP_TABLES.put(EntityType.ZOMBIE, eliteZombieDrops);
    ELITE_DROP_TABLES.put(EntityType.SKELETON, eliteSkeletonDrops);

    // Boss drops (guaranteed valuable items)
    BOSS_DROP_TABLE.put(Material.EMERALD_BLOCK, 1.0);
    BOSS_DROP_TABLE.put(Material.DIAMOND_BLOCK, 0.5);
    BOSS_DROP_TABLE.put(Material.NETHERITE_INGOT, 0.3);
    BOSS_DROP_TABLE.put(Material.ENCHANTED_GOLDEN_APPLE, 0.4);
  }

  public LootManager(Bastion plugin, WaveManager waveManager) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.random = new Random();
  }

  /**
   * Generates and drops loot for a killed mob
   * @param entity The killed mob
   * @param isElite Whether the mob was an elite variant
   * @param isBoss Whether the mob was a boss
   */
  public void generateLoot(
    LivingEntity entity,
    boolean isElite,
    boolean isBoss
  ) {
    Map<Material, Double> dropTable;

    if (isBoss) {
      dropTable = BOSS_DROP_TABLE;
    } else if (isElite) {
      dropTable =
        ELITE_DROP_TABLES.getOrDefault(entity.getType(), new HashMap<>());
    } else {
      dropTable =
        MOB_DROP_TABLES.getOrDefault(entity.getType(), new HashMap<>());
    }

    // Apply wave scaling to drop rates
    double waveMultiplier = 1.0 + (waveManager.getCurrentWave() * 0.1);

    for (Map.Entry<Material, Double> entry : dropTable.entrySet()) {
      double adjustedRate = entry.getValue() * waveMultiplier;
      if (random.nextDouble() < adjustedRate) {
        ItemStack item = createItem(entry.getKey(), isElite, isBoss);
        entity.getWorld().dropItemNaturally(entity.getLocation(), item);
      }
    }

    // Always drop some emeralds for boss kills
    if (isBoss) {
      int emeraldCount = 5 + random.nextInt(waveManager.getCurrentWave() * 2);
      entity
        .getWorld()
        .dropItemNaturally(
          entity.getLocation(),
          new ItemStack(Material.EMERALD, emeraldCount)
        );
    }
  }

  /**
   * Creates an item with appropriate enchantments based on mob type
   */
  private ItemStack createItem(
    Material material,
    boolean isElite,
    boolean isBoss
  ) {
    ItemStack item = new ItemStack(material);

    // Add enchantments for weapons/armor from elite/boss mobs
    if ((isElite || isBoss) && isEnchantable(material)) {
      int maxEnchants = isBoss ? 3 : 1;
      addRandomEnchantments(item, maxEnchants);
    }

    return item;
  }

  /**
   * Checks if a material can be enchanted
   */
  private boolean isEnchantable(Material material) {
    return (
      material.name().contains("SWORD") ||
      material.name().contains("AXE") ||
      material.name().contains("BOW") ||
      material.name().contains("ARMOR") ||
      material.name().contains("HELMET") ||
      material.name().contains("CHESTPLATE") ||
      material.name().contains("LEGGINGS") ||
      material.name().contains("BOOTS")
    );
  }

  /**
   * Adds random enchantments to an item
   */
  private void addRandomEnchantments(ItemStack item, int maxEnchants) {
    List<Enchantment> possibleEnchants = new ArrayList<>(
      Arrays.asList(Enchantment.values())
    );
    Collections.shuffle(possibleEnchants);

    int enchants = 0;
    for (Enchantment enchant : possibleEnchants) {
      if (enchants >= maxEnchants) break;
      if (enchant.canEnchantItem(item)) {
        int level = random.nextInt(enchant.getMaxLevel()) + 1;
        item.addEnchantment(enchant, level);
        enchants++;
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
