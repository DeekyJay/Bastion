package city.emerald.bastion;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import city.emerald.bastion.command.ConfigCommand;
import city.emerald.bastion.economy.LootManager;
import city.emerald.bastion.economy.TradeManager;
import city.emerald.bastion.economy.UpgradeManager;
import city.emerald.bastion.game.GameStateManager;
import city.emerald.bastion.game.StatsManager;
import city.emerald.bastion.game.UIManager;
import city.emerald.bastion.wave.CreeperExplosionManager;
import city.emerald.bastion.wave.LightningManager;
import city.emerald.bastion.wave.MobAI;
import city.emerald.bastion.wave.MobSpawnManager;
import city.emerald.bastion.wave.WaveManager;

public final class Bastion extends JavaPlugin implements Listener {

  private FileConfiguration config;
  private Logger logger;
  private GameStateManager gameStateManager;
  private UIManager uiManager;
  private StatsManager statsManager;
  private VillageManager villageManager;
  private BarrierManager barrierManager;
  private WaveManager waveManager;
  private MobSpawnManager mobSpawnManager;
  private MobAI mobAI;
  private LootManager lootManager;
  private TradeManager tradeManager;
  private UpgradeManager upgradeManager;
  private LightningManager lightningManager;
  private CreeperExplosionManager creeperExplosionManager;

  @Override
  public void onEnable() {
    // Initialize logger and config
    logger = getLogger();
    saveDefaultConfig();
    config = getConfig();

    // Initialize managers in the correct order to resolve dependencies
    // 1. Standalone managers
    gameStateManager = new GameStateManager(this);
    villageManager = new VillageManager(this);
    statsManager = new StatsManager(this);

    // 2. Managers that depend on standalone managers
    barrierManager = new BarrierManager(this, villageManager);
    lightningManager = new LightningManager(this, barrierManager);
    waveManager = new WaveManager(this, villageManager, lightningManager, gameStateManager);
    lootManager = new LootManager(this, gameStateManager);

    // Initialize MobSpawnManager
    mobSpawnManager = new MobSpawnManager(this, villageManager, barrierManager, lootManager);
    mobSpawnManager.setWaveManager(waveManager);
  
    tradeManager = new TradeManager(this, villageManager, waveManager);
    upgradeManager = new UpgradeManager(this, villageManager);
    uiManager = new UIManager(this, waveManager, villageManager, gameStateManager);
    creeperExplosionManager = new CreeperExplosionManager(this);

    // Initialize MobAI
    mobAI = new MobAI(this, villageManager, gameStateManager);
    getServer().getPluginManager().registerEvents(mobAI, this);

    // 3. Inject dependencies using setters to break circular dependencies
    gameStateManager.setWaveManager(waveManager);
    gameStateManager.setVillageManager(villageManager);
    mobSpawnManager.setWaveManager(waveManager);
    waveManager.setMobSpawnManager(mobSpawnManager);
    villageManager.setUpgradeManager(upgradeManager);
    villageManager.setBarrierManager(barrierManager);

    // Register event listeners
    getServer().getPluginManager().registerEvents(this, this);
    getServer().getPluginManager().registerEvents(gameStateManager, this);
    getServer().getPluginManager().registerEvents(mobSpawnManager, this);

    // Register commands
    ConfigCommand configCommand = new ConfigCommand(this);
    getCommand("bastionconfig").setExecutor(configCommand);
    getCommand("bastionconfig").setTabCompleter(configCommand);

    logger.info("Bastion plugin enabled successfully!");
  }

  @EventHandler
  public void onMobDeath(EntityDeathEvent event) {
    if (!gameStateManager.isGameActive()) { return; }

    if (event.getEntity() instanceof Monster) {
      waveManager.onMobKill();
    }

    lootManager.handleMobDeath(event);
  }

  @Override
  public void onDisable() {
    // Save any necessary data
    saveConfig();

    // Cleanup managers and active games
    if (gameStateManager.isGameActive()) {
      gameStateManager.stopGame();
    }
    uiManager.cleanup();
    statsManager.saveStats();
    creeperExplosionManager.cleanup();

    logger.info("Bastion plugin disabled successfully!");
  }

