package city.emerald.bastion;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BarrierManager implements Listener {

  private final Bastion plugin;
  private final VillageManager villageManager;
  private boolean isActive;
  private final int BARRIER_RADIUS = 50; // 100x100 area means 50 block radius
  private final int BARRIER_HEIGHT = 256;
  private BukkitRunnable particleTask;

  public BarrierManager(Bastion plugin, VillageManager villageManager) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.isActive = false;
    // Register both the barrier and spawn prevention events
    plugin.getServer().getPluginManager().registerEvents(this, plugin);

    // Register event to prevent natural mob spawning
    plugin
      .getServer()
      .getPluginManager()
      .registerEvents(
        new Listener() {
          @EventHandler
          public void onCreatureSpawn(
            org.bukkit.event.entity.CreatureSpawnEvent event
          ) {
            if (!isActive) return;

            // Allow only our custom spawns and villagers
            if (
              event.getSpawnReason() !=
              org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM &&
              event.getEntityType() != org.bukkit.entity.EntityType.VILLAGER
            ) {
              Location loc = event.getLocation();
              if (
                villageManager.getVillageCenter().isPresent() &&
                isInBarrier(loc, villageManager.getVillageCenter().get())
              ) {
                event.setCancelled(true);
              }
            }
          }
        },
        plugin
      );
  }

  /**
   * Activates the barrier system.
   */
  public void activate() {
    if (!villageManager.getVillageCenter().isPresent()) {
      throw new IllegalStateException(
        "Cannot activate barrier without a village center!"
      );
    }

    isActive = true;
    clearFriendlyMobs();
    startParticleEffect();
  }

  /**
   * Deactivates the barrier system.
   */
  public void deactivate() {
    isActive = false;
    if (particleTask != null) {
      particleTask.cancel();
      particleTask = null;
    }
  }

  /**
   * Creates particle effects to visualize the barrier.
   */
  private void startParticleEffect() {
    particleTask =
      new BukkitRunnable() {
        @Override
        public void run() {
          if (!villageManager.getVillageCenter().isPresent()) {
            deactivate();
            return;
          }
          Location center = villageManager.getVillageCenter().get();
          World world = center.getWorld();

          // Display particles in a circular pattern
          // More frequent particles
          for (int degree = 0; degree < 360; degree += 2) {
            double radian = Math.toRadians(degree);
            double x = center.getX() + (BARRIER_RADIUS * Math.cos(radian));
            double z = center.getZ() + (BARRIER_RADIUS * Math.sin(radian));

            // Create vertical line of particles
            // More visible particles
            for (int y = 0; y < BARRIER_HEIGHT; y += 4) {
              Location particleLoc = new Location(
                world,
                x,
                center.getY() + y,
                z
              );
              // Brighter, larger particles
              world.spawnParticle(
                Particle.DUST,
                particleLoc,
                1,
                0,
                0,
                0,
                new DustOptions(Color.fromRGB(255, 0, 0), 2.0f)
              );
            }
          }
        }
      };

    // Run particle effect every 20 ticks (1 second)
    particleTask.runTaskTimer(plugin, 0L, 20L);
  }

  /**
   * Handles player teleportation to the village center.
   * @param player The player to teleport
   */
  public void teleportToVillageCenter(Player player) {
    villageManager
      .getVillageCenter()
      .ifPresent(center -> {
        // Find safe location near center
        Location safe = findSafeLocation(center);
        player.teleport(safe);
      });
  }

  /**
   * Finds a safe location near the specified center point.
   * @param center The center location
   * @return A safe location for teleportation
   */
  private Location findSafeLocation(Location center) {
    World world = center.getWorld();
    Location safe = center.clone();

    // Find highest solid block
    safe.setY(world.getHighestBlockYAt(safe));
    // Move up one block to ensure player stands on surface
    safe.add(0, 1, 0);

    return safe;
  }

  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (!isActive || !villageManager.getVillageCenter().isPresent()) {
      return;
    }

    Location center = villageManager.getVillageCenter().get();
    Location playerLoc = event.getTo();

    // Check if player is trying to move outside barrier
    if (!isInBarrier(playerLoc, center)) {
      event.setCancelled(true);

      // Push player back towards center
      Vector pushDirection = center
        .toVector()
        .subtract(playerLoc.toVector())
        .normalize();
      event.getPlayer().setVelocity(pushDirection.multiply(0.5));

      // Notify player
      event.getPlayer().sendMessage("§cYou cannot leave the protected area!");
    }
  }

  /**
   * Checks if a location is within the barrier bounds.
   * @param location The location to check
   * @param center The barrier center
   * @return true if the location is within bounds
   */
  public boolean isInBarrier(Location location, Location center) {
    // Check horizontal distance
    if (location.distanceSquared(center) > BARRIER_RADIUS * BARRIER_RADIUS) {
      return false;
    }

    // Check vertical bounds
    return location.getY() >= 0 && location.getY() <= BARRIER_HEIGHT;
  }

  /**
   * Handles player respawn within the barrier.
   * @param player The player to respawn
   */
  public void handleRespawn(Player player) {
    if (isActive && villageManager.getVillageCenter().isPresent()) {
      teleportToVillageCenter(player);
    }
  }

  /**
   * Checks if the barrier is currently active.
   * @return true if the barrier is active
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Clears friendly mobs from the barrier area to reduce mob cap usage
   */
  private void clearFriendlyMobs() {
    if (!villageManager.getVillageCenter().isPresent()) {
      return;
    }

    Location center = villageManager.getVillageCenter().get();
    World world = center.getWorld();

    // Remove all non-hostile entities except villagers and players
    world
      .getEntities()
      .forEach(entity -> {
        if (
          entity instanceof org.bukkit.entity.LivingEntity &&
          !(entity instanceof org.bukkit.entity.Monster) &&
          !(entity instanceof org.bukkit.entity.Villager) &&
          !(entity instanceof org.bukkit.entity.Player) &&
          !(entity instanceof org.bukkit.entity.Golem) &&
          isInBarrier(entity.getLocation(), center)
        ) {
          entity.remove();
        }
      });
  }
}
