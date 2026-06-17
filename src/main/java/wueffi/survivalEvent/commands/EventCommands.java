package wueffi.survivalEvent.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import wueffi.survivalEvent.utils.LocationHandler;
import wueffi.survivalEvent.utils.PlaytimeManager;

import java.util.List;
import java.util.stream.Collectors;

public final class EventCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "playtime" -> handlePlaytime(sender);
            case "check" -> handleCheck(sender, args);
            case "start" -> handleStart(sender);
            case "end" -> handleEnd(sender);
            default -> false;
        };
    }

    private boolean handlePlaytime(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /playtime. Use /check <player> instead.");
            return true;
        }
        long seconds = PlaytimeManager.getSecondsToday(player);
        player.sendMessage("§aYour playtime: §f" + formatSeconds(seconds));
        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /check <player>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer '" + args[0] + "' is not online.");
            return true;
        }
        long seconds = PlaytimeManager.getSecondsToday(target);
        sender.sendMessage("§e" + target.getName() + "§f's playtime: §a" + formatSeconds(seconds));
        return true;
    }

    private boolean handleStart(CommandSender sender) {
        Player player = ((Player) sender);
        long seconds = PlaytimeManager.getSecondsToday(player.getUniqueId());
        if (seconds >= 7200) {
            sender.sendMessage("§cYou have already used up your time for today. Come back tomorrow!");
            return true;
        }
        sender.sendMessage("§eYou have §a" + formatSeconds(seconds) + "§e used today!");

        Location spawnLocation = LocationHandler.loadLocation(player);
        if (spawnLocation == null) {
            World world = Bukkit.getWorld("world2");

            if (world == null) {
                return false;
            }

            player.teleport(world.getSpawnLocation());
        } else {
            player.teleport(spawnLocation);
        }

        return true;
    }

    private boolean handleEnd(CommandSender sender) {
        Player player = ((Player) sender);
        long seconds = PlaytimeManager.getSecondsToday(player.getUniqueId());
        sender.sendMessage("§eYou have §a" + formatSeconds(seconds) + "§e used today!");

        LocationHandler.saveLocation(player);

        World world = Bukkit.getWorld("world");

        if (world == null) {
            return false;
        }

        player.teleport(world.getSpawnLocation());

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("check") || args.length != 1) return List.of();
        String partial = args[0].toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partial))
                .collect(Collectors.toList());
    }

    public static String formatSeconds(long total) {
        return String.format("%dh %02dm %02ds", total / 3600, (total % 3600) / 60, total % 60);
    }
}
