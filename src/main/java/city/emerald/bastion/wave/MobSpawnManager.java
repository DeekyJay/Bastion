package city.emerald.bastion.wave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import city.emerald.bastion.BarrierManager;
import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.economy.LootManager;

public class MobSpawnManager implements Listener {

  private final Bastion plugin;
  private WaveManager waveManager;
  private final VillageManager villageManager;
  private final BarrierManager barrierManager;
  private final LootManager lootManager;
  private final Random random;
  private BukkitTask spawnTask;
  private Map<LivingEntity, Long> spawnTimes;
  private int currentMobCount;
  private Location previousSpawnLocation = null;

  public MobSpawnManager(
    Bastion plugin,
    VillageManager villageManager,
    BarrierManager barrierManager,
    LootManager lootManager
  ) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.barrierManager = barrierManager;
    this.lootManager = lootManager;
    this.random = new Random();
    this.spawnTimes = new HashMap<>();
    this.currentMobCount = 0;
  }

  public void setWaveManager(WaveManager waveManager) {
    this.waveManager = waveManager;
  }

  public void stopSpawning() {
    if (spawnTask != null) {
      spawnTask.cancel();
      spawnTask = null;
    }

    // Clean up existing mobs
    for (LivingEntity mob : spawnTimes.keySet()) {
      mob.remove();
    }
    spawnTimes.clear();
    currentMobCount = 0;
  }

  /**
   * Gets the real-time count of living (valid) mobs.
   * @return The number of currently living mobs
   */
  public int getLivingMobCount() {
    return getImmediateLivingMobCount();
  }

  /**
   * Gets an immediate count of living (valid) mobs by checking all tracked mobs right now.
   * Use this when you need the most current count without waiting for the timer update.
   * @return The number of currently living mobs (calculated immediately)
   */
  public int getImmediateLivingMobCount() {
    return (int) spawnTimes.keySet().stream()
      .filter(LivingEntity::isValid)
      .count();
  }

  private Location findSafeSpawnLocation() {
    Optional<Location> centerOpt = villageManager.getVillageCenter();
    if (!centerOpt.isPresent()) {
      return null;
    }

    Location center = centerOpt.get();
    World world = center.getWorld();
    int attempts = 0;
    int maxAttempts = 50;

    while (attempts++ < maxAttempts) {
      // Get random angle and distance from center
      double angle = random.nextDouble() * 2 * Math.PI;
      double distance = 35 + random.nextDouble() * 10; // Spawn between 35-45 blocks from center (within 50 block barrier)

      // Calculate position
      double x = center.getX() + (distance * Math.cos(angle));
      double z = center.getZ() + (distance * Math.sin(angle));

      Location spawnLoc = new Location(world, x, 0, z);
      int y = 1 + world.getHighestBlockYAt(spawnLoc);
      spawnLoc.setY(y);

      // Validate location
      if (isValidSpawnLocation(spawnLoc)) {
        return spawnLoc;
      }
    }

    return null;
  }

  private Location findSpawnLocationWithRetry() {
    for (int retry = 0; retry < 50; retry++) {
      Location loc = findSafeSpawnLocation();
      if (loc != null) {
        previousSpawnLocation = loc.clone();
        return loc;
      }
      
      if (previousSpawnLocation != null) {
        return previousSpawnLocation.clone();
      }
    }
    
    return null;
  }

  private boolean isValidSpawnLocation(Location loc) {
    if (
      !barrierManager.isInBarrier(loc, villageManager.getVillageCenter().get())
    ) {
      return false;
    }

    // Check if location is away from players and villagers
    double minDistance = 20.0;

    // Check distance from players
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.getLocation().distance(loc) < minDistance) {
        return false;
      }
    }

    // Check distance from villagers
    for (Villager villager : villageManager.getRegisteredVillagers()) {
      if (villager.getLocation().distance(loc) < minDistance) {
        return false;
      }
    }

    return true;
  }

  private void applyMobAttributes(LivingEntity mob) {
    // All stat scaling removed - mobs keep their base attributes
    // This method kept as stub for future use (equipment, effects, etc.)
  }

  private String formatMobName(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
  }

  /**
   * Calculate probability of selecting a mob with given difficulty
   * @param targetDifficulty The desired difficulty level
   * @param candidateDifficulty The difficulty of the candidate mob
   * @return Probability [0.0, 1.0] of selecting this mob
   */
  private double calculateSelectionProbability(double targetDifficulty, double candidateDifficulty) {
    double distance = Math.abs(candidateDifficulty - targetDifficulty);
    return Math.exp(-distance * 2.0);
  }

  /**
   * Generate initial mob distribution using probabilistic sampling
   * @param targetDifficulty The desired average difficulty
   * @param mobCount Number of mobs to generate
   * @param availableMobTypes List of available mob types
   * @param mobDifficultyMap Difficulty values for each mob type
   * @return Initial mob list
   */
  private List<EntityType> generateInitialDistribution(
      double targetDifficulty,
      int mobCount,
      List<EntityType> availableMobTypes,
      Map<EntityType, Double> mobDifficultyMap
  ) {
    List<EntityType> initialMobs = new ArrayList<>();
    
    plugin.getLogger().info("DEBUG: availableMobTypes size = " + availableMobTypes.size());
    
    while (initialMobs.size() < mobCount) {
      // Pick random mob
      EntityType candidateMob = availableMobTypes.get(random.nextInt(availableMobTypes.size()));
      
      // Calculate acceptance probability based on difficulty
      double mobDifficulty = mobDifficultyMap.getOrDefault(candidateMob, 1.0);
      double acceptanceProbability = calculateSelectionProbability(targetDifficulty, mobDifficulty);
      
      // Accept if random number is less than or equal to probability
      if (random.nextDouble() <= acceptanceProbability) {
        initialMobs.add(candidateMob);
      }
    }
    
    return initialMobs;
  }

  /**
   * Generates and spawns all mobs for a given wave at once.
   * @param waveNumber The current wave number.
   * @param mobCount The total number of mobs to spawn for the wave.
   */
  public void spawnWave(int waveNumber, int mobCount) {
    List<EntityType> mobList = generateMobListForWave(waveNumber, mobCount);
    int actualSpawned = 0;

    for (EntityType mobType : mobList) {
      Location spawnLoc = findSpawnLocationWithRetry();
      if (spawnLoc == null) {
        plugin.getLogger().warning("Could not find a safe spawn location for wave " + waveNumber);
        continue;
      }

      LivingEntity mob = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, mobType);

      // Log spawn location for debugging
      plugin.getLogger().info(String.format("Spawned %s at coordinates: X=%.2f, Y=%.2f, Z=%.2f", 
          mobType.name(), spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ()));

      // Equip mobs to prevent them from burning in daylight
      if (mob instanceof org.bukkit.entity.Zombie || mob instanceof org.bukkit.entity.Skeleton) {
          if (mob.getEquipment() != null) {
              mob.getEquipment().setHelmet(new ItemStack(Material.LEATHER_HELMET));
              mob.getEquipment().setHelmetDropChance(0.0f); // Prevent helmet drop
          }
      }

      // Apply wave-based attributes
      applyMobAttributes(mob);

      // Determine if mob is elite or boss
      boolean isElite = waveNumber >= 5 && random.nextDouble() < 0.2;
      boolean isBoss = waveNumber % 10 == 0;

      // Set custom name to indicate wave number and type
      String prefix = isBoss
        ? "§4[BOSS]"
        : (isElite ? "§5[ELITE]" : "§c[Wave " + waveNumber + "]");
      mob.setCustomName(prefix + " " + formatMobName(mob.getType().name()));
      mob.setCustomNameVisible(true);

      // Apply health modifications for elite and boss mobs
      if (isBoss) {
        // Double health for boss mobs
        double baseHealth = mob.getMaxHealth();
        mob.setMaxHealth(baseHealth * 2.0);
        mob.setHealth(baseHealth * 2.0);
      } else if (isElite) {
        // 50% more health for elite mobs
        double baseHealth = mob.getMaxHealth();
        mob.setMaxHealth(baseHealth * 1.5);
        mob.setHealth(baseHealth * 1.5);
      }

      // Track spawn time
      spawnTimes.put(mob, System.currentTimeMillis());
      currentMobCount++;
      actualSpawned++;
    }
    
    // Update WaveManager with actual spawned count
    waveManager.adjustRemainingMobs(actualSpawned);
    plugin.getLogger().info("Spawned " + actualSpawned + " of " + mobCount + " requested mobs for wave " + waveNumber);
  }

  /**
   * Generates a list of mobs for a wave based on a target difficulty score.
   * @param waveNumber The current wave number.
   * @param mobCount The total number of mobs in the wave.
   * @return A list of EntityTypes to be spawned.
   */
  private List<EntityType> generateMobListForWave(int waveNumber, int mobCount) {
    ConfigurationSection difficultyConfig = plugin.getConfig().getConfigurationSection("mob_difficulty");
    if (difficultyConfig == null || difficultyConfig.getKeys(false).isEmpty()) {
        plugin.getLogger().severe("mob_difficulty section is empty or missing from config.yml!");
        return Collections.nCopies(mobCount, EntityType.ZOMBIE);
    }

    Map<EntityType, Double> mobDifficultyMap = new HashMap<>();
    List<EntityType> availableMobTypes = new ArrayList<>();
    for (String key : difficultyConfig.getKeys(false)) {
        try {
            EntityType type = EntityType.valueOf(key.toUpperCase());
            mobDifficultyMap.put(type, difficultyConfig.getDouble(key));
            availableMobTypes.add(type);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid mob type in config.yml: " + key);
        }
    }

    double startingDifficulty = plugin.getDoubleSafe("wave.difficulty_scaling.starting_average_difficulty", 1.0);
    double difficultyIncrease = plugin.getDoubleSafe("wave.difficulty_scaling.average_difficulty_increase_percent", 5.0) / 100.0;
    int maxFailedSwaps = plugin.getIntSafe("wave.difficulty_scaling.max_failed_swaps", 50);

    double targetAverageDifficulty = startingDifficulty * Math.pow(1 + difficultyIncrease, waveNumber - 1);

    // Generate initial distribution using probabilistic sampling
    List<EntityType> mobSet = generateInitialDistribution(targetAverageDifficulty, mobCount, availableMobTypes, mobDifficultyMap);
    
    // Calculate initial average difficulty
    double currentAverageDifficulty = mobSet.stream()
        .mapToDouble(mob -> mobDifficultyMap.getOrDefault(mob, 1.0))
        .average()
        .orElse(1.0);

    // Simulated annealing optimization
    int failedSwaps = 0;
    double currentDistance = Math.abs(currentAverageDifficulty - targetAverageDifficulty);
    
    while (failedSwaps < maxFailedSwaps) {
        if (availableMobTypes.isEmpty()) break;

        int indexToSwap = random.nextInt(mobSet.size());
        EntityType candidateType = availableMobTypes.get(random.nextInt(availableMobTypes.size()));

        double difficultyRemoved = mobDifficultyMap.getOrDefault(mobSet.get(indexToSwap), 1.0);
        double difficultyAdded = mobDifficultyMap.getOrDefault(candidateType, 1.0);

        double newTotalDifficulty = (currentAverageDifficulty * mobCount) - difficultyRemoved + difficultyAdded;
        double newAverageDifficulty = newTotalDifficulty / mobCount;
        double newDistance = Math.abs(newAverageDifficulty - targetAverageDifficulty);

        // Accept if new distance is not worse than current distance
        if (newDistance <= currentDistance) {
            mobSet.set(indexToSwap, candidateType);
            currentAverageDifficulty = newAverageDifficulty;
            currentDistance = newDistance;
            failedSwaps = 0; // Reset on successful swap
        } else {
            failedSwaps++;
        }
    }
    
    plugin.getLogger().info("Generated mob set for wave " + waveNumber + " with average difficulty: " + currentAverageDifficulty);
    return mobSet;
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    // Track mob deaths for cleanup purposes
    if (event.getEntity() instanceof org.bukkit.entity.Monster) {
      spawnTimes.remove(event.getEntity());
      if (currentMobCount > 0) {
        currentMobCount--;
      }
    }
  }

  /**
   * Instantly cleanup all remaining hostile mobs without drops when wave completion target is reached
   */
  public void cleanupRemainingMobs() {
    plugin.getLogger().info("Instantly cleaning up " + spawnTimes.size() + " remaining mobs");

    // Remove all tracked hostile mobs instantly without drops
    for (LivingEntity mob : spawnTimes.keySet()) {
      if (mob.isValid()) {
        // Clear drops to prevent item spam
        mob.getWorld().getEntitiesByClass(org.bukkit.entity.Monster.class).forEach(monster -> {
          if (monster.equals(mob)) {
            monster.remove();
          }
        });
      }
    }

    // Clear all tracking
    spawnTimes.clear();
    currentMobCount = 0;

    plugin.getLogger().info("Mob cleanup completed");
  }

  /**
   * Kill all currently spawned mobs to generate death logs for debugging
   * @return The number of mobs killed
   */
  public int killAllSpawnedMobs() {
    int killedCount = 0;
    
    // Copy keys to avoid ConcurrentModificationException
    List<LivingEntity> mobsToKill = new ArrayList<>(spawnTimes.keySet());
    
    for (LivingEntity mob : mobsToKill) {
      if (mob.isValid()) {
        mob.setHealth(0); // Kill the mob to trigger death event
        killedCount++;
      }
    }
    
    return killedCount;
  }
}
