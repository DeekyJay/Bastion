package city.emerald.bastion;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    private final Bastion plugin;
    private final VillageManager villageManager;
    private boolean spawnHasBeenSet = false;

    public WorldListener(Bastion plugin, VillageManager villageManager) {
        this.plugin = plugin;
        this.villageManager = villageManager;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        // Only run this once for the main overworld and if spawn hasn't been set
        if (spawnHasBeenSet || event.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return;
        }

        // Check if this is the server's primary world, which is usually the first one loaded
        if (plugin.getServer().getWorlds().get(0).equals(event.getWorld())) {
            plugin.getLogger().info("Primary overworld loaded. Searching for a village to set the world spawn...");
            
            // Use a Bukkit task to delay the search slightly, ensuring chunks and entities are ready
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (villageManager.findAndSelectVillage(event.getWorld())) {
                    plugin.getLogger().info("Village found and world spawn has been set automatically.");
                } else {
                    plugin.getLogger().warning("Could not automatically find a suitable village. World spawn not set. An admin may need to run /bastion findvillage manually.");
                }
                // Mark as checked so we don't run this again for other worlds
                spawnHasBeenSet = true; 
            }, 20L); // Delay for 1 second (20 ticks)
        }
    }
}
