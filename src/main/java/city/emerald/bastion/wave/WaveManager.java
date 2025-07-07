package city.emerald.bastion.wave;

import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.game.GameStateManager;

public class WaveManager {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private final LightningManager lightningManager;
  private final GameStateManager gameStateManager;
  private MobSpawnManager mobSpawnManager;
  private WaveState waveState;
  private int currentWave;
  private int remainingMobs;
  private int killCount;
  private double difficultyMultiplier;

  public enum WaveState {
    INACTIVE,
    PREPARING,
    ACTIVE,
    COMPLETED,
  }

  public WaveManager(
    Bastion plugin,
    VillageManager villageManager,
    LightningManager lightningManager,
    GameStateManager gameStateManager
  ) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.lightningManager = lightningManager;
    this.gameStateManager = gameStateManager;
    this.waveState = WaveState.INACTIVE;
    this.currentWave = 0;
    this.remainingMobs = 0;
    this.killCount = 0;
    this.difficultyMultiplier = 1.0;
  }

  /**
   * Set the MobSpawnManager to enable cleanup functionality
   */
  public void setMobSpawnManager(MobSpawnManager mobSpawnManager) {
    this.mobSpawnManager = mobSpawnManager;
  }

  public void startWave(int waveNumber) {
    this.waveState = WaveState.PREPARING;
    this.killCount = 0;

    // Calculate mob count based on wave number (more appropriate than player count for initial calculation)
    this.remainingMobs = calculateMobCount(waveNumber);

    long preparationDelaySeconds = plugin.getConfig().getLong("wave.preparation_delay_seconds", 10L);

    // Announce wave start
    Bukkit.broadcastMessage(
      "§6Wave " + waveNumber + " starting in " + preparationDelaySeconds + " seconds!"
    );

    // Start wave after delay
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          this.waveState = WaveState.ACTIVE;
          this.currentWave = waveNumber;
          this.gameStateManager.setCurrentWaveNumber(waveNumber);
          // Remove duplicate assignment - remainingMobs already set above
          this.difficultyMultiplier = 1.0 + (waveNumber * 0.1);

          // Start lightning strikes on boss waves
          if (waveNumber > 0 && waveNumber % 10 == 0) {
            lightningManager.start();
          }

          // Announce the wave start
          Bukkit.broadcastMessage("§cWave " + waveNumber + " has begun!");
        },
        preparationDelaySeconds * 20L
      ); // 10 seconds * 20 ticks
  }

  public void completeWave() {
    this.waveState = WaveState.INACTIVE;
    lightningManager.stop();
    // Any other wave completion logic can go here
    plugin.getServer().broadcastMessage("§aWave " + currentWave + " completed!");

    // Apply Hero of the Village effect every 5 waves
    if (currentWave > 0 && currentWave % 5 == 0) {
      Bukkit.getOnlinePlayers().forEach(player -> {
        // Apply Hero of the Village for 2 Minecraft days (48000 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 48000, 0));
        player.sendMessage("§5You are celebrated as the Hero of the Village!");
      });
    }

    long completionDelaySeconds = plugin.getConfig().getLong("wave.completion_delay_seconds", 10L);

    // Start next wave after delay
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          startWave(currentWave + 1);
        },
        completionDelaySeconds * 20L
      ); // 10 seconds * 20 ticks
  }

  public void stopWave() {
    this.waveState = WaveState.INACTIVE;
    this.remainingMobs = 0;
    lightningManager.stop();
  }

  public void onMobKill() {
    killCount++;
    remainingMobs--;

    // Safety check: prevent remainingMobs from going below 0
    if (remainingMobs < 0) {
      remainingMobs = 0;
    }

    if (remainingMobs <= 0 && waveState == WaveState.ACTIVE) {
      // Trigger instant mob cleanup before completing wave
      cleanupRemainingMobs();
      completeWave();
    }
  }

  public double getDifficultyMultiplier() {
    return difficultyMultiplier;
  }

  public double getHealthMultiplier(int playerCount) {
    return getDifficultyMultiplier() * (1 + 0.3 * playerCount);
  }

  private int calculateMobCount(int waveNumber) {
    int playerCount = Bukkit.getOnlinePlayers().size();
    // Base mob count increases with wave number, scaled by player count
    return (5 + waveNumber) + (2 * playerCount);
  }

  /**
   * Instantly cleanup all remaining hostile mobs when wave completion target is reached
   */
  private void cleanupRemainingMobs() {
    plugin.getLogger().info("Cleaning up remaining mobs for wave completion");
    if (mobSpawnManager != null) {
      mobSpawnManager.cleanupRemainingMobs();
    }
  }

  public boolean isWaveActive() {
    return waveState == WaveState.ACTIVE;
  }

  public boolean isPreparingWave() {
    return waveState == WaveState.PREPARING;
  }

  public int getCurrentWave() {
    return currentWave;
  }

  public int getRemainingMobs() {
    return remainingMobs;
  }

  public int getKillCount() {
    return killCount;
  }

  public int getTotalMobs() {
    return killCount + remainingMobs;
  }

  public WaveState getWaveState() {
    return waveState;
  }
}
