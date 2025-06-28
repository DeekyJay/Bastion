package city.emerald.bastion.command;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import city.emerald.bastion.Bastion;

public class ConfigCommand implements CommandExecutor, TabCompleter {

    private final Bastion plugin;

    public ConfigCommand(Bastion plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Command logic will go here
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Tab completion logic will go here
        return null;
    }
}
