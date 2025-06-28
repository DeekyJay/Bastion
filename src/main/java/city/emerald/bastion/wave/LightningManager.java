package city.emerald.bastion.wave;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitTask;

import city.emerald.bastion.BarrierManager;
import city.emerald.bastion.Bastion;

public class LightningManager {

    private final Bastion plugin;
    private final BarrierManager barrierManager;
    private BukkitTask lightningTask;

    public LightningManager(Bastion plugin, BarrierManager barrierManager) {
        this.plugin = plugin;
        this.barrierManager = barrierManager;
    }

    /**
     * Starts the lightning strike task.
     * This should be called at the beginning of a boss wave.
     */
    public void start() {
        // Ensure any existing task is stopped before starting a new one.
        if (lightningTask != null && !lightningTask.isCancelled()) {
            lightningTask.cancel();
        }

        // Start a new repeating task that runs every 5 seconds (100 ticks).
        lightningTask = Bukkit.getScheduler().runTaskTimer(plugin, this::strikeRandomTarget, 100L, 100L);
    }

    /**
     * Stops the lightning strike task.
     * This should be called at the end of a boss wave.
     */
    public void stop() {
        if (lightningTask != null && !lightningTask.isCancelled()) {
            lightningTask.cancel();
            lightningTask = null;
        }
    }

    /**
     * The core logic that finds a target and strikes it with lightning.
     * This will be called by the repeating Bukkit task.
     */
    private void strikeRandomTarget() {
        barrierManager.getVillageManager().getVillageCenter().ifPresent(center -> {
            // Combine lists of all potential targets within the barrier
            List<Entity> potentialTargets = new java.util.ArrayList<>();

            // Add players in the barrier
            potentialTargets.addAll(center.getWorld().getEntitiesByClass(Player.class).stream()
                .filter(player -> barrierManager.isInBarrier(player.getLocation(), center))
                .collect(java.util.stream.Collectors.toList()));

            // Add villagers in the barrier
            potentialTargets.addAll(center.getWorld().getEntitiesByClass(Villager.class).stream()
                .filter(villager -> barrierManager.isInBarrier(villager.getLocation(), center))
                .collect(java.util.stream.Collectors.toList()));

            // Add creepers in the barrier
            potentialTargets.addAll(center.getWorld().getEntitiesByClass(Creeper.class).stream()
                .filter(creeper -> barrierManager.isInBarrier(creeper.getLocation(), center))
                .collect(java.util.stream.Collectors.toList()));

            if (!potentialTargets.isEmpty()) {
                // Select a random target from the combined list
                Entity target = potentialTargets.get(new java.util.Random().nextInt(potentialTargets.size()));
                
                // Use a real lightning strike for gameplay effects
                target.getWorld().strikeLightning(target.getLocation());
            }
        });
    }
}
