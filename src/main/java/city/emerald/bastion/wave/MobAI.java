package city.emerald.bastion.wave;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;
import city.emerald.bastion.game.GameStateManager;
import net.md_5.bungee.api.ChatColor;

public class MobAI implements Listener {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private final GameStateManager gameStateManager;
  private final Random random;

  private final double playerTargetWeight;
  private final double villagerTargetWeight;
  private final double targetSwitchChance;
  private final double maxTargetDistance;
  private final long scanIntervalTicks;

  private final int creeperVisionRadius;

  public MobAI(
    Bastion plugin,
    VillageManager villageManager,
    GameStateManager gameStateManager
  ) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.gameStateManager = gameStateManager;
    this.random = new Random();

    this.creeperVisionRadius = plugin.getIntSafe("mob_ai.creeper_vision_radius", 10);
    this.playerTargetWeight = plugin.getDoubleSafe("mob_ai.player_target_weight", 0.7);
    this.villagerTargetWeight = plugin.getDoubleSafe("mob_ai.villager_target_weight", 0.3);
    this.targetSwitchChance = plugin.getDoubleSafe("mob_ai.target_switch_chance", 0.1);
    this.maxTargetDistance = plugin.getDoubleSafe("mob_ai.max_target_distance", 50.0);
    this.scanIntervalTicks = plugin.getLongSafe("mob_ai.scan_interval_ticks", 100);

    // Start AI update task
    startAIUpdateTask();

