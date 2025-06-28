package city.emerald.bastion.wave;

import org.bukkit.Bukkit;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;

public class WaveManager {

  private final Bastion plugin;
  private final VillageManager villageManager;

  private int currentWave;
  private WaveState waveState;
  private int remainingMobs;
  private int killCount;
  private final double BASE_DIFFICULTY_MULTIPLIER = 1.5;

  public enum WaveState {
    INACTIVE,
    PREPARING,
    ACTIVE,
    COMPLETED,
  }

  public WaveManager(Bastion plugin, VillageManager villageManager) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.currentWave = 0;
    this.waveState = WaveState.INACTIVE;
    this.remainingMobs = 0;
    this.killCount = 0;
  }

  public void startWave(int waveNumber) {
    this.currentWave = waveNumber;
    this.waveState = WaveState.PREPARING;
    this.killCount = 0;

    // Calculate mob count based on player count
    int playerCount = Bukkit.getOnlinePlayers().size();
    this.remainingMobs = calculateMobCount(playerCount);

    // Announce wave start
    Bukkit.broadcastMessage(
      "§6Wave " + waveNumber + " starting in 10 seconds!"
    );

    // Start wave after delay
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          this.waveState = WaveState.ACTIVE;
          Bukkit.broadcastMessage("§cWave " + waveNumber + " has begun!");
        },
        200L
      ); // 10 seconds * 20 ticks
  }

  public void stopWave() {
    this.waveState = WaveState.INACTIVE;
    this.currentWave = 0;
    this.remainingMobs = 0;
    this.killCount = 0;
  }

  public void onMobKill() {
    killCount++;
    remainingMobs--;

    if (remainingMobs <= 0 && waveState == WaveState.ACTIVE) {
      completeWave();
    }
  }

  private void completeWave() {
    this.waveState = WaveState.COMPLETED;
    Bukkit.broadcastMessage("§aWave " + currentWave + " completed!");

    // Start next wave after delay
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          startWave(currentWave + 1);
        },
        200L
      ); // 10 seconds * 20 ticks
  }

  public double getDifficultyMultiplier() {
    return Math.pow(BASE_DIFFICULTY_MULTIPLIER, currentWave - 1);
  }

  public double getHealthMultiplier(int playerCount) {
    return getDifficultyMultiplier() * (1 + 0.3 * playerCount);
  }

  private int calculateMobCount(int playerCount) {
    return 10 + (2 * playerCount);
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

  public WaveState getWaveState() {
    return waveState;
  }
}
