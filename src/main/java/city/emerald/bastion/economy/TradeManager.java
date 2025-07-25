package city.emerald.bastion.economy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.wave.WaveManager;

public class TradeManager implements Listener {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private final WaveManager waveManager;
  private final Random random;

  // Trade tiers that unlock with wave progression
  private static final Map<Integer, List<TradeOffer>> TRADE_TIERS = new HashMap<>();

  private static class TradeOffer {

    final Material result;
    final Material cost1;
    final int costAmount1;
    final Material cost2;
    final int costAmount2;
    final boolean isEnchanted;

    TradeOffer(
      Material result,
      Material cost1,
      int costAmount1,
      Material cost2,
      int costAmount2,
      boolean isEnchanted
    ) {
      this.result = result;
      this.cost1 = cost1;
      this.costAmount1 = costAmount1;
      this.cost2 = cost2;
      this.costAmount2 = costAmount2;
      this.isEnchanted = isEnchanted;
    }
  }

  static {
    // Tier 1 trades (Available from start)
    List<TradeOffer> tier1 = Arrays.asList(
      new TradeOffer(Material.IRON_SWORD, Material.EMERALD, 5, null, 0, false),
      new TradeOffer(
        Material.IRON_CHESTPLATE,
        Material.EMERALD,
        8,
        null,
        0,
        false
      ),
      new TradeOffer(
        Material.BOW,
        Material.EMERALD,
        6,
        Material.STRING,
        3,
        false
      ),
      new TradeOffer(
        Material.ARROW,
        Material.EMERALD,
        1,
        Material.FLINT,
        2,
        false
      )
    );

    // Tier 2 trades (Wave 5+)
    List<TradeOffer> tier2 = Arrays.asList(
      new TradeOffer(
        Material.DIAMOND_SWORD,
        Material.EMERALD,
        15,
        Material.IRON_SWORD,
        1,
        false
      ),
      new TradeOffer(
        Material.DIAMOND_CHESTPLATE,
        Material.EMERALD,
        20,
        Material.IRON_CHESTPLATE,
        1,
        false
      ),
      new TradeOffer(
        Material.CROSSBOW,
        Material.EMERALD,
        12,
        Material.BOW,
        1,
        false
      ),
      new TradeOffer(
        Material.SHIELD,
        Material.EMERALD,
        8,
        Material.IRON_INGOT,
        5,
        false
      )
    );

    // Tier 3 trades (Wave 10+)
    List<TradeOffer> tier3 = Arrays.asList(
      new TradeOffer(
        Material.DIAMOND_SWORD,
        Material.EMERALD,
        25,
        Material.DIAMOND_SWORD,
        1,
        true
      ),
      new TradeOffer(
        Material.DIAMOND_CHESTPLATE,
        Material.EMERALD,
        30,
        Material.DIAMOND_CHESTPLATE,
        1,
        true
      ),
      new TradeOffer(Material.BOW, Material.EMERALD, 20, Material.BOW, 1, true),
      new TradeOffer(
        Material.GOLDEN_APPLE,
        Material.EMERALD_BLOCK,
        1,
        null,
        0,
        false
      )
    );

    TRADE_TIERS.put(1, tier1);
    TRADE_TIERS.put(5, tier2);
    TRADE_TIERS.put(10, tier3);
  }

  public TradeManager(
    Bastion plugin,
    VillageManager villageManager,
    WaveManager waveManager
  ) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.waveManager = waveManager;
    this.random = new Random();
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onTradeInventoryOpen(InventoryOpenEvent event) {
    return; // No custom trades if disabled
    /* 
    if (!(event.getInventory().getHolder() instanceof Villager)) {
      return;
    }

    Villager villager = (Villager) event.getInventory().getHolder();
    if (!villageManager.getRegisteredVillagers().contains(villager)) {
      return;
    }

    refreshVillagerTrades(villager);
    /* */
  }

  /**
   * Updates a villager's trades based on current wave progression
   */
  private void refreshVillagerTrades(Villager villager) {
    List<MerchantRecipe> recipes = new ArrayList<>();
    int currentWave = waveManager.getCurrentWave();

    // Add trades from all unlocked tiers
    for (Map.Entry<Integer, List<TradeOffer>> entry : TRADE_TIERS.entrySet()) {
      if (currentWave >= entry.getKey()) {
        for (TradeOffer offer : entry.getValue()) {
          recipes.add(createRecipe(offer));
        }
      }
    }

    // Add some random bonus trades based on wave number
    addBonusTrades(recipes, currentWave);

    villager.setRecipes(recipes);
  }

  /**
   * Creates a merchant recipe from a trade offer
   */
  private MerchantRecipe createRecipe(TradeOffer offer) {
    ItemStack result = new ItemStack(offer.result);

    // Add random enchantments if specified
    if (offer.isEnchanted) {
      addRandomEnchantments(result);
    }

    MerchantRecipe recipe = new MerchantRecipe(result, 0, 5, true);

    // Add required items
    recipe.addIngredient(new ItemStack(offer.cost1, offer.costAmount1));
    if (offer.cost2 != null) {
      recipe.addIngredient(new ItemStack(offer.cost2, offer.costAmount2));
    }

    return recipe;
  }

  /**
   * Adds random high-tier trades based on wave progression
   */
  private void addBonusTrades(List<MerchantRecipe> recipes, int currentWave) {
    int bonusCount = Math.min(currentWave / 5, 3); // Up to 3 bonus trades

    for (int i = 0; i < bonusCount; i++) {
      ItemStack result = generateBonusItem(currentWave);
      MerchantRecipe recipe = new MerchantRecipe(result, 0, 1, true);

      // Scale emerald cost with item value
      int emeraldCost = 10 + (currentWave * 2);
      recipe.addIngredient(new ItemStack(Material.EMERALD, emeraldCost));

      recipes.add(recipe);
    }
  }

  /**
   * Generates a random high-value item for bonus trades
   */
  private ItemStack generateBonusItem(int currentWave) {
    Material[] possibleItems = {
      Material.DIAMOND_SWORD,
      Material.DIAMOND_AXE,
      Material.DIAMOND_HELMET,
      Material.DIAMOND_CHESTPLATE,
      Material.DIAMOND_LEGGINGS,
      Material.DIAMOND_BOOTS,
    };

    ItemStack item = new ItemStack(
      possibleItems[random.nextInt(possibleItems.length)]
    );
    addRandomEnchantments(item);
    return item;
  }

  /**
   * Adds 1-3 random enchantments to an item
   */
  private void addRandomEnchantments(ItemStack item) {
    List<Enchantment> possibleEnchants = new ArrayList<>(
      Arrays.asList(Enchantment.values())
    );
    Collections.shuffle(possibleEnchants);

    int enchantCount = random.nextInt(2) + 1; // 1-3 enchantments
    int added = 0;

    for (Enchantment enchant : possibleEnchants) {
      if (added >= enchantCount) break;
      if (enchant.canEnchantItem(item)) {
        int level = random.nextInt(enchant.getMaxLevel()) + 1;
        item.addEnchantment(enchant, level);
        added++;
      }
    }
  }

  /**
   * Forces a refresh of all villager trades
   */
  public void refreshAllTrades() {
    for (Villager villager : villageManager.getRegisteredVillagers()) {
      refreshVillagerTrades(villager);
    }
  }
}
