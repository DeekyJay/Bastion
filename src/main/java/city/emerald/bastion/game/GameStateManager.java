package city.emerald.bastion.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.wave.WaveManager;

public class GameStateManager implements Listener {

  private final Bastion plugin;
  private WaveManager waveManager;
  private VillageManager villageManager;

  private GameState currentState;
  private final Map<UUID, Boolean> activePlayers;
  private boolean isGameActive;
  private int minPlayers;
  private int maxPlayers;
  private int currentWaveNumber = 0;
  
  // Pause state management
  private GameState pausedFromState;

  public enum GameState {
    LOBBY("Waiting for players..."),
    PREPARING("Preparing game..."),
    ACTIVE("Game in progress"),
    PAUSED("Game paused"),
    FAILED("Wave failed - restarting..."),
    COMPLETED("Game completed!");

    private final String message;

    GameState(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  public GameStateManager(Bastion plugin) {
    this.plugin = plugin;
    this.currentState = GameState.LOBBY;
    this.activePlayers = new HashMap<>();
    this.isGameActive = false;
    this.minPlayers = plugin.getIntSafe("min_players", 1);
    this.maxPlayers = plugin.getIntSafe("max_players", 8);

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void setWaveManager(WaveManager waveManager) {
    this.waveManager = waveManager;
  }

  public void setVillageManager(VillageManager villageManager) {
    this.villageManager = villageManager;
  }

  public void startGame() {
    if (currentState != GameState.LOBBY || waveManager == null) {
      return;
    }

    // Add all online players to active players
    for (Player player : Bukkit.getOnlinePlayers()) {
      activePlayers.put(player.getUniqueId(), true);
    }

    if (activePlayers.size() < minPlayers) {
      Bukkit.broadcastMessage(
        "§cNot enough players to start! Need at least " +
        minPlayers +
        " players."
      );
      return;
    }

    currentState = GameState.PREPARING;
    isGameActive = true;

    // Start preparation phase
    Bukkit.broadcastMessage("§6Preparing game...");
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          currentState = GameState.ACTIVE;
          if (waveManager != null) waveManager.startWave(1);
          Bukkit.broadcastMessage("§aGame started!");
        },
        200L
      ); // 10 seconds preparation
  }

  public void stopGame() {
    if (currentState == GameState.LOBBY) {
      return;
    }

    currentState = GameState.LOBBY;
    isGameActive = false;
    activePlayers.clear();

    if (waveManager != null) {
      waveManager.stopWave();
    }

    Bukkit.broadcastMessage("§cGame stopped!");
  }

  public void endGame() {
    if (currentState != GameState.ACTIVE) {
      return;
    }

    currentState = GameState.COMPLETED;
    isGameActive = false;

    Bukkit.broadcastMessage("§6=== Game Completed! ===");
    Bukkit.broadcastMessage(
      "§eWaves Survived: §f" + waveManager.getCurrentWave()
    );
    Bukkit.broadcastMessage(
      "§eVillagers Protected: §f" +
      villageManager.getRegisteredVillagers().size()
    );

    // Reset after delay
    Bukkit
      .getScheduler()
      .runTaskLater(
        plugin,
        () -> {
          stopGame();
        },
        200L
      );
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (isGameActive) {
      if (
        currentState == GameState.ACTIVE && activePlayers.size() < maxPlayers
      ) {
        activePlayers.put(player.getUniqueId(), true);
        player.sendMessage("§aYou've joined an active game!");
      } else {
        player.sendMessage(
          "§cA game is in progress. Please wait for the next round."
        );
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    activePlayers.remove(player.getUniqueId());

    if (isGameActive && activePlayers.isEmpty()) {
      stopGame();
      Bukkit.broadcastMessage("§cGame ended - All players left!");
    }
  }

  public GameState getCurrentState() {
    return currentState;
  }

  public void setCurrentState(GameState state) {
    this.currentState = state;
  }

  public int getCurrentWaveNumber() {
    return currentWaveNumber;
  }

  public void setCurrentWaveNumber(int waveNumber) {
    this.currentWaveNumber = waveNumber;
  }

  public boolean isGameActive() {
    return isGameActive;
  }

  public Map<UUID, Boolean> getActivePlayers() {
    return new HashMap<>(activePlayers);
  }

  public void checkWaveCompletion() {
    if (
      currentState == GameState.ACTIVE && waveManager.getRemainingMobs() <= 0
    ) {
      if (
        waveManager.getCurrentWave() >=
        plugin.getIntSafe("max_waves", 30)
      ) {
        endGame();
      }
    }
  }
  
  /**
   * Pause the game if it's in a pauseable state
   * @return true if successfully paused, false if cannot pause
   */
  public boolean pauseGame() {
    if (currentState == GameState.ACTIVE || currentState == GameState.PREPARING) {
      pausedFromState = currentState;
      currentState = GameState.PAUSED;
      return true;
    }
    return false;
  }
  
  /**
   * Resume the game from paused state
   * @return true if successfully resumed, false if not paused
   */
  public boolean resumeGame() {
    if (currentState == GameState.PAUSED && pausedFromState != null) {
      currentState = pausedFromState;
      pausedFromState = null;
      return true;
    }
    return false;
  }
  
  /**
   * Check if the game is currently paused
   * @return true if in PAUSED state
   */
  public boolean isPaused() {
    return currentState == GameState.PAUSED;
  }
  
  /**
   * Check if a new wave can start (not paused)
   * @return true if waves can start/continue
   */
  public boolean canStartWave() {
    return !isPaused();
  }
}
