package city.emerald.bastion.wave;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import city.emerald.bastion.Bastion;

public class CreeperExplosionManager implements Listener {

  private final Bastion plugin;
  private final Map<Creeper, ProgressMonitor> activeMonitors;
  
  // Configuration values
  private boolean enabled;
  private int countdownSeconds;
  private int progressCheckInterval; // in ticks
  private int positionHistorySize;

  public CreeperExplosionManager(Bastion plugin) {
    this.plugin = plugin;
    this.activeMonitors = new ConcurrentHashMap<>();
    loadConfiguration();
    
    // Register events
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  private void loadConfiguration() {
    this.enabled = plugin.getConfig().getBoolean("creeper-explosion.enabled", true);
    this.countdownSeconds = plugin.getConfig().getInt("creeper-explosion.countdown-seconds", 5);
    this.progressCheckInterval = plugin.getConfig().getInt("creeper-explosion.progress-check-interval", 20);
    this.positionHistorySize = plugin.getConfig().getInt("creeper-explosion.position-history-size", 3);
  }

  @EventHandler
  public void onEntityTarget(EntityTargetEvent event) {
    if (!enabled || event.isCancelled()) {
      return;
    }

    // Only handle creepers targeting players or villagers
    if (!(event.getEntity() instanceof Creeper)) {
      return;
    }

    Creeper creeper = (Creeper) event.getEntity();
    
    // Check if target is a LivingEntity first
    if (!(event.getTarget() instanceof LivingEntity)) {
      removeMonitor(creeper);
      return;
    }
    
    LivingEntity target = (LivingEntity) event.getTarget();

    // Only monitor if targeting a player or villager
    if (!(target instanceof Player) && !(target instanceof Villager)) {
      // Remove any existing monitor if creeper loses valid target
      removeMonitor(creeper);
      return;
    }

    // If creeper already has a monitor, remove it before creating new one
    removeMonitor(creeper);

    // Create new progress monitor for this creeper
    ProgressMonitor monitor = new ProgressMonitor(creeper, target);
    activeMonitors.put(creeper, monitor);
    monitor.start();
  }

  @EventHandler
  public void onEntityDeath(EntityDeathEvent event) {
    if (event.getEntity() instanceof Creeper) {
      removeMonitor((Creeper) event.getEntity());
    }
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent event) {
    if (event.getEntity() instanceof Creeper) {
      removeMonitor((Creeper) event.getEntity());
    }
  }

  private void removeMonitor(Creeper creeper) {
    ProgressMonitor monitor = activeMonitors.remove(creeper);
    if (monitor != null) {
      monitor.cleanup();
    }
  }

  /**
   * Clean up all active monitors (called when game ends or plugin disables)
   */
  public void cleanup() {
    for (ProgressMonitor monitor : activeMonitors.values()) {
      monitor.cleanup();
    }
    activeMonitors.clear();
  }

  private class ProgressMonitor {
    private final Creeper creeper;
    private final LivingEntity target;
    private BukkitTask countdownTask;
    private BukkitTask progressCheckTask;
    
    // Progress tracking fields
    private final Deque<Long> recentBlockPositions;
    private boolean isPreExplosion;

    public ProgressMonitor(Creeper creeper, LivingEntity target) {
      this.creeper = creeper;
      this.target = target;
      this.isPreExplosion = false;
      this.recentBlockPositions = new LinkedList<>();
      
      // Initialize tracking values
      resetProgress();
    }

    public void start() {
      startCountdownTask();
      startProgressCheckTask();
    }

    private void startCountdownTask() {
      countdownTask = new BukkitRunnable() {
        @Override
        public void run() {
          if (creeper.isValid() && !creeper.isDead()) {
            // Force explosion
            creeper.explode();
            plugin.getLogger().info("Creeper exploded due to pathfinding obstruction");
          }
          cleanup();
        }
      }.runTaskLater(plugin, countdownSeconds * 20L); // Convert seconds to ticks
    }

    private void startProgressCheckTask() {
      progressCheckTask = new BukkitRunnable() {
        @Override
        public void run() {
          if (!creeper.isValid() || creeper.isDead() || !target.isValid()) {
            cleanup();
            return;
          }

          // Check if creeper is in pre-explosion state
          if (creeper.getFuseTicks() > 0) {
            if (!isPreExplosion) {
              isPreExplosion = true;
              resetProgress();
              resetCountdown();
            }
            return;
          } else {
            isPreExplosion = false;
          }

          // Check for progress
          if (hasProgress()) {
            resetCountdown();
          }
        }
      }.runTaskTimer(plugin, progressCheckInterval, progressCheckInterval);
    }

    private void resetCountdown() {
      if (countdownTask != null && !countdownTask.isCancelled()) {
        countdownTask.cancel();
      }
      startCountdownTask();
    }

    private void resetProgress() {
      // Clear position history and add current block key
      recentBlockPositions.clear();
      recentBlockPositions.addLast(creeper.getLocation().toBlockKey());
    }

    private boolean hasProgress() {
      long currentBlockKey = creeper.getLocation().toBlockKey();
      
      // Check if current block key is different from any recent positions
      boolean hasNewPosition = !recentBlockPositions.contains(currentBlockKey);
      
      // Add current position to end of deque
      recentBlockPositions.addLast(currentBlockKey);
      
      // Maintain deque size by removing from front
      while (recentBlockPositions.size() > positionHistorySize) {
        recentBlockPositions.removeFirst();
      }
      
      return hasNewPosition;
    }

    public void cleanup() {
      if (countdownTask != null && !countdownTask.isCancelled()) {
        countdownTask.cancel();
      }
      if (progressCheckTask != null && !progressCheckTask.isCancelled()) {
        progressCheckTask.cancel();
      }
      activeMonitors.remove(creeper);
    }
  }
}
