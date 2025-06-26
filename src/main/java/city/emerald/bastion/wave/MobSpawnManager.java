package city.emerald.bastion.wave;

import city.emerald.bastion.BarrierManager;
import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.economy.LootManager;
import java.util.*;
import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MobSpawnManager {

  private final Bastion plugin;
  private final WaveManager waveManager;
  private final VillageManager villageManager;
  private final BarrierManager barrierManager;
  private final LootManager lootManager;
  private final Random random;
  private BukkitTask spawnTask;
  private Map<LivingEntity, Long> spawnTimes;
  private int currentMobCount;
  private static final long MOB_LIFETIME = 5 * 60 * 20; // 5 minutes in ticks
  private static final int SPAWN_INTERVAL = 40; // 2 seconds between spawns
  private static final Map<Integer, List<EntityType>> WAVE_MOB_TYPES = new HashMap<>();

  static {
    // Basic waves (1-4)
    List<EntityType> basicMobs = Arrays.asList(
      EntityType.ZOMBIE,
      EntityType.SKELETON
    );

    // Medium waves (5-9)
    List<EntityType> mediumMobs = Arrays.asList(
      EntityType.ZOMBIE,
      EntityType.SKELETON,
      EntityType.SPIDER,
      EntityType.CREEPER
    );

    // Elite waves (10+)
    List<EntityType> eliteMobs = Arrays.asList(
      EntityType.ZOMBIE,
      EntityType.SKELETON,
      EntityType.SPIDER,
      EntityType.CREEPER,
      EntityType.WITCH
    );

    WAVE_MOB_TYPES.put(1, basicMobs);
    WAVE_MOB_TYPES.put(5, mediumMobs);
    WAVE_MOB_TYPES.put(10, eliteMobs);
  }

  public MobSpawnManager(
    Bastion plugin,
    WaveManager waveManager,
    VillageManager villageManager,
    BarrierManager barrierManager,
    LootManager lootManager
  ) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.villageManager = villageManager;
    this.barrierManager = barrierManager;
    this.lootManager = lootManager;
    this.random = new Random();
    this.spawnTimes = new HashMap<>();
    this.currentMobCount = 0;
  }

  public void startSpawning() {
    if (spawnTask != null) {
      spawnTask.cancel();
    }

    // Task to spawn mobs
    spawnTask =
      new BukkitRunnable() {
        @Override
        public void run() {
          if (!waveManager.isWaveActive()) {
            this.cancel();
            return;
          }

          // Check and remove expired mobs
          Iterator<Map.Entry<LivingEntity, Long>> it = spawnTimes
            .entrySet()
            .iterator();
          while (it.hasNext()) {
            Map.Entry<LivingEntity, Long> entry = it.next();
            LivingEntity mob = entry.getKey();
            long spawnTime = entry.getValue();

            if (
              !mob.isValid() ||
              (System.currentTimeMillis() - spawnTime) >= (MOB_LIFETIME * 50)
            ) { // Convert ticks to ms
              mob.remove();
              it.remove();
              currentMobCount--;
            }
          }

          // Try to spawn new mob if below wave limit
          if (currentMobCount < waveManager.getRemainingMobs()) {
            trySpawnMob();
          }
        }
      }
        .runTaskTimer(plugin, 0L, SPAWN_INTERVAL);
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

  private void trySpawnMob() {
    if (waveManager.getRemainingMobs() <= 0) {
      return;
    }

    Location spawnLoc = findSafeSpawnLocation();
    if (spawnLoc == null) {
      return;
    }

    EntityType mobType = selectMobType();
    LivingEntity mob = (LivingEntity) spawnLoc
      .getWorld()
      .spawnEntity(spawnLoc, mobType);

    // Make zombies immune to sunlight
    if (mob instanceof org.bukkit.entity.Zombie) {
      ((org.bukkit.entity.Zombie) mob).setShouldBurnInDay(false);
    }

    // Apply wave-based attributes
    applyMobAttributes(mob);

    // Determine if mob is elite or boss
    int currentWave = waveManager.getCurrentWave();
    boolean isElite = currentWave >= 5 && random.nextDouble() < 0.2;
    boolean isBoss = currentWave % 10 == 0;

    // Set custom name to indicate wave number and type
    String prefix = isBoss
      ? "§4[BOSS]"
      : (isElite ? "§5[ELITE]" : "§c[Wave " + currentWave + "]");
    mob.setCustomName(prefix + " " + formatMobName(mob.getType().name()));
    mob.setCustomNameVisible(true);

    // Track spawn time
    spawnTimes.put(mob, System.currentTimeMillis());
    currentMobCount++;
  }

  private EntityType selectMobType() {
    int currentWave = waveManager.getCurrentWave();
    List<EntityType> availableMobs = null;

    // Find the highest wave tier that's less than or equal to current wave
    for (int waveTier : new TreeSet<>(WAVE_MOB_TYPES.keySet())
      .descendingSet()) {
      if (currentWave >= waveTier) {
        availableMobs = WAVE_MOB_TYPES.get(waveTier);
        break;
      }
    }

    if (availableMobs == null || availableMobs.isEmpty()) {
      return EntityType.ZOMBIE; // Fallback
    }

    return availableMobs.get(random.nextInt(availableMobs.size()));
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
      int y = world.getHighestBlockYAt(spawnLoc);
      spawnLoc.setY(y);

      // Validate location
      if (isValidSpawnLocation(spawnLoc)) {
        return spawnLoc;
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
    int playerCount = Bukkit.getOnlinePlayers().size();
    double healthMultiplier = waveManager.getHealthMultiplier(playerCount);

    // Set health
    double baseHealth = mob
      .getAttribute(
        org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH")
      )
      .getBaseValue();
    mob
      .getAttribute(
        org.bukkit.attribute.Attribute.valueOf("GENERIC_MAX_HEALTH")
      )
      .setBaseValue(baseHealth * healthMultiplier);
    mob.setHealth(baseHealth * healthMultiplier);

    // Set damage
    if (
      mob.getAttribute(
        org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE")
      ) !=
      null
    ) {
      double baseDamage = mob
        .getAttribute(
          org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE")
        )
        .getBaseValue();
      mob
        .getAttribute(
          org.bukkit.attribute.Attribute.valueOf("GENERIC_ATTACK_DAMAGE")
        )
        .setBaseValue(baseDamage * waveManager.getDifficultyMultiplier());
    }

    // Increase speed slightly each wave
    if (
      mob.getAttribute(
        org.bukkit.attribute.Attribute.valueOf("GENERIC_MOVEMENT_SPEED")
      ) !=
      null
    ) {
      double baseSpeed = mob
        .getAttribute(
          org.bukkit.attribute.Attribute.valueOf("GENERIC_MOVEMENT_SPEED")
        )
        .getBaseValue();
      mob
        .getAttribute(
          org.bukkit.attribute.Attribute.valueOf("GENERIC_MOVEMENT_SPEED")
        )
        .setBaseValue(baseSpeed * (1 + 0.05 * waveManager.getCurrentWave()));
    }
  }

  private long calculateSpawnDelay() {
    // Start at 100 ticks (5 seconds), decrease with wave number but never below 20 ticks
    return Math.max(20L, 100L - (waveManager.getCurrentWave() * 5L));
  }

  private String formatMobName(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
  }
}
