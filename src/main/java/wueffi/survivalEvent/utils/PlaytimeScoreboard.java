package wueffi.survivalEvent.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import wueffi.survivalEvent.commands.EventCommands;

public final class PlaytimeScoreboard {

    private static final long TICK_INTERVAL = 20L;

    private static JavaPlugin plugin;
    private static BukkitTask updateTask;

    private PlaytimeScoreboard() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        startUpdateTask();
    }

    public static void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    private static void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            World world = Bukkit.getWorld("world2");
            if (world == null) return;

            for (Player player : world.getPlayers()) {
                update(player);
            }
        }, TICK_INTERVAL, TICK_INTERVAL);
    }

    public static void update(Player player) {
        Scoreboard board = player.getScoreboard();
        for (Objective obj : board.getObjectives()) {
            obj.unregister();
        }

        Objective obj = board.registerNewObjective("playtime", Criteria.DUMMY, Component.text("Survival Event").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        long secondsLeft = Math.max(0L, 7200 - PlaytimeManager.getSecondsToday(player));

        obj.getScore(" ").setScore(3);
        obj.getScore("§7Time left today:").setScore(2);
        obj.getScore("§a" + EventCommands.formatSeconds(secondsLeft)).setScore(1);
        obj.getScore("  ").setScore(0);

        player.setScoreboard(board);
    }
}