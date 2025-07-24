package city.emerald.bastion.wave;

import org.bukkit.Bukkit;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

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
  // remainingMobs and killCount no longer used - we use living mob count directly
  
  // Wave timer fields
  private BukkitTask waveTimerTask;
  private long waveStartTime;

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
    // Removed remainingMobs and killCount - using living mob count directly
  }

  /**
   * Set the MobSpawnManager to enable cleanup functionality
   */
  public void setMobSpawnManager(MobSpawnManager mobSpawnManager) {
    this.mobSpawnManager = mobSpawnManager;
  }

  public void startWave(int waveNumber) {
    // Check if waves can start (not paused)
    if (!gameStateManager.canStartWave()) {
      plugin.getLogger().info("Cannot start wave - game is paused");
      return;
    }
    
    this.waveState = WaveState.PREPARING;
    // No longer track killCount - using living mob count directly

    // Calculate mob count for spawning
    final int mobsToSpawn = calculateMobCount(waveNumber);

    long preparationDelaySeconds = plugin.getLongSafe("wave.preparation_delay_seconds", 10L);

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
          
          // Record wave start time and start timer
          this.waveStartTime = System.currentTimeMillis();
          startWaveTimer();

          // Start lightning strikes on boss waves
          if (waveNumber > 0 && waveNumber % 10 == 0) {
            lightningManager.start();
          }

          // Spawn the wave using the calculated mob count
          if (mobSpawnManager != null) {
            mobSpawnManager.spawnWave(waveNumber, mobsToSpawn);
          }

          // Announce the wave start
          Bukkit.broadcastMessage("§cWave " + waveNumber + " has begun!");
        },
        preparationDelaySeconds * 20L
      ); // 10 seconds * 20 ticks
  }

  public void completeWave() {
    // Cancel the timer first
    if (waveTimerTask != null) {
      waveTimerTask.cancel();
      waveTimerTask = null;
    }
    
    // Determine next wave based on current state
    GameStateManager.GameState currentState = gameStateManager.getCurrentState();
    int nextWave = currentWave; // Default: repeat same wave
    
    if (currentState == GameStateManager.GameState.COMPLETED) {
      // Wave succeeded - advance to next wave
      nextWave = currentWave + 1;
      plugin.getServer().broadcastMessage("§aWave " + currentWave + " completed!");
      
      // Apply Hero of the Village effect every 5 waves
      if (currentWave > 0 && currentWave % 5 == 0) {
        Bukkit.getOnlinePlayers().forEach(player -> {
          // Apply Hero of the Village for 2 Minecraft days (48000 ticks)
          player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, 48000, 0));
          player.sendMessage("§5You are celebrated as the Hero of the Village!");
        });
      }
    } else if (currentState == GameStateManager.GameState.FAILED) {
      // Wave failed - restart same wave
      plugin.getServer().broadcastMessage("§cWave " + currentWave + " failed! Restarting...");
    }
    
    // Common cleanup
    this.waveState = WaveState.INACTIVE;
    lightningManager.stop();
    
    // Schedule next wave (either repeat or advance)
    final int finalNextWave = nextWave;
    long completionDelaySeconds = plugin.getLongSafe("wave.completion_delay_seconds", 10L);
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      startWave(finalNextWave);
    }, completionDelaySeconds * 20L);
  }

  public void stopWave() {
    this.waveState = WaveState.INACTIVE;
    // No longer track remainingMobs - using living mob count directly
    
    // Cancel wave timer
    if (waveTimerTask != null) {
      waveTimerTask.cancel();
      waveTimerTask = null;
    }
    
    lightningManager.stop();
  }

  public void onMobKill() {
    // This method is deprecated - wave completion is now checked via timer using living mob count
    // Can be removed entirely
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
    // Return living mob count from MobSpawnManager
    if (mobSpawnManager != null) {
      return mobSpawnManager.getImmediateLivingMobCount();
    }
    return 0;
  }

  public int getKillCount() {
    // Calculate kill count as total spawned minus living
    if (mobSpawnManager != null) {
      int totalSpawned = calculateMobCount(currentWave); // This is the intended spawn count
      int livingCount = mobSpawnManager.getImmediateLivingMobCount();
      return Math.max(0, totalSpawned - livingCount);
    }
    return 0;
  }

  public int getTotalMobs() {
    // Return the intended total mob count for current wave
    return calculateMobCount(currentWave);
  }

  public void adjustRemainingMobs(int actualSpawned) {
    // This method is no longer needed - living mob count is tracked automatically
    // Kept for compatibility but does nothing
  }

  public WaveState getWaveState() {
    return waveState;
  }
  
  /**
   * Start the wave timer that runs every second to check for timeout and countdown
   */
  private void startWaveTimer() {
    if (waveTimerTask != null) {
      waveTimerTask.cancel();
    }
    
    waveTimerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::handleWaveTimer, 20L, 20L);
  }
  
  /**
   * Handle wave timer tick - runs every second during active wave
   */
  private void handleWaveTimer() {
    if (waveState != WaveState.ACTIVE) {
      return;
    }
    
    // Check if game is paused - if so, do nothing
    if (gameStateManager.isPaused()) {
      return;
    }
    
    // Check for wave completion using living mob count
    if (mobSpawnManager != null) {
      int livingMobs = mobSpawnManager.getImmediateLivingMobCount();
      if (livingMobs <= 0) {
        // All mobs killed - set COMPLETED state and complete wave
        gameStateManager.setCurrentState(GameStateManager.GameState.COMPLETED);
        cleanupRemainingMobs();
        completeWave();
        return;
      }
    }
    
    // Reload config value (allows runtime changes)
    int waveDurationSeconds = plugin.getIntSafe("wave.wave_duration_seconds", 300);
    
    long elapsedSeconds = getElapsedTime();
    long remainingSeconds = waveDurationSeconds - elapsedSeconds;
    
    if (remainingSeconds <= 0) {
      // Time expired - set FAILED state and complete wave
      gameStateManager.setCurrentState(GameStateManager.GameState.FAILED);
      cleanupRemainingMobs();
      Bukkit.broadcastMessage("§cTime's up! Wave failed - restarting at same difficulty...");
      completeWave();
    } else if (remainingSeconds <= 10) {
      // Display countdown
      Bukkit.broadcastMessage("§c" + remainingSeconds + " seconds remaining!");
    }
    // Otherwise do nothing
  }
  
  /**
   * Get elapsed time since wave started in seconds
   */
  private long getElapsedTime() {
    return (System.currentTimeMillis() - waveStartTime) / 1000;
  }
  
  /**
   * Get remaining time in current wave in seconds
   */
  public long getRemainingTime() {
    int waveDurationSeconds = plugin.getIntSafe("wave.wave_duration_seconds", 300);
    return waveDurationSeconds - getElapsedTime();
  }
}
