package city.emerald.bastion.wave;

import org.bukkit.scheduler.BukkitTask;

import city.emerald.bastion.BarrierManager;
import city.emerald.bastion.Bastion;

public class LightningManager {

    private final Bastion plugin;
    private final WaveManager waveManager;
    private final BarrierManager barrierManager;
    private BukkitTask lightningTask;

    public LightningManager(Bastion plugin, WaveManager waveManager, BarrierManager barrierManager) {
        this.plugin = plugin;
        this.waveManager = waveManager;
        this.barrierManager = barrierManager;
    }

    /**
     * Starts the lightning strike task.
     * This should be called at the beginning of a boss wave.
     */
    public void start() {
        // TODO: Implement logic to start the repeating Bukkit task
    }

    /**
     * Stops the lightning strike task.
     * This should be called at the end of a boss wave.
     */
    public void stop() {
        // TODO: Implement logic to stop the Bukkit task
    }

    /**
     * The core logic that finds a target and strikes it with lightning.
     * This will be called by the repeating Bukkit task.
     */
    private void strikeRandomTarget() {
        // TODO: Implement logic to find and strike a random target
    }
}
