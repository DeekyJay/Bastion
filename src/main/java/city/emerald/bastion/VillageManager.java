package city.emerald.bastion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
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
   * Finds the nearest village structure from a given search location.
   * @param world The world to search in
   * @param searchCenter The location to search from
   * @return Location of the village structure, or null if none found
   */
  public Location findVillage(World world, Location searchCenter) {
    plugin
      .getLogger()
      .info("Searching for the nearest village structure from location: " + searchCenter.toVector());

    // Use locateNearestStructure to find a village even in unloaded chunks
    Location nearestVillage = world.locateNearestStructure(
      searchCenter,
      StructureType.VILLAGE,
      5000, // Search within a 5000 block radius
      true // Find in unloaded chunks
    );

    if (nearestVillage != null) {
      plugin
        .getLogger()
        .info("Found a village structure at: " + nearestVillage.toVector());
    } else {
      plugin
        .getLogger()
        .warning(
          "No village structure found within a 5000 block radius of the search location."
        );
    }

    return nearestVillage;
  }

  /**
   * Selects a village at the given location as the game village.
   * @param villageLocation The location of the village structure
   * @return true if the village was successfully selected
   */
  public boolean selectVillage(Location villageLocation) {
    if (villageLocation == null) {
      plugin.getLogger().warning("Cannot select village: location is null");
      return false;
    }

    World world = villageLocation.getWorld();
    if (world == null) {
      plugin.getLogger().warning("Cannot select village: world is null");
      return false;
    }

    // Find a safe location for the village center
    Location spawnLoc = findSafeLocation(villageLocation);
    this.villageCenter = spawnLoc;
    world.setSpawnLocation(spawnLoc);
    
    // Load the chunk to ensure villagers can be registered
    spawnLoc.getChunk().load();
    
    plugin
      .getLogger()
      .info("Village selected and spawn set at: " + spawnLoc.toVector());

    return true;
  }

  /**
   * Finds and selects a valid village for the game.
   * @param world The world to search in
   * @return true if a valid village was found and selected
   * @deprecated Use {@link #findVillage(World, Location)} and {@link #selectVillage(Location)} instead
   */
  @Deprecated
  public boolean findAndSelectVillage(World world) {
    Location villageLocation = findVillage(world, world.getSpawnLocation());
    return villageLocation != null && selectVillage(villageLocation);
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
    int requiredVillagers = 4;

    plugin.getLogger().info("DEBUG: Searching for villagers in radius: " + radius);
    plugin.getLogger().info("DEBUG: Village location: " + villageLocation);
    
    // Load just the center chunk and a few nearby chunks
    int centerChunkX = villageLocation.getBlockX() >> 4;
    int centerChunkZ = villageLocation.getBlockZ() >> 4;
    
    // Load a 3x3 area of chunks around the village
    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        world.getChunkAt(centerChunkX + x, centerChunkZ + z).load();
      }
    }
    
    plugin.getLogger().info("DEBUG: Loaded 3x3 chunk area");
    
    // Use getEntitiesByClass to find villagers more reliably
    Collection<Villager> villagers = world.getEntitiesByClass(Villager.class);
    
    plugin.getLogger().info("DEBUG: Found " + villagers.size() + " total villagers in world");
    
    // Count villagers within radius
    for (Villager villager : villagers) {
      double distance = villager.getLocation().distance(villageLocation);
      if (distance <= radius) {
        villagerCount++;
        plugin.getLogger().info("DEBUG: Villager at " + villager.getLocation() + " is " + distance + " blocks away");
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
  public void registerVillagersInRange(World world) {
    registeredVillagers.clear();
    // Get barrier radius from config, fallback to 80 if barrier manager not set
    int radius = barrierManager != null
      ? barrierManager.getBarrierRadius()
      : plugin.getIntSafe("village.barrier.radius", 80);

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
    
    // Search in expanding spiral pattern until safe location found
    int maxRadius = 50; // Maximum search radius
    
    for (int radius = 0; radius <= maxRadius; radius++) {
        // Check center point first (radius 0)
        if (radius == 0) {
            if (isSafeLocation(world, safe)) {
                return safe;
            }
            continue;
        }
        
        // Search in spiral pattern around center
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                // Only check perimeter of current radius to avoid rechecking inner areas
                if (Math.abs(x) != radius && Math.abs(z) != radius) {
                    continue;
                }
                
                Location candidate = center.clone().add(x, 0, z);
                if (isSafeLocation(world, candidate)) {
                    return candidate;
                }
            }
        }
    }
    
    // Fallback: return original location with basic safety (better than nothing)
    plugin.getLogger().warning("Could not find safe location within " + maxRadius + " blocks, using fallback");
    safe.setY(world.getHighestBlockYAt(safe) + 1);
    return safe;
  }

  /**
   * Checks if a location is safe for player teleportation.
   * @param world The world
   * @param location The location to check (Y coordinate will be adjusted)
   * @return true if the location is safe
   */
  private boolean isSafeLocation(World world, Location location) {
    // Find the highest solid block at this X,Z coordinate
    int highestY = world.getHighestBlockYAt(location);
    
    // Check if we're at build limit
    if (highestY >= world.getMaxHeight() - 2) {
        return false; // Not enough space above
    }
    
    Location groundLevel = location.clone();
    groundLevel.setY(highestY);
    
    // Check the ground block - must be solid and safe
    Material groundMaterial = groundLevel.getBlock().getType();
    if (!groundMaterial.isSolid() || isDangerousMaterial(groundMaterial)) {
        return false;
    }
    
    // Check the two blocks above ground (where player head and body will be)
    Location airBlock1 = groundLevel.clone().add(0, 1, 0);
    Location airBlock2 = groundLevel.clone().add(0, 2, 0);
    
    Material air1Material = airBlock1.getBlock().getType();
    Material air2Material = airBlock2.getBlock().getType();
    
    // Both blocks must be passable (air, water, tall grass, etc.) and not dangerous
    if (!isPassableMaterial(air1Material) || isDangerousMaterial(air1Material) ||
        !isPassableMaterial(air2Material) || isDangerousMaterial(air2Material)) {
        return false;
    }
    
    // Update the location to be one block above the safe ground
    location.setY(highestY + 1);
    return true;
  }

  /**
   * Checks if a material is dangerous to players.
   * @param material The material to check
   * @return true if the material is dangerous
   */
  private boolean isDangerousMaterial(Material material) {
    switch (material) {
        case LAVA:
        case FIRE:
        case SOUL_FIRE:
        case MAGMA_BLOCK:
        case CAMPFIRE:
        case SOUL_CAMPFIRE:
        case SWEET_BERRY_BUSH:
        case CACTUS:
        case WITHER_ROSE:
        case POWDER_SNOW:
            return true;
        default:
            return false;
    }
  }

  /**
   * Checks if a material is passable (safe to stand in).
   * @param material The material to check
   * @return true if the material is passable
   */
  private boolean isPassableMaterial(Material material) {
    // Air and other non-solid blocks that are safe to stand in
    return material.isAir() || 
           material == Material.WATER ||
           material == Material.TALL_GRASS ||
           material == Material.SHORT_GRASS ||
           material == Material.FERN ||
           material == Material.LARGE_FERN ||
           material == Material.DEAD_BUSH ||
           material == Material.SEAGRASS ||
           material == Material.TALL_SEAGRASS ||
           material == Material.KELP ||
           material == Material.KELP_PLANT ||
           !material.isSolid(); // General fallback for non-solid blocks
  }

  /**
   * Retrieves the barrier radius from the BarrierManager or config.
   * @return the barrier radius
   */
  private int getBarrierRadius() {
    return barrierManager != null
      ? barrierManager.getBarrierRadius()
      : plugin.getIntSafe("village.barrier.radius", 80);
  }
}
