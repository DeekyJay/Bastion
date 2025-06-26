package city.emerald.bastion.game;

import city.emerald.bastion.Bastion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class StatsManager {

  private final Bastion plugin;
  private final File statsFile;
  private FileConfiguration statsConfig;

  // Session stats
  private final Map<UUID, PlayerStats> sessionStats;
  private GameStats currentGameStats;

  public StatsManager(Bastion plugin) {
    this.plugin = plugin;
    this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
    this.sessionStats = new HashMap<>();
    this.currentGameStats = new GameStats();

    loadStats();
  }

  private void loadStats() {
    if (!statsFile.exists()) {
      plugin.saveResource("stats.yml", false);
    }
    statsConfig = YamlConfiguration.loadConfiguration(statsFile);
  }

  public void saveStats() {
    try {
      statsConfig.save(statsFile);
    } catch (IOException e) {
      plugin.getLogger().severe("Could not save stats to " + statsFile);
      e.printStackTrace();
    }
  }

  public static class PlayerStats {

    private int kills;
    private int deaths;
    private int wavesPlayed;
    private int wavesWon;
    private int emeraldsSpent;
    private int upgradesPurchased;
    private int villagersSaved;

    public PlayerStats() {
      this.kills = 0;
      this.deaths = 0;
      this.wavesPlayed = 0;
      this.wavesWon = 0;
      this.emeraldsSpent = 0;
      this.upgradesPurchased = 0;
      this.villagersSaved = 0;
    }

    public void incrementKills() {
      kills++;
    }

    public void incrementDeaths() {
      deaths++;
    }

    public void incrementWavesPlayed() {
      wavesPlayed++;
    }

    public void incrementWavesWon() {
      wavesWon++;
    }

    public void addEmeraldsSpent(int amount) {
      emeraldsSpent += amount;
    }

    public void incrementUpgradesPurchased() {
      upgradesPurchased++;
    }

    public void setVillagersSaved(int count) {
      villagersSaved = count;
    }

    // Getters
    public int getKills() {
      return kills;
    }

    public int getDeaths() {
      return deaths;
    }

    public int getWavesPlayed() {
      return wavesPlayed;
    }

    public int getWavesWon() {
      return wavesWon;
    }

    public int getEmeraldsSpent() {
      return emeraldsSpent;
    }

    public int getUpgradesPurchased() {
      return upgradesPurchased;
    }

    public int getVillagersSaved() {
      return villagersSaved;
    }
  }

  public static class GameStats {

    private int maxWaveReached;
    private int totalVillagersSaved;
    private int totalMobsKilled;
    private int totalEmeraldsCollected;
    private int totalUpgradesPurchased;
    private long startTime;
    private long endTime;

    public GameStats() {
      this.maxWaveReached = 0;
      this.totalVillagersSaved = 0;
      this.totalMobsKilled = 0;
      this.totalEmeraldsCollected = 0;
      this.totalUpgradesPurchased = 0;
      this.startTime = System.currentTimeMillis();
    }

    public void setMaxWaveReached(int wave) {
      maxWaveReached = wave;
    }

    public void setTotalVillagersSaved(int count) {
      totalVillagersSaved = count;
    }

    public void incrementMobsKilled() {
      totalMobsKilled++;
    }

    public void addEmeraldsCollected(int amount) {
      totalEmeraldsCollected += amount;
    }

    public void incrementUpgradesPurchased() {
      totalUpgradesPurchased++;
    }

    public void endGame() {
      endTime = System.currentTimeMillis();
    }

    public int getMaxWaveReached() {
      return maxWaveReached;
    }

    public int getTotalVillagersSaved() {
      return totalVillagersSaved;
    }

    public int getTotalMobsKilled() {
      return totalMobsKilled;
    }

    public int getTotalEmeraldsCollected() {
      return totalEmeraldsCollected;
    }

    public int getTotalUpgradesPurchased() {
      return totalUpgradesPurchased;
    }

    public long getDuration() {
      return endTime - startTime;
    }
  }

  public PlayerStats getPlayerStats(Player player) {
    return sessionStats.computeIfAbsent(
      player.getUniqueId(),
      k -> new PlayerStats()
    );
  }

  public void onGameStart() {
    currentGameStats = new GameStats();
    sessionStats.clear();
  }

  public void onGameEnd() {
    currentGameStats.endGame();
    saveGameStats();
    updatePlayerStats();
  }

  private void saveGameStats() {
    ConfigurationSection gameHistory = statsConfig.createSection(
      "game-history." + System.currentTimeMillis()
    );
    gameHistory.set("max-wave", currentGameStats.getMaxWaveReached());
    gameHistory.set(
      "villagers-saved",
      currentGameStats.getTotalVillagersSaved()
    );
    gameHistory.set("mobs-killed", currentGameStats.getTotalMobsKilled());
    gameHistory.set(
      "emeralds-collected",
      currentGameStats.getTotalEmeraldsCollected()
    );
    gameHistory.set(
      "upgrades-purchased",
      currentGameStats.getTotalUpgradesPurchased()
    );
    gameHistory.set("duration", currentGameStats.getDuration());

    List<String> players = new ArrayList<>();
    for (UUID id : sessionStats.keySet()) {
      Player player = Bukkit.getPlayer(id);
      if (player != null) {
        players.add(player.getName());
      }
    }
    gameHistory.set("players", players);

    saveStats();
  }

  private void updatePlayerStats() {
    for (Map.Entry<UUID, PlayerStats> entry : sessionStats.entrySet()) {
      UUID playerId = entry.getKey();
      PlayerStats stats = entry.getValue();

      String path = "player-stats." + playerId;
      statsConfig.set(
        path + ".kills",
        statsConfig.getInt(path + ".kills", 0) + stats.getKills()
      );
      statsConfig.set(
        path + ".deaths",
        statsConfig.getInt(path + ".deaths", 0) + stats.getDeaths()
      );
      statsConfig.set(
        path + ".waves-played",
        statsConfig.getInt(path + ".waves-played", 0) + stats.getWavesPlayed()
      );
      statsConfig.set(
        path + ".waves-won",
        statsConfig.getInt(path + ".waves-won", 0) + stats.getWavesWon()
      );
      statsConfig.set(
        path + ".emeralds-spent",
        statsConfig.getInt(path + ".emeralds-spent", 0) +
        stats.getEmeraldsSpent()
      );
      statsConfig.set(
        path + ".upgrades-purchased",
        statsConfig.getInt(path + ".upgrades-purchased", 0) +
        stats.getUpgradesPurchased()
      );

      // Update high scores
      int highestWave = statsConfig.getInt(path + ".highest-wave", 0);
      if (currentGameStats.getMaxWaveReached() > highestWave) {
        statsConfig.set(
          path + ".highest-wave",
          currentGameStats.getMaxWaveReached()
        );
      }

      int mostVillagers = statsConfig.getInt(path + ".most-villagers-saved", 0);
      if (stats.getVillagersSaved() > mostVillagers) {
        statsConfig.set(
          path + ".most-villagers-saved",
          stats.getVillagersSaved()
        );
      }
    }

    saveStats();
  }

  public GameStats getCurrentGameStats() {
    return currentGameStats;
  }

  public String generateEndGameSummary() {
    StringBuilder summary = new StringBuilder();
    summary.append("§6=== Game Summary ===\n");
    summary
      .append("§eWaves Completed: §f")
      .append(currentGameStats.getMaxWaveReached())
      .append("\n");
    summary
      .append("§eVillagers Saved: §f")
      .append(currentGameStats.getTotalVillagersSaved())
      .append("\n");
    summary
      .append("§eTotal Kills: §f")
      .append(currentGameStats.getTotalMobsKilled())
      .append("\n");
    summary
      .append("§eEmeralds Collected: §f")
      .append(currentGameStats.getTotalEmeraldsCollected())
      .append("\n");
    summary
      .append("§eUpgrades Purchased: §f")
      .append(currentGameStats.getTotalUpgradesPurchased())
      .append("\n");
    summary
      .append("§eDuration: §f")
      .append(formatDuration(currentGameStats.getDuration()))
      .append("\n");
    return summary.toString();
  }

  private String formatDuration(long ms) {
    long seconds = ms / 1000;
    long minutes = seconds / 60;
    seconds = seconds % 60;
    return String.format("%d:%02d", minutes, seconds);
  }
}
