package city.emerald.bastion;

import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class WorldListener implements Listener {

    private final Bastion plugin;
    private final VillageManager villageManager;

    public WorldListener(Bastion plugin, VillageManager villageManager) {
        this.plugin = plugin;
        this.villageManager = villageManager;
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        // This event fires once when the server is fully loaded.
        // We get the primary overworld and attempt to set the spawn.
        
        World mainWorld = plugin.getServer().getWorlds().get(0);

        if (mainWorld != null && mainWorld.getEnvironment() == World.Environment.NORMAL) {
            plugin.getLogger().info("Server loaded. Searching for a village to set the world spawn...");

            // Use a short delay to ensure all chunks and entities are fully loaded and ready for the search.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                org.bukkit.Location villageLocation = villageManager.findVillage(mainWorld, mainWorld.getSpawnLocation());
                if (villageLocation != null && villageManager.selectVillage(villageLocation)) {
                    plugin.getLogger().info("Village found and world spawn has been set automatically.");
                } else {
                    plugin.getLogger().warning("Could not automatically find a suitable village. World spawn not set. An admin may need to run /bastion findvillage manually.");
                }
            }, 20L); // Delay for 1 second (20 ticks)
        }
    }
}
