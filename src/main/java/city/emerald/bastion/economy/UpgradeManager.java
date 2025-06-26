package city.emerald.bastion.economy;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;

public class UpgradeManager {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private final Map<String, Map<String, Integer>> playerUpgrades;
  private final Map<String, Map<String, Integer>> villageUpgrades;

  // Define upgrade types and their costs
  public enum UpgradeType {
    // Village defense upgrades
    VILLAGER_HEALTH("Villager Health", 10, 5),
    BARRIER_STRENGTH("Barrier Strength", 15, 3),
    VILLAGER_REGEN("Villager Regeneration", 20, 3),

    // Combat upgrades
    DAMAGE_BOOST("Damage Boost", 8, 5),
    PROTECTION("Protection", 12, 5),
    SPEED_BOOST("Speed Boost", 10, 3);

    private final String name;
    private final int baseCost;
    private final int maxLevel;

    UpgradeType(String name, int baseCost, int maxLevel) {
      this.name = name;
      this.baseCost = baseCost;
      this.maxLevel = maxLevel;
    }

    public String getName() {
      return name;
    }

    public int getBaseCost() {
      return baseCost;
    }

    public int getMaxLevel() {
      return maxLevel;
    }
  }

  public UpgradeManager(Bastion plugin, VillageManager villageManager) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.playerUpgrades = new HashMap<>();
    this.villageUpgrades = new HashMap<>();
  }

  /**
   * Attempts to purchase a player upgrade
   * @return true if successful
   */
  public boolean purchasePlayerUpgrade(Player player, UpgradeType upgrade) {
    String playerId = player.getUniqueId().toString();
    Map<String, Integer> upgrades = playerUpgrades.computeIfAbsent(
      playerId,
      k -> new HashMap<>()
    );

    int currentLevel = upgrades.getOrDefault(upgrade.name(), 0);
    if (currentLevel >= upgrade.maxLevel) {
      player.sendMessage("§cThis upgrade is already at maximum level!");
      return false;
    }

    int cost = calculateUpgradeCost(upgrade, currentLevel);
    if (!hasEnoughEmeralds(player, cost)) {
      player.sendMessage("§cYou need " + cost + " emeralds for this upgrade!");
      return false;
    }

    removeEmeralds(player, cost);
    upgrades.put(upgrade.name(), currentLevel + 1);
    applyPlayerUpgrade(player, upgrade, currentLevel + 1);

    player.sendMessage(
      "§aUpgraded " + upgrade.name() + " to level " + (currentLevel + 1)
    );
    return true;
  }

  /**
   * Attempts to purchase a village upgrade
   * @return true if successful
   */
  public boolean purchaseVillageUpgrade(Player player, UpgradeType upgrade) {
    String villageId = villageManager
      .getVillageCenter()
      .get()
      .getWorld()
      .getUID()
      .toString();
    Map<String, Integer> upgrades = villageUpgrades.computeIfAbsent(
      villageId,
      k -> new HashMap<>()
    );

    int currentLevel = upgrades.getOrDefault(upgrade.name(), 0);
    if (currentLevel >= upgrade.maxLevel) {
      player.sendMessage("§cThis village upgrade is already at maximum level!");
      return false;
    }

    int cost = calculateUpgradeCost(upgrade, currentLevel);
    if (!hasEnoughEmeralds(player, cost)) {
      player.sendMessage("§cYou need " + cost + " emeralds for this upgrade!");
      return false;
    }

    removeEmeralds(player, cost);
    upgrades.put(upgrade.name(), currentLevel + 1);
    applyVillageUpgrade(upgrade, currentLevel + 1);

    player.sendMessage(
      "§aUpgraded village " + upgrade.name() + " to level " + (currentLevel + 1)
    );
    return true;
  }

  /**
   * Calculates the cost of the next upgrade level
   */
  private int calculateUpgradeCost(UpgradeType upgrade, int currentLevel) {
    return upgrade.baseCost * (currentLevel + 1);
  }

  /**
   * Checks if a player has enough emeralds
   */
  private boolean hasEnoughEmeralds(Player player, int amount) {
    int emeralds = 0;
    for (ItemStack item : player.getInventory().getContents()) {
      if (item != null && item.getType() == Material.EMERALD) {
        emeralds += item.getAmount();
      }
      if (item != null && item.getType() == Material.EMERALD_BLOCK) {
        emeralds += item.getAmount() * 9;
      }
    }
    return emeralds >= amount;
  }

  /**
   * Removes emeralds from a player's inventory
   */
  private void removeEmeralds(Player player, int amount) {
    int remaining = amount;

    // First use emerald blocks if available
    if (remaining >= 9) {
      ItemStack[] contents = player.getInventory().getContents();
      for (int i = 0; i < contents.length && remaining >= 9; i++) {
        ItemStack item = contents[i];
        if (item != null && item.getType() == Material.EMERALD_BLOCK) {
          int blocksNeeded = remaining / 9;
          int blocksAvailable = item.getAmount();
          int blocksToRemove = Math.min(blocksNeeded, blocksAvailable);

          remaining -= blocksToRemove * 9;
          item.setAmount(blocksAvailable - blocksToRemove);
        }
      }
    }

    // Then use individual emeralds
    if (remaining > 0) {
      ItemStack[] contents = player.getInventory().getContents();
      for (int i = 0; i < contents.length && remaining > 0; i++) {
        ItemStack item = contents[i];
        if (item != null && item.getType() == Material.EMERALD) {
          int toRemove = Math.min(remaining, item.getAmount());
          remaining -= toRemove;
          item.setAmount(item.getAmount() - toRemove);
        }
      }
    }
  }

  /**
   * Applies purchased upgrade effects to a player
   */
  private void applyPlayerUpgrade(
    Player player,
    UpgradeType upgrade,
    int level
  ) {
    switch (upgrade) {
      case DAMAGE_BOOST:
        // Applied in combat events
        break;
      case PROTECTION:
        // Applied in damage events
        break;
      case SPEED_BOOST:
        player.setWalkSpeed(0.2f + (level * 0.04f));
        break;
    }
  }

  /**
   * Applies purchased upgrade effects to the village
   */
  private void applyVillageUpgrade(UpgradeType upgrade, int level) {
    switch (upgrade) {
      case VILLAGER_HEALTH:
        for (Villager villager : villageManager.getRegisteredVillagers()) {
          villager.setMaxHealth(20 + (level * 5));
          villager.setHealth(villager.getMaxHealth());
        }
        break;
      case BARRIER_STRENGTH:
        // Applied in damage events
        break;
      case VILLAGER_REGEN:
        // Applied in healing events
        break;
    }
  }

  /**
   * Gets the current level of a player upgrade
   */
  public int getPlayerUpgradeLevel(Player player, UpgradeType upgrade) {
    return playerUpgrades
      .getOrDefault(player.getUniqueId().toString(), new HashMap<>())
      .getOrDefault(upgrade.name(), 0);
  }

  /**
   * Gets the current level of a village upgrade
   */
  public int getVillageUpgradeLevel(UpgradeType upgrade) {
    if (!villageManager.getVillageCenter().isPresent()) {
      return 0;
    }
    String villageId = villageManager
      .getVillageCenter()
      .get()
      .getWorld()
      .getUID()
      .toString();
    return villageUpgrades
      .getOrDefault(villageId, new HashMap<>())
      .getOrDefault(upgrade.name(), 0);
  }

  /**
   * Gets all available upgrades and their current levels for a player
   */
  public Map<UpgradeType, Integer> getPlayerUpgrades(Player player) {
    Map<UpgradeType, Integer> result = new HashMap<>();
    Map<String, Integer> upgrades = playerUpgrades.get(
      player.getUniqueId().toString()
    );

    if (upgrades != null) {
      for (UpgradeType upgrade : UpgradeType.values()) {
        result.put(upgrade, upgrades.getOrDefault(upgrade.name(), 0));
      }
    }

    return result;
  }

  /**
   * Gets all available village upgrades and their current levels
   */
  public Map<UpgradeType, Integer> getVillageUpgrades() {
    Map<UpgradeType, Integer> result = new HashMap<>();
    if (!villageManager.getVillageCenter().isPresent()) {
      return result;
    }

    String villageId = villageManager
      .getVillageCenter()
      .get()
      .getWorld()
      .getUID()
      .toString();
    Map<String, Integer> upgrades = villageUpgrades.get(villageId);

    if (upgrades != null) {
      for (UpgradeType upgrade : UpgradeType.values()) {
        result.put(upgrade, upgrades.getOrDefault(upgrade.name(), 0));
      }
    }

    return result;
  }

  /**
   * Reset all upgrades (called when game ends)
   */
  public void reset() {
    playerUpgrades.clear();
    villageUpgrades.clear();
  }
}
