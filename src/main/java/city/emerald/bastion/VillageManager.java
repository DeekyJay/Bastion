package city.emerald.bastion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import city.emerald.bastion.economy.UpgradeManager;

public class VillageManager {

  private final Bastion plugin;
  private UpgradeManager upgradeManager;
  private BarrierManager barrierManager;
  private Location villageCenter;
  private List<Villager> registeredVillagers;
  private boolean isProtected;

  public VillageManager(Bastion plugin) {
    this.plugin = plugin;
    this.registeredVillagers = new ArrayList<>();
    this.isProtected = false;
  }

  public void setUpgradeManager(UpgradeManager upgradeManager) {
    this.upgradeManager = upgradeManager;
  }

  public void setBarrierManager(BarrierManager barrierManager) {
    this.barrierManager = barrierManager;
  }

  /**
   * Finds and selects a valid village for the game.
   * @param world The world to search in
   * @return true if a valid village was found and selected
   */
  public boolean findAndSelectVillage(World world) {
    plugin
      .getLogger()
      .info("Searching for the nearest village structure with at least 4 villagers...");

    // Use locateNearestStructure to find a village even in unloaded chunks
    Location nearestVillage = world.locateNearestStructure(
      world.getSpawnLocation(),
      StructureType.VILLAGE,
      5000, // Search within a 5000 block radius
      true // Find in unloaded chunks
    );

    if (nearestVillage != null) {
      plugin
        .getLogger()
        .info("Found a village structure at: " + nearestVillage.toVector());

      // We found a village structure, now validate it has enough villagers
      if (isValidVillageLocation(nearestVillage) &&
        hasEnoughVillagers(nearestVillage, world)) {
        Location spawnLoc = findSafeLocation(nearestVillage);
        this.villageCenter = spawnLoc;
        world.setSpawnLocation(spawnLoc);

        // We need to load the chunk to register villagers
        spawnLoc.getChunk().load();
        plugin
          .getServer()
          .getScheduler()
          .runTaskLater(
            plugin,
            () -> {
              registerVillagersInRange(world);
              plugin
                .getLogger()
                .info(
                  "Village selected and spawn set. " +
                  registeredVillagers.size() +
                  " villagers registered."
                );
            },
            20L
          ); // Delay to allow chunk to fully load

        return true;
      } else {
        plugin
          .getLogger()
          .warning(
            "Found a village structure, but it's not suitable (insufficient solid ground or fewer than 4 villagers)."
          );
        return false;
      }
    } else {
      plugin
        .getLogger()
        .warning(
          "No village structure found within a 5000 block radius of the world spawn."
        );
      return false;
    }
  }

  /**
   * Checks if a location is valid for a village center.
   * @param location The location to check
   * @return true if the location is valid
   */
  private boolean isValidVillageLocation(Location location) {
    int radius = getBarrierRadius();
    World world = location.getWorld();

    // Check if area has enough solid ground
    int groundBlocks = 0;
    int requiredGroundBlocks = (radius * 2) * (radius * 2) / 4; // 25% of area should be valid ground

    for (int x = -radius; x <= radius; x += 10) {
      for (int z = -radius; z <= radius; z += 10) {
        Location check = location.clone().add(x, 0, z);
        check.setY(world.getHighestBlockYAt(check));
        if (check.getBlock().getType().isSolid()) {
          groundBlocks += 100; // Each sample represents 10x10 blocks
        }
      }
    }

    return groundBlocks >= requiredGroundBlocks;
  }

  /**
   * Checks if a village location has at least 4 villagers within the barrier range.
   * @param villageLocation The village center location
   * @param world The world to search in
   * @return true if the village has at least 4 villagers
   */
  private boolean hasEnoughVillagers(Location villageLocation, World world) {
    int radius = getBarrierRadius();
    int villagerCount = 0;

    // Load the chunk to ensure villagers are loaded
    villageLocation.getChunk().load();

    // Count villagers within the barrier radius
    for (Entity entity : world.getEntities()) {
      if (entity.getType() == EntityType.VILLAGER) {
        if (isInRange(entity.getLocation(), villageLocation, radius)) {
          villagerCount++;
        }
      }
    }

    plugin
      .getLogger()
      .info(
        "Found " +
        villagerCount +
        " villagers in village area (minimum required: 4)"
      );
    return villagerCount >= 4;
  }

