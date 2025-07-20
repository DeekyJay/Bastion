package city.emerald.bastion;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.block.Block;
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
  private int barrierRadius;
  private int barrierHeight;
  private boolean domeShape;
  private boolean showUnderground;
  private double particleDensity;
  private int particleSpacing;
  private int updateInterval;
  private BukkitRunnable particleTask;

  // Enhanced particle system settings
  private float particleSize;
  private boolean useMultipleParticleTypes;
  private boolean enableParticleAnimation;
  private int maxUndergroundDepth;
  private boolean debugMode;
  private Color primaryColor;
  private Color secondaryColor;
  private int particleIntensity;

  public BarrierManager(Bastion plugin, VillageManager villageManager) {
    this.plugin = plugin;
    this.villageManager = villageManager;
    this.isActive = false;

    // Load configuration values
    loadConfiguration();

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

  public VillageManager getVillageManager() {
    return villageManager;
  }

  /**
   * Loads configuration values from the plugin config
   */
  private void loadConfiguration() {
    this.barrierRadius =
      plugin.getIntSafe("village.barrier.radius", 80);
    this.barrierHeight =
      plugin.getIntSafe("village.barrier.height", 256);
    this.domeShape =
      plugin.getBooleanSafe("village.barrier.dome_shape", true);
    this.showUnderground =
      plugin.getBooleanSafe("village.barrier.show_underground", true);
    this.particleDensity =
      plugin.getDoubleSafe("effects.barrier.particle_density", 1.0);
    this.particleSpacing =
      plugin.getIntSafe("effects.barrier.particle_spacing", 2);
    this.updateInterval =
      plugin.getIntSafe("effects.barrier.update_interval", 20);

    // Enhanced particle settings
    this.particleSize =
      (float) plugin.getDoubleSafe("effects.barrier.particle_size", 4.0);
    this.useMultipleParticleTypes =
      plugin.getBooleanSafe("effects.barrier.multiple_particle_types", true);
    this.enableParticleAnimation =
      plugin.getBooleanSafe("effects.barrier.particle_animation", true);
    this.maxUndergroundDepth =
      plugin.getIntSafe("effects.barrier.max_underground_depth", 20);
    this.debugMode =
      plugin.getBooleanSafe("effects.barrier.debug_mode", false);
    this.particleIntensity =
      plugin.getIntSafe("effects.barrier.particle_intensity", 3);

    // Load particle colors
    String primaryColorStr = plugin.getStringSafe("effects.barrier.primary_color", "0,255,255"); // Bright cyan
    String secondaryColorStr = plugin.getStringSafe("effects.barrier.secondary_color", "255,255,0"); // Bright yellow
    this.primaryColor = parseColor(primaryColorStr);
    this.secondaryColor = parseColor(secondaryColorStr);
  }

  /**
   * Parses a color string in format "R,G,B" to Color object
   */
  private Color parseColor(String colorStr) {
    try {
      String[] parts = colorStr.split(",");
      if (parts.length == 3) {
        int r = Integer.parseInt(parts[0].trim());
        int g = Integer.parseInt(parts[1].trim());
        int b = Integer.parseInt(parts[2].trim());
        return Color.fromRGB(
          Math.max(0, Math.min(255, r)),
          Math.max(0, Math.min(255, g)),
          Math.max(0, Math.min(255, b))
        );
      }
    } catch (Exception e) {
      if (debugMode) {
        plugin
          .getLogger()
          .warning(
            "Failed to parse color: " + colorStr + ", using default cyan"
          );
      }
    }
    return Color.fromRGB(0, 255, 255); // Default to cyan
  }

  /**
   * Reloads configuration values (useful for config changes)
   */
  public void reloadConfiguration() {
    loadConfiguration();
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

          if (domeShape) {
            generateDomeParticles(world, center);
          } else {
            generateCylindricalParticles(world, center);
          }
        }
      };

    // Run particle effect using configured interval
    particleTask.runTaskTimer(plugin, 0L, updateInterval);
  }

  /**
   * Generates 3D dome-shaped particle pattern extending underground
   */
  private void generateDomeParticles(World world, Location center) {
    double angleStep = Math.toRadians(
      particleSpacing * 360.0 / (2 * Math.PI * barrierRadius)
    );

    // Generate full sphere using spherical coordinates, but limit underground extent
    for (double phi = 0; phi <= Math.PI; phi += angleStep) { // Full elevation range (0 to 180 degrees)
      double ringRadius = barrierRadius * Math.sin(phi);
      double yOffset = barrierRadius * Math.cos(phi);

      if (ringRadius < 1) continue; // Skip very small rings near the poles

      // For underground sections, limit how deep we go
      double actualY = center.getY() + yOffset;

      // Don't go below world minimum or too far underground
      int worldMinY = world.getMinHeight();
      int maxUndergroundY = center.getBlockY() - maxUndergroundDepth;

      if (actualY < Math.max(worldMinY, maxUndergroundY)) {
        continue; // Skip particles that are too deep underground
      }

      double circumference = 2 * Math.PI * ringRadius;
      int pointsOnRing = Math.max(8, (int) (circumference / particleSpacing));

      for (int i = 0; i < pointsOnRing; i++) {
        double theta = 2 * Math.PI * i / pointsOnRing; // Azimuth angle

        double x = center.getX() + ringRadius * Math.cos(theta);
        double y = actualY;
        double z = center.getZ() + ringRadius * Math.sin(theta);

        Location particleLoc = new Location(world, x, y, z);

        // Enhanced visibility check for underground particles
        if (shouldShowParticleAtLocation(particleLoc, center)) {
          spawnBarrierParticle(world, particleLoc);
        }
      }
    }
  }

  /**
   * Determines if a particle should be shown at the given location
   */
  private boolean shouldShowParticleAtLocation(
    Location particleLoc,
    Location center
  ) {
    // Always show particles above ground level
    if (particleLoc.getY() >= center.getY()) {
      return showUnderground || isLocationVisible(particleLoc);
    }

    // For underground particles, only show if underground mode is enabled AND there are air spaces
    if (!showUnderground) {
      return false;
    }

    // Enhanced underground detection
    return isLocationVisible(particleLoc);
  }

  /**
   * Generates cylindrical wall particle pattern extending underground (legacy mode)
   */
  private void generateCylindricalParticles(World world, Location center) {
    for (int degree = 0; degree < 360; degree += particleSpacing) {
      double radian = Math.toRadians(degree);
      double x = center.getX() + (barrierRadius * Math.cos(radian));
      double z = center.getZ() + (barrierRadius * Math.sin(radian));

      // Calculate underground and above-ground ranges
      int worldMinY = world.getMinHeight();
      int maxUndergroundY = center.getBlockY() - maxUndergroundDepth;
      int startY = Math.max(worldMinY, maxUndergroundY);
      int endY = center.getBlockY() + barrierHeight;

      // Generate particles from underground up to barrier height
      for (int y = startY; y < endY; y += particleSpacing) {
        Location particleLoc = new Location(world, x, y, z);

        if (shouldShowParticleAtLocation(particleLoc, center)) {
          spawnBarrierParticle(world, particleLoc);
        }
      }
    }
  }

  /**
   * Enhanced visibility check for underground cave detection
   */
  private boolean isLocationVisible(Location location) {
    Block block = location.getBlock();
    Material type = block.getType();

    // Check if the current location is air-like
    if (isAirLike(type)) {
      return true;
    }

    // If we're not showing underground particles, stop here
    if (!showUnderground) {
      return false;
    }

    // For underground detection, check if there are air spaces below this location
    return hasAirSpacesBelow(location);
  }

  /**
   * Checks if a material is considered air-like for barrier visibility
   */
  private boolean isAirLike(Material type) {
    return (
      type == Material.AIR ||
      type == Material.CAVE_AIR ||
      type == Material.VOID_AIR ||
      type == Material.WATER ||
      type == Material.LAVA ||
      !type.isSolid()
    );
  }

  /**
   * Enhanced underground cave detection - checks for air spaces below surface level
   */
  private boolean hasAirSpacesBelow(Location location) {
    World world = location.getWorld();
    int surfaceY = world.getHighestBlockYAt(location);
    int currentY = location.getBlockY();

    // If we're above surface, use normal air check
    if (currentY >= surfaceY) {
      return isAirLike(location.getBlock().getType());
    }

    // We're below surface - check for air spaces in a small radius around this position
    for (int checkY = currentY - 2; checkY <= currentY + 2; checkY++) {
      if (
        checkY < world.getMinHeight() ||
        checkY > Math.min(currentY + maxUndergroundDepth, surfaceY)
      ) {
        continue;
      }

      for (int dx = -1; dx <= 1; dx++) {
        for (int dz = -1; dz <= 1; dz++) {
          Location checkLoc = new Location(
            world,
            location.getX() + dx,
            checkY,
            location.getZ() + dz
          );

          if (isAirLike(checkLoc.getBlock().getType())) {
            if (debugMode) {
              plugin
                .getLogger()
                .info(
                  "Found air space at " +
                  checkLoc +
                  " near barrier position " +
                  location
                );
            }
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Enhanced particle spawning with multiple effects and animation
   */
  private void spawnBarrierParticle(World world, Location location) {
    // Calculate animation effects
    long currentTime = System.currentTimeMillis();
    double animationPhase = enableParticleAnimation
      ? Math.sin(currentTime / 1000.0 + location.getX() + location.getZ())
      : 0;

    // Determine colors based on animation
    Color currentPrimaryColor = primaryColor;
    Color currentSecondaryColor = secondaryColor;

    if (enableParticleAnimation) {
      // Pulse between primary and secondary colors
      double pulseIntensity = (animationPhase + 1.0) / 2.0; // Normalize to 0-1
      currentPrimaryColor =
        blendColors(primaryColor, secondaryColor, pulseIntensity);
    }

    // Spawn multiple particle types for better visibility
    for (int i = 0; i < particleIntensity; i++) {
      spawnPrimaryDustParticle(world, location, currentPrimaryColor);
      if (useMultipleParticleTypes) {
        spawnAdditionalParticles(world, location, i);
      }
    }

    // Debug particle spawning
    if (debugMode) {
      spawnDebugParticle(world, location);
    }
  }

  /**
   * Spawns the primary dust particle at the given location.
   */
  private void spawnPrimaryDustParticle(
    World world,
    Location location,
    Color color
  ) {
    world.spawnParticle(
      Particle.DUST,
      location.getX() + (Math.random() - 0.5) * 0.3,
      location.getY() + (Math.random() - 0.5) * 0.3,
      location.getZ() + (Math.random() - 0.5) * 0.3,
      1,
      0,
      0,
      0,
      new DustOptions(color, particleSize * (float) particleDensity)
    );
  }

  /**
   * Spawns additional particle types for enhanced effects.
   */
  private void spawnAdditionalParticles(World world, Location location, int i) {
    // Add flame particles for extra visibility
    if (i == 0) {
      world.spawnParticle(
        Particle.FLAME,
        location.getX(),
        location.getY(),
        location.getZ(),
        1,
        0.1,
        0.1,
        0.1,
        0.01
      );
    }

    // Add enchantment table particles for magical effect
    if (i == 1 && particleIntensity > 1) {
      world.spawnParticle(
        Particle.ENCHANT,
        location.getX(),
        location.getY() + 0.5,
        location.getZ(),
        2,
        0.2,
        0.2,
        0.2,
        0.5
      );
    }

    // Add firework spark for high intensity
    if (i == 2 && particleIntensity > 2) {
      world.spawnParticle(
        Particle.FIREWORK,
        location.getX(),
        location.getY(),
        location.getZ(),
        1,
        0.1,
        0.1,
        0.1,
        0.1
      );
    }
  }

  /**
   * Spawns a debug particle at the given location.
   */
  private void spawnDebugParticle(World world, Location location) {
    world.spawnParticle(
      Particle.DUST,
      location.getX(),
      location.getY() + 1,
      location.getZ(),
      1,
      0,
      0,
      0,
      new DustOptions(Color.fromRGB(255, 255, 255), 1.0f)
    );
  }

  /**
   * Blends two colors based on intensity (0.0 = color1, 1.0 = color2)
   */
  private Color blendColors(Color color1, Color color2, double intensity) {
    intensity = Math.max(0.0, Math.min(1.0, intensity)); // Clamp to 0-1

    int r1 = color1.getRed();
    int g1 = color1.getGreen();
    int b1 = color1.getBlue();

    int r2 = color2.getRed();
    int g2 = color2.getGreen();
    int b2 = color2.getBlue();

    int r = (int) (r1 + (r2 - r1) * intensity);
    int g = (int) (g1 + (g2 - g1) * intensity);
    int b = (int) (b1 + (b2 - b1) * intensity);

    return Color.fromRGB(r, g, b);
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
    if (domeShape) {
      return isWithinDome(location, center);
    } else {
      return isWithinCylinder(location, center);
    }
  }

  /**
   * Checks if a location is within the spherical dome barrier
   */
  private boolean isWithinDome(Location location, Location center) {
    // Calculate 3D distance from center
    double dx = location.getX() - center.getX();
    double dy = location.getY() - center.getY();
    double dz = location.getZ() - center.getZ();

    // For dome, only consider locations above the center Y level
    if (dy < 0) {
      // Below center level, use cylindrical bounds
      double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
      return horizontalDistance <= barrierRadius;
    }

    // Above center level, use spherical bounds
    double distance3D = Math.sqrt(dx * dx + dy * dy + dz * dz);
    return distance3D <= barrierRadius;
  }

  /**
   * Checks if a location is within the cylindrical barrier (legacy mode)
   */
  private boolean isWithinCylinder(Location location, Location center) {
    // Check horizontal distance
    double horizontalDistance = Math.sqrt(
      Math.pow(location.getX() - center.getX(), 2) +
      Math.pow(location.getZ() - center.getZ(), 2)
    );

    if (horizontalDistance > barrierRadius) {
      return false;
    }

    // Check vertical bounds
    World world = center.getWorld();
    return (
      location.getY() >= world.getMinHeight() &&
      location.getY() <= barrierHeight
    );
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
   * Gets the current barrier radius.
   * @return the barrier radius in blocks
   */
  public int getBarrierRadius() {
    return barrierRadius;
  }

  /**
   * Gets the current barrier height.
   * @return the barrier height in blocks
   */
  public int getBarrierHeight() {
    return barrierHeight;
  }

  /**
   * Checks if dome shape is enabled.
   * @return true if dome shape is enabled
   */
  public boolean isDomeShape() {
    return domeShape;
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
