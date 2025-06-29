package city.emerald.bastion.game;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.economy.UpgradeManager;
import city.emerald.bastion.wave.WaveManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public class UIManager {

  private final Bastion plugin;
  private final WaveManager waveManager;
  private final VillageManager villageManager;
  private final GameStateManager gameStateManager;
  private final Map<UUID, Scoreboard> playerScoreboards;
  private BossBar waveProgressBar;
  private BukkitTask updateTask;

  public UIManager(
    Bastion plugin,
    WaveManager waveManager,
    VillageManager villageManager,
    GameStateManager gameStateManager
  ) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.villageManager = villageManager;
    this.gameStateManager = gameStateManager;
    this.playerScoreboards = new HashMap<>();

    setupBossBar();
    startUpdateTask();
  }

  private void setupBossBar() {
    waveProgressBar =
      Bukkit.createBossBar(
        "Wave Progress",
        BarColor.BLUE,
        BarStyle.SEGMENTED_10
      );
    waveProgressBar.setVisible(false);
  }

  private void startUpdateTask() {
    updateTask =
      Bukkit
        .getScheduler()
        .runTaskTimer(
          plugin,
          () -> {
            if (gameStateManager.isGameActive()) {
              updateAllDisplays();
            }
          },
          20L,
          20L
        ); // Update every second
  }

  public void cleanup() {
    if (updateTask != null) {
      updateTask.cancel();
    }
    waveProgressBar.removeAll();
    playerScoreboards.clear();
  }

  private void updateAllDisplays() {
    updateWaveProgress();
    for (Player player : Bukkit.getOnlinePlayers()) {
      updateScoreboard(player);
    }
  }

  private void updateWaveProgress() {
    if (waveManager.isWaveActive()) {
      String title = String.format(
        "Wave %d - %d enemies remaining",
        waveManager.getCurrentWave(),
        waveManager.getRemainingMobs()
      );
      waveProgressBar.setTitle(title);

      // Fixed progress calculation: killCount / totalMobs
      int totalMobs = waveManager.getTotalMobs();
      double progress = totalMobs > 0 ?
        (double) waveManager.getKillCount() / totalMobs : 0.0;
      waveProgressBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }
  }

  public void updateScoreboard(Player player) {
    ScoreboardManager manager = Bukkit.getScoreboardManager();
    Scoreboard board = playerScoreboards.computeIfAbsent(
      player.getUniqueId(),
      k -> manager.getNewScoreboard()
    );

    // Remove old objective if exists
    if (board.getObjective("bastion") != null) {
      board.getObjective("bastion").unregister();
    }

    // Create new objective
    Objective objective = board.registerNewObjective(
      "bastion",
      "dummy",
      "§6§lBastion"
    );
    objective.setDisplaySlot(DisplaySlot.SIDEBAR);

    // Add scores
    int line = 15;

    objective.getScore("§r").setScore(line--);
    objective
      .getScore(
        "§fGame State: §e" + gameStateManager.getCurrentState().getMessage()
      )
      .setScore(line--);
    objective.getScore("§r§r").setScore(line--);

    if (gameStateManager.isGameActive()) {
      objective
        .getScore("§fWave: §a" + waveManager.getCurrentWave())
        .setScore(line--);
      objective
        .getScore("§fEnemies: §c" + waveManager.getRemainingMobs())
        .setScore(line--);
      objective.getScore("§r§r§r").setScore(line--);

      objective
        .getScore(
          "§fVillagers: §e" + villageManager.getRegisteredVillagers().size()
        )
        .setScore(line--);
      objective
        .getScore("§fKills: §a" + waveManager.getKillCount())
        .setScore(line--);
    }

    player.setScoreboard(board);
  }

  public void showUpgradeAvailable(
    Player player,
    UpgradeManager.UpgradeType upgrade
  ) {
    player.sendTitle(
      "§a⭐ Upgrade Available!",
      "§e" + upgrade.getName() + " can be purchased",
      10,
      40,
      10
    );
  }

  public void showWaveStarting(int waveNumber) {
    waveProgressBar.setVisible(true);
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendTitle(
        "§6Wave " + waveNumber,
        "§ePrepare for battle!",
        10,
        40,
        10
      );
    }
  }

  public void showWaveComplete(int waveNumber) {
    waveProgressBar.setVisible(false);
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendTitle(
        "§aWave " + waveNumber + " Complete!",
        "§eGet ready for the next wave...",
        10,
        40,
        10
      );
    }
  }

  public void showGameOver(int wavesCompleted, int villagersSaved) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      player.sendTitle(
        "§c§lGame Over!",
        String.format(
          "§eWaves: %d | Villagers Saved: %d",
          wavesCompleted,
          villagersSaved
        ),
        20,
        60,
        20
      );
    }
    cleanup();
  }

  public void addPlayer(Player player) {
    waveProgressBar.addPlayer(player);
    updateScoreboard(player);
  }

  public void removePlayer(Player player) {
    waveProgressBar.removePlayer(player);
    playerScoreboards.remove(player.getUniqueId());
    player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
  }
}
