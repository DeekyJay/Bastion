package city.emerald.bastion.wave;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Creature;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import city.emerald.bastion.Bastion;
import city.emerald.bastion.VillageManager;

public class MobAI implements Listener {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private final WaveManager waveManager;
  private final Random random;

  private static final double PLAYER_TARGET_WEIGHT = 0.7;
  private static final double VILLAGER_TARGET_WEIGHT = 0.3;
  private static final double TARGET_SWITCH_CHANCE = 0.1;
  private static final double MAX_TARGET_DISTANCE = 50.0;

  public MobAI(
    Bastion plugin,
    VillageManager villageManager,
    WaveManager waveManager
  ) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.waveManager = waveManager;
    this.random = new Random();

    // Register events
    Bukkit.getPluginManager().registerEvents(this, plugin);

    // Start AI update task
    startAIUpdateTask();
  }

  private void startAIUpdateTask() {
    new BukkitRunnable() {
      @Override
      public void run() {
        if (!waveManager.isWaveActive()) {
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
      if (random.nextDouble() > TARGET_SWITCH_CHANCE) {
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
      mob.getLocation().distance(target.getLocation()) <= MAX_TARGET_DISTANCE
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

      double distance = mobLoc.distance(player.getLocation());
      if (distance > MAX_TARGET_DISTANCE) {
        continue;
      }

      double score =
        PLAYER_TARGET_WEIGHT * (1.0 - (distance / MAX_TARGET_DISTANCE));
      if (score > bestScore) {
        bestScore = score;
        bestTarget = player;
      }
    }

    // Consider villagers
    for (Villager villager : villageManager.getRegisteredVillagers()) {
      if (!villager.isValid() || villager.isDead()) {
        continue;
      }

      double distance = mobLoc.distance(villager.getLocation());
      if (distance > MAX_TARGET_DISTANCE) {
        continue;
      }

      double score =
        VILLAGER_TARGET_WEIGHT * (1.0 - (distance / MAX_TARGET_DISTANCE));
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
      !waveManager.isWaveActive() || !(event.getEntity() instanceof Monster)
    ) {
      return;
    }

    // Allow natural targeting but modify weights
    if (event.getTarget() instanceof Player) {
      if (random.nextDouble() > PLAYER_TARGET_WEIGHT) {
        event.setCancelled(true);
      }
    } else if (event.getTarget() instanceof Villager) {
      if (random.nextDouble() > VILLAGER_TARGET_WEIGHT) {
        event.setCancelled(true);
      }
    }
  }

  private boolean isHostileMob(Entity entity) {
    return (
      entity instanceof Monster ||
      entity instanceof Slime ||
      entity instanceof Flying
    );
  }
}
