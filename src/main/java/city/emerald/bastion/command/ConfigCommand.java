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
        if (!sender.hasPermission("bastion.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§cUsage: /bastionconfig <reload|set|get> [key] [value]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aConfiguration reloaded.");
                break;
            case "set":
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /bastionconfig set <key> <value>");
                    return true;
                }
                String key = args[1];
                // Rebuild the value string if it contains spaces
                String value = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                plugin.getConfig().set(key, value);
                plugin.saveConfig();
                sender.sendMessage("§aConfiguration saved: " + key + " = " + value);
                break;
            case "get":
                sender.sendMessage("§aFetching configuration value...");
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /bastionconfig get <key>");
                    return true;
                }
                String getKey = args[1];
                plugin.getLogger().info("[DEBUG] GET command received for key: '" + getKey + "'");

                Object getValue = plugin.getConfig().get(getKey);

                if (getValue != null) {
                    plugin.getLogger().info("[DEBUG] Found value: '" + getValue.toString() + "' of type: " + getValue.getClass().getName());
                    sender.sendMessage("§a" + getKey + ": §f" + formatConfigValue(getValue));
                } else {
                    plugin.getLogger().warning("[DEBUG] Key not found: '" + getKey + "'");
                    sender.sendMessage("§cKey not found: " + getKey);
                }
                break;
            default:
                sender.sendMessage("§cUnknown subcommand. Use 'reload', 'set', or 'get'.");
                break;
        }

        return true;
    }

    private String formatConfigValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof List) {
            List<?> list = (List<?>) value;
            return "[" + list.stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(", ")) + "]";
        } else if (value instanceof org.bukkit.configuration.ConfigurationSection) {
            org.bukkit.configuration.ConfigurationSection section = (org.bukkit.configuration.ConfigurationSection) value;
            return "{" + section.getKeys(false).stream()
                .map(key -> key + ": " + formatConfigValue(section.get(key)))
                .collect(java.util.stream.Collectors.joining(", ")) + "}";
        } else {
            return value.toString();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.stream.Stream.of("reload", "set", "get")
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("get"))) {
            // Return all keys, including nested ones, for tab completion
            return plugin.getConfig().getKeys(true).stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}
