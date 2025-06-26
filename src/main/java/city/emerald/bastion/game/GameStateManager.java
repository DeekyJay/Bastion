package city.emerald.bastion.game;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.wave.WaveManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameStateManager implements Listener {

  private final Bastion plugin;
  private final WaveManager waveManager;
  private final VillageManager villageManager;

  private GameState currentState;
  private final Map<UUID, Boolean> activePlayers;
  private boolean isGameActive;
  private int minPlayers;
  private int maxPlayers;

  public enum GameState {
    LOBBY("Waiting for players..."),
    PREPARING("Preparing game..."),
    ACTIVE("Game in progress"),
    COMPLETED("Game completed!");

    private final String message;

    GameState(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  public GameStateManager(
    Bastion plugin,
    WaveManager waveManager,
    VillageManager villageManager
  ) {
    this.plugin = plugin;
    this.waveManager = waveManager;
    this.villageManager = villageManager;
    this.currentState = GameState.LOBBY;
    this.activePlayers = new HashMap<>();
    this.isGameActive = false;
    this.minPlayers = plugin.getConfig().getInt("min-players", 1);
    this.maxPlayers = plugin.getConfig().getInt("max-players", 8);

    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void startGame() {
    if (currentState != GameState.LOBBY) {
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
          waveManager.startWave(1);
          Bukkit.broadcastMessage("§aGame started!");
        },
        200L
      ); // 10 seconds preparation
  }

  public void stopGame() {
    if (!isGameActive) {
      return;
    }

    waveManager.stopWave();
    currentState = GameState.LOBBY;
    isGameActive = false;
    activePlayers.clear();
    villageManager.cleanup();

    Bukkit.broadcastMessage("§cGame stopped!");
  }

  public void completeGame() {
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
        plugin.getConfig().getInt("max-waves", 30)
      ) {
        completeGame();
      }
    }
  }
}