  @Override
  public boolean onCommand(
    CommandSender sender,
    Command command,
    String label,
    String[] args
  ) {
    if (sender == null) {
      return false; // Cannot handle command without a sender
    }
    if (command.getName().equalsIgnoreCase("bastion")) {
      if (args.length == 0) {
        sender.sendMessage("§6=== Bastion Commands ===");
        sender.sendMessage(
          "§e/bastion findvillage §7- Find and select a village"
        );
        sender.sendMessage("§e/bastion barrier §7- Toggle the barrier");
        sender.sendMessage("§e/bastion start §7- Start a new defense wave");
        sender.sendMessage("§e/bastion stop §7- Stop the current game");
        sender.sendMessage("§e/bastion info §7- Show game status");
        if (sender.hasPermission("bastion.admin")) {
          sender.sendMessage("§e/bastion pause §7- Pause the current game");
          sender.sendMessage("§e/bastion resume §7- Resume the paused game");
          sender.sendMessage("§e/bastion debug §7- Toggle debug mode");
          sender.sendMessage("§e/bastion stats <player> §7- View player stats");
        }
        sender.sendMessage(
          "§e/bastion upgrade player <type> §7- Purchase player upgrade"
        );
        sender.sendMessage(
          "§e/bastion upgrade village <type> §7- Purchase village upgrade"
        );
        sender.sendMessage("§e/bastion upgrades §7- List available upgrades");
        return true;
      }

      switch (args[0].toLowerCase()) {
        case "findvillage":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to select villages!");
            return true;
          }
          if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
          }
          Player player = (Player) sender;
          if (villageManager.findAndSelectVillage(player.getWorld())) {
            sender.sendMessage("§aVillage found and selected!");
            
            // Teleport all online players to the village
            for (Player onlinePlayer : getServer().getOnlinePlayers()) {
              barrierManager.teleportToVillageCenter(onlinePlayer);
            }
            
            // Announce to all players
            getServer().broadcastMessage("§aAll players have been teleported to the selected village!");
          } else {
            sender.sendMessage("§cNo valid village found nearby!");
          }
          break;
        case "barrier":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage(
              "§cYou don't have permission to control the barrier!"
            );
            return true;
          }
          if (!villageManager.getVillageCenter().isPresent()) {
            sender.sendMessage(
              "§cNo village selected! Use /bastion findvillage first."
            );
            return true;
          }
          if (barrierManager.isActive()) {
            barrierManager.deactivate();
            sender.sendMessage("§cBarrier deactivated.");
          } else {
            barrierManager.activate();
            // Register villagers now that players are in the village and chunks are loaded
            //villageManager.registerVillagersInRange(((Player) sender).getWorld());
            sender.sendMessage("§aBarrier activated!");
          }
          break;
        case "start":
          if (!sender.hasPermission("bastion.start")) {
            sender.sendMessage("§cYou don't have permission to start waves!");
            return true;
          }
          if (!villageManager.getVillageCenter().isPresent()) {
            sender.sendMessage(
              "§cNo village selected! Use /bastion findvillage first."
            );
            return true;
          }
          if (!barrierManager.isActive()) {
            sender.sendMessage(
              "§cBarrier must be active! Use /bastion barrier first."
            );
            return true;
          }
          // Ensure villagers are registered before starting the game
          villageManager.registerVillagersInRange(((Player) sender).getWorld());
          gameStateManager.startGame();
          statsManager.onGameStart();
          sender.sendMessage("§aStarting new game...");
          break;
        case "stop":
          if (!sender.hasPermission("bastion.stop")) {
            sender.sendMessage("§cYou don't have permission to stop waves!");
            return true;
          }
          gameStateManager.stopGame();
          statsManager.onGameEnd();
          sender.sendMessage("§cGame stopped.");
          break;
        case "pause":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to pause the game!");
            return true;
          }
          if (!gameStateManager.isPaused() && (gameStateManager.isGameActive() || gameStateManager.getCurrentState() == GameStateManager.GameState.PREPARING)) {
            gameStateManager.pauseGame();
            sender.sendMessage("§6Game paused.");
            getServer().broadcastMessage("§6Game has been paused by an admin.");
          } else if (gameStateManager.isPaused()) {
            sender.sendMessage("§eGame is already paused.");
          } else {
            sender.sendMessage("§cNo game is running to pause.");
          }
          break;
        case "resume":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to resume the game!");
            return true;
          }
          if (gameStateManager.isPaused()) {
            gameStateManager.resumeGame();
            sender.sendMessage("§aGame resumed.");
            getServer().broadcastMessage("§aGame has been resumed by an admin.");
          } else {
            sender.sendMessage("§cGame is not paused.");
          }
          break;
        case "upgrade":
          if (args.length < 3) {
            sender.sendMessage(
              "§cUsage: /bastion upgrade <player|village> <type>"
            );
            return true;
          }
          if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
          }
          handleUpgradeCommand((Player) sender, args[1], args[2]);
          break;
        case "upgrades":
          if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
          }
          showUpgrades((Player) sender);
          break;
        case "info":
          showGameStatus(sender);
          break;
        case "debug":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to use debug commands!");
            return true;
          }
          // Toggle debug mode implementation would go here
          sender.sendMessage("§eDebug command not yet implemented.");
          break;
        case "stats":
          if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to view player stats!");
            return true;
          }
          if (args.length < 2) {
            sender.sendMessage("§cUsage: /bastion stats <player>");
            return true;
          }
          // Player stats viewing implementation would go here
          sender.sendMessage("§eStats command not yet implemented.");
          break;
        default:
          sender.sendMessage("§cUnknown command. Use /bastion for help.");
          break;
      }

      return true;
    }

    return false;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    if (gameStateManager.isGameActive()) {
      uiManager.addPlayer(player);
      player.sendMessage("§6A village defense game is currently in progress!");
      player.sendMessage(
        "§eGame State: " + gameStateManager.getCurrentState().getMessage()
      );
    }
  }

  @EventHandler
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!gameStateManager.isGameActive()) return;

    // Handle player damage upgrades
    if (event.getDamager() instanceof Player) {
      Player player = (Player) event.getDamager();
      int damageLevel = upgradeManager.getPlayerUpgradeLevel(
        player,
        UpgradeManager.UpgradeType.DAMAGE_BOOST
      );
      if (damageLevel > 0) {
        event.setDamage(event.getDamage() * (1 + (damageLevel * 0.2))); // 20% increase per level
      }
    }

    // Handle protection upgrades for players
    if (event.getEntity() instanceof Player) {
      Player player = (Player) event.getEntity();
      int protectionLevel = upgradeManager.getPlayerUpgradeLevel(
        player,
        UpgradeManager.UpgradeType.PROTECTION
      );
      if (protectionLevel > 0) {
        event.setDamage(event.getDamage() * (1 - (protectionLevel * 0.1))); // 10% reduction per level
      }
    }

    // Handle barrier protection for villagers
    if (event.getEntity() instanceof org.bukkit.entity.Villager) {
      double reduction = villageManager.getBarrierDamageReduction();
      if (reduction > 0) {
        event.setDamage(event.getDamage() * (1 - reduction));
      }
    }
  }

  // Utility methods for wave management (to be implemented)
  private void startWave(int waveNumber) {
    waveManager.startWave(waveNumber);
    mobSpawnManager.startSpawning();
    Bukkit.broadcastMessage("§6Wave " + waveNumber + " is starting!");
  }

  private void stopWave() {
    waveManager.stopWave();
    mobSpawnManager.stopSpawning();
    Bukkit.broadcastMessage("§cWave stopped!");
  }

  @EventHandler
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!gameStateManager.isGameActive() || !waveManager.isWaveActive()) {
      return;
    }

    // Note: Mob kill tracking is handled in onMobDeath() method to prevent double counting
  }

  /**
   * Handles the upgrade command
   */
  private void handleUpgradeCommand(
    Player player,
    String type,
    String upgradeName
  ) {
    try {
      UpgradeManager.UpgradeType upgrade = UpgradeManager.UpgradeType.valueOf(
        upgradeName.toUpperCase()
      );

      boolean success;
      if (type.equalsIgnoreCase("player")) {
        success = upgradeManager.purchasePlayerUpgrade(player, upgrade);
      } else if (type.equalsIgnoreCase("village")) {
        success = upgradeManager.purchaseVillageUpgrade(player, upgrade);
      } else {
        player.sendMessage(
          "§cInvalid upgrade type. Use 'player' or 'village'."
        );
        return;
      }

      if (!success) {
        player.sendMessage("§cFailed to purchase upgrade.");
      }
    } catch (IllegalArgumentException e) {
      player.sendMessage(
        "§cInvalid upgrade type. Use /bastion upgrades to see available upgrades."
      );
    }
  }

  /**
   * Shows detailed game status
   */
  private void showGameStatus(CommandSender sender) {
    sender.sendMessage("§6=== Bastion Status ===");
    sender.sendMessage(
      "§7Village Selected: §f" +
      (villageManager.getVillageCenter().isPresent() ? "Yes" : "No")
    );
    sender.sendMessage(
      "§7Barrier Active: §f" + (barrierManager.isActive() ? "Yes" : "No")
    );
    sender.sendMessage(
      "§7Game State: §f" + gameStateManager.getCurrentState().getMessage()
    );
    
    if (gameStateManager.isPaused()) {
      sender.sendMessage("§7Status: §6PAUSED");
    }

    if (villageManager.getVillageCenter().isPresent()) {
      sender.sendMessage(
        "§7Villagers Protected: §f" +
        villageManager.getRegisteredVillagers().size()
      );
    }

    if (gameStateManager.isGameActive()) {
      sender.sendMessage("§7Wave: §f" + waveManager.getCurrentWave());
      sender.sendMessage(
        "§7Remaining Mobs: §f" + waveManager.getRemainingMobs()
      );
      sender.sendMessage(
        "§7Total Kills: §f" +
        statsManager.getCurrentGameStats().getTotalMobsKilled()
      );
      
      if (waveManager.isWaveActive() && !gameStateManager.isPaused()) {
        long remainingTime = waveManager.getRemainingTime();
        if (remainingTime > 0) {
          long minutes = remainingTime / 60;
          long seconds = remainingTime % 60;
          sender.sendMessage(String.format("§7Time Remaining: §f%d:%02d", minutes, seconds));
        }
      }
    }
  }

  /**
   * Shows available upgrades and their costs
   */
  private void showUpgrades(Player player) {
    player.sendMessage("§6=== Available Upgrades ===");
    player.sendMessage("§ePlayer Upgrades:");
    for (UpgradeManager.UpgradeType upgrade : UpgradeManager.UpgradeType.values()) {
      int level = upgradeManager.getPlayerUpgradeLevel(player, upgrade);
      if (level < upgrade.getMaxLevel()) {
        int cost = upgrade.getBaseCost() * (level + 1);
        player.sendMessage(
          String.format(
            "§7- %s (Level %d/%d) - %d emeralds",
            upgrade.getName(),
            level,
            upgrade.getMaxLevel(),
            cost
          )
        );
      }
    }

    player.sendMessage("\n§eVillage Upgrades:");
    for (UpgradeManager.UpgradeType upgrade : UpgradeManager.UpgradeType.values()) {
      int level = upgradeManager.getVillageUpgradeLevel(upgrade);
      if (level < upgrade.getMaxLevel()) {
        int cost = upgrade.getBaseCost() * (level + 1);
        player.sendMessage(
          String.format(
            "§7- %s (Level %d/%d) - %d emeralds",
            upgrade.getName(),
            level,
            upgrade.getMaxLevel(),
            cost
          )
        );
      }
    }
  }
}