    // Start the periodic task
    new BukkitRunnable() {
      @Override
      public void run() {
        scanForTargets();
      }
    }.runTaskTimer(plugin, 0L, scanIntervalTicks);
  }

  public void registerEvents() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  private void startAIUpdateTask() {
    new BukkitRunnable() {
      @Override
      public void run() {
        if (gameStateManager.getCurrentState() != GameStateManager.GameState.ACTIVE) {
          return;
        }
        updateAllMobAI();
      }
    }
      .runTaskTimer(plugin, 20L, 20L); // Update every second
  }

  private void updateAllMobAI() {
    if (!villageManager.getVillageCenter().isPresent()) {
      return;
    }

    Location center = villageManager.getVillageCenter().get();
    World world = center.getWorld();

    // Process all hostile mobs in the world
    world
      .getLivingEntities()
      .stream()
      .filter(this::isHostileMob)
      .forEach(this::updateMobAI);
  }

  private void updateMobAI(LivingEntity mob) {
    // Skip if mob already has a valid target
    if (hasValidTarget(mob)) {
      // Chance to switch targets
      if (random.nextDouble() > targetSwitchChance) {
        return;
      }
    }

    // Find new target
    LivingEntity target = findBestTarget(mob);
    if (target != null) {
      if (mob instanceof Creature) {
        ((Creature) mob).setTarget(target);
      }
    }

    // Special behavior for ranged mobs
    if (mob instanceof Skeleton) {
      handleRangedMobBehavior((Skeleton) mob);
    }
  }

  private boolean hasValidTarget(LivingEntity mob) {
    if (!(mob instanceof Creature)) {
      return false;
    }

    LivingEntity target = ((Creature) mob).getTarget();
    if (target == null || !target.isValid() || target.isDead()) {
      return false;
    }

    return (
      mob.getLocation().distance(target.getLocation()) <= maxTargetDistance
    );
  }

  private LivingEntity findBestTarget(LivingEntity mob) {
    Location mobLoc = mob.getLocation();
    LivingEntity bestTarget = null;
    double bestScore = -1;

    // Consider players
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (!player.isValid() || player.isDead()) {
        continue;
      }

      if (player != null) {
        Location playerLocation = player.getLocation();
        if (playerLocation != null) {
          double distance = mobLoc.distance(playerLocation);
          if (distance > maxTargetDistance) {
            continue;
          }

          double score = playerTargetWeight * (1.0 - (distance / maxTargetDistance));
          if (score > bestScore) {
            bestScore = score;
            bestTarget = player;
          }
        }
      }
    }

    // Consider villagers
    for (Villager villager : villageManager.getRegisteredVillagers()) {
        Location villagerLocation = villager.getLocation();
        double distance = mobLoc.distance(villagerLocation);
        if (distance > maxTargetDistance) {
            continue;
        }

        double score = villagerTargetWeight * (1.0 - (distance / maxTargetDistance));
        if (score > bestScore) {
            bestScore = score;
            bestTarget = villager;
        }
    }

    return bestTarget;
  }

  private void handleRangedMobBehavior(Skeleton skeleton) {
    LivingEntity target = skeleton.getTarget();
    if (target == null) {
      return;
    }

    Location mobLoc = skeleton.getLocation();
    Location targetLoc = target.getLocation();
    double distance = mobLoc.distance(targetLoc);

    // Try to maintain optimal range (10-15 blocks)
    if (distance < 10) {
      // Move away from target
      Vector direction = mobLoc
        .toVector()
        .subtract(targetLoc.toVector())
        .normalize();
      Location retreatLoc = mobLoc.add(direction.multiply(5));
      skeleton.getPathfinder().moveTo(retreatLoc, 1.2);
    }
  }

  @EventHandler
  public void onEntityTarget(EntityTargetEvent event) {
    if (
      gameStateManager.getCurrentState() != GameStateManager.GameState.ACTIVE || !(event.getEntity() instanceof Monster)
    ) {
      return;
    }

    // Allow natural targeting but modify weights
    if (event.getTarget() instanceof Player) {
      if (random.nextDouble() > playerTargetWeight) {
        event.setCancelled(true);
      }
    } else if (event.getTarget() instanceof Villager) {
      if (random.nextDouble() > villagerTargetWeight) {
        event.setCancelled(true);
      }
    }

    if (!(event.getEntity() instanceof org.bukkit.entity.Creeper)) {
      return;
    }

    org.bukkit.entity.Creeper creeper = (org.bukkit.entity.Creeper) event.getEntity();
    Location creeperLocation = creeper.getLocation();
    World world = creeperLocation.getWorld();

    // Find nearby players and villagers within the creeper vision radius
    world.getNearbyEntities(creeperLocation, creeperVisionRadius, creeperVisionRadius, creeperVisionRadius)
        .stream()
        .filter(entity -> entity instanceof Player || entity instanceof Villager)
        .findFirst()
        .ifPresent(target -> {
            event.setTarget((LivingEntity) target);
            plugin.getLogger().info(String.format("Creeper targeting entity through solid blocks: %s", target));
        });
  }

  private void scanForTargets() {
    for (World world : Bukkit.getWorlds()) {
      for (LivingEntity entity : world.getLivingEntities()) {
        if (!(entity instanceof Creeper creeper)) continue;

        // Skip creepers that already have a valid target
        if (creeper.getTarget() != null && creeper.getTarget().isValid() && !creeper.getTarget().isDead()) continue;

        LivingEntity hiddenEntity = findHiddenEntityNearby(creeper);
        if (hiddenEntity != null) {
          EntityTargetLivingEntityEvent event = new EntityTargetLivingEntityEvent(
            creeper,
            hiddenEntity,
            EntityTargetEvent.TargetReason.CUSTOM
          );

          Bukkit.getPluginManager().callEvent(event);

          if (!event.isCancelled()) {
            creeper.setTarget(event.getTarget());
            //logToChat("Creeper set target to hidden entity: " + event.getTarget().getName());
          }
        }
      }
    }
  }

  private LivingEntity findHiddenEntityNearby(Creeper creeper) {
    Location creepLoc = creeper.getLocation();
    double radiusSquared = creeperVisionRadius * creeperVisionRadius;

    for (Player player : creepLoc.getWorld().getPlayers()) {
      if (!player.isValid() || player.isDead()) continue;
      if (player.getGameMode() != GameMode.SURVIVAL) continue;
      if (creepLoc.distanceSquared(player.getLocation()) > radiusSquared) continue;

      // Only target if player is hidden
      if (!creeper.hasLineOfSight(player)) {
        return player;
      }
    }

    for (LivingEntity entity : creepLoc.getWorld().getLivingEntities()) {
      if (!(entity instanceof Villager villager)) continue;
      if (!villager.isValid() || villager.isDead()) continue;
      if (creepLoc.distanceSquared(villager.getLocation()) > radiusSquared) continue;

      // Only target if villager is hidden
      if (!creeper.hasLineOfSight(villager)) {
        return villager;
      }
    }

    return null;
  }

  @EventHandler
  public void onTarget(EntityTargetLivingEntityEvent event) {
    if (!(event.getEntity() instanceof Creeper creeper)) return;
    if (!(event.getTarget() instanceof LivingEntity target)) return;

    if (!creeper.hasLineOfSight(target)) {
      event.setTarget(target); // override targeting
    }
  }

  private boolean isHostileMob(Entity entity) {
    return (
      entity instanceof Monster ||
      entity instanceof Slime ||
      entity instanceof Flying
    );
  }

  private void logToChat(String message) {
    for (Player player : Bukkit.getOnlinePlayers()) {
        player.sendMessage(ChatColor.GREEN + "[MobAI Debug] " + message);
    }
  }
}