  /**
   * Registers all villagers within the barrier range.
   * @param world The world to search in
   */
  private void registerVillagersInRange(World world) {
    registeredVillagers.clear();
    // Get barrier radius from config, fallback to 80 if barrier manager not set
    int radius = barrierManager != null
      ? barrierManager.getBarrierRadius()
      : plugin.getConfig().getInt("village.barrier.radius", 80);

    for (Entity entity : world.getEntities()) {
      if (entity.getType() == EntityType.VILLAGER) {
        if (isInRange(entity.getLocation(), villageCenter, radius)) {
          Villager villager = (Villager) entity;
          registeredVillagers.add(villager);

          // Apply health upgrade if available
          int healthLevel = upgradeManager.getVillageUpgradeLevel(
            UpgradeManager.UpgradeType.VILLAGER_HEALTH
          );
          if (healthLevel > 0) {
            villager.setMaxHealth(20 + (healthLevel * 5));
            villager.setHealth(villager.getMaxHealth());
          }
        }
      }
    }
  }

  /**
   * Checks if a location is within range of the center.
   */
  private boolean isInRange(Location loc1, Location loc2, int radius) {
    return loc1.distanceSquared(loc2) <= radius * radius;
  }

  /**
   * Gets the village center location.
   * @return Optional containing the village center location if set
   */
  public Optional<Location> getVillageCenter() {
    return Optional.ofNullable(villageCenter);
  }

  /**
   * Gets all registered villagers in the protected area.
   * @return List of registered villagers
   */
  public List<Villager> getRegisteredVillagers() {
    return new ArrayList<>(registeredVillagers);
  }

  /**
   * Sets the protection state of the village.
   * @param protected_ true to enable protection, false to disable
   */
  public void setProtected(boolean protected_) {
    this.isProtected = protected_;
  }

  /**
   * Checks if the village is currently protected.
   * @return true if the village is protected
   */
  public boolean isProtected() {
    return isProtected;
  }

  /**
   * Cleans up the village manager.
   */
  public void cleanup() {
    // Reset villager health before clearing
    for (Villager villager : registeredVillagers) {
      villager.setMaxHealth(20);
      villager.setHealth(20);
    }
    registeredVillagers.clear();
    villageCenter = null;
    isProtected = false;
  }

  /**
   * Heal villagers based on regeneration upgrade
   */
  public void applyVillagerRegeneration() {
    int regenLevel = upgradeManager.getVillageUpgradeLevel(
      UpgradeManager.UpgradeType.VILLAGER_REGEN
    );
    if (regenLevel > 0) {
      double healAmount = regenLevel * 0.5; // 0.5 hearts per level
      for (Villager villager : registeredVillagers) {
        double newHealth = Math.min(
          villager.getMaxHealth(),
          villager.getHealth() + healAmount
        );
        villager.setHealth(newHealth);
      }
    }
  }

  /**
   * Apply damage reduction from barrier upgrade
   * @return The amount of damage to reduce
   */
  public double getBarrierDamageReduction() {
    if (this.upgradeManager == null) {
      return 0.0; // Default no reduction if upgrade manager not initialized
    }
    int barrierLevel = upgradeManager.getVillageUpgradeLevel(
      UpgradeManager.UpgradeType.BARRIER_STRENGTH
    );
    return barrierLevel * 0.1; // 10% damage reduction per level
  }

  /**
   * Finds a safe location near the specified center point.
   * @param center The center location
   * @return A safe location for teleportation
   */
  private Location findSafeLocation(Location center) {
    World world = center.getWorld();
    Location safe = center.clone();

    // Find highest solid block
    safe.setY(world.getHighestBlockYAt(safe));
    // Move up one block to ensure player stands on surface
    safe.add(0, 1, 0);

    return safe;
  }

  /**
   * Retrieves the barrier radius from the BarrierManager or config.
   * @return the barrier radius
   */
  private int getBarrierRadius() {
    return barrierManager != null
      ? barrierManager.getBarrierRadius()
      : plugin.getConfig().getInt("village.barrier.radius", 80);
  }
}
