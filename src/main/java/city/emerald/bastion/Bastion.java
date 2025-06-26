package city.emerald.bastion;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Bastion extends JavaPlugin implements Listener {

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
  }

  @Override
  public void onDisable() {
    // Plugin shutdown logic
  }

  @EventHandler
  public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
    event.getPlayer().sendMessage("Welcome to the Bastion plugin!");
  }
}
