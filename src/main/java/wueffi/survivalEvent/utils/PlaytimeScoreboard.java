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

import java.util.*;

public final class PlaytimeScoreboard {
    private static final long TICK_INTERVAL = 20L;
    private static final int SNAPSHOT_INTERVAL = 300;
    private static final int VIEW_COUNT = 3;

    private static JavaPlugin plugin;
    private static BukkitTask updateTask;

    private static int tickCounter = 0;
    private static int viewIndex = 0;
    private static Map<String, Integer> globalItemTotals = new LinkedHashMap<>();

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

            tickCounter++;
            if (tickCounter == 1 || tickCounter % SNAPSHOT_INTERVAL == 0) {
                refreshSnapshot(world);
            }
            if (tickCounter > 1 && tickCounter % 5 == 0) {
                viewIndex = (viewIndex + 1) % VIEW_COUNT;
            }

            for (Player player : world.getPlayers()) {
                update(player);
            }
        }, TICK_INTERVAL, TICK_INTERVAL);
    }

    private static void refreshSnapshot(World world) {
        globalItemTotals = ItemReportTask.scanWorld(world);
    }

    public static void update(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board.equals(Bukkit.getScoreboardManager().getMainScoreboard())) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }

        new ArrayList<>(board.getObjectives()).forEach(Objective::unregister);

        Component title = switch (viewIndex) {
            case 0 -> Component.text("Leaderboard").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
            case 1 -> Component.text("Top Items").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD);
            case 2 -> Component.text("Rare Items").color(NamedTextColor.RED).decorate(TextDecoration.BOLD);
            default -> Component.text("Survival Event").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
        };

        Objective obj = board.registerNewObjective("playtime", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        long secondsLeft = Math.max(0L, 7200 - PlaytimeManager.getSecondsToday(player));
        double pts = PlayerPointsStore.get(player.getUniqueId());

        obj.getScore(" ").setScore(0);
        obj.getScore("§a" + EventCommands.formatSeconds(secondsLeft)).setScore(1);
        obj.getScore("§7Time left:").setScore(2);
        obj.getScore("  ").setScore(3);
        obj.getScore("§e" + pts + " pts").setScore(4);
        obj.getScore("§7Your points:").setScore(5);
        obj.getScore("§8──────────────").setScore(6);

        switch (viewIndex) {
            case 0 -> buildLeaderboard(obj, 7);
            case 1 -> buildMostCollected(obj, 7);
            case 2 -> buildLeastCollected(obj, 7);
        }

        player.setScoreboard(board);
    }

    private static void buildLeaderboard(Objective obj, int base) {
        List<Map.Entry<UUID, Double>> top = PlayerPointsStore.getTopN(5);
        int count = top.size();

        String[] prefix = {"§6#1 ", "§7#2 ", "§c#3 ", "§f#4 ", "§f#5 "};

        for (int i = 0; i < count; i++) {
            Map.Entry<UUID, Double> e = top.get(i);
            String name = PlayerPointsStore.getName(e.getKey());
            obj.getScore(prefix[i] + "§f" + name + " §e" + String.format("%.2f", e.getValue())).setScore(base + count - 1 - i);
        }
    }

    private static void buildMostCollected(Objective obj, int base) {
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(globalItemTotals.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        int count = Math.min(sorted.size(), 5);

        for (int i = 0; i < count; i++) {
            Map.Entry<String, Integer> e = sorted.get(i);
            obj.getScore("§b#" + (i + 1) + " " + e.getKey() + "§7: §f" + e.getValue()).setScore(base + count - 1 - i);
        }
    }

    private static void buildLeastCollected(Objective obj, int base) {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(globalItemTotals.entrySet());
        entries.removeIf(e -> e.getValue() < 1);
        entries.sort(Map.Entry.comparingByValue());

        int count = Math.min(entries.size(), 5);
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Integer> e = entries.get(i);
            obj.getScore("§c#" + (i + 1) + " " + e.getKey() + "§7: §f" + e.getValue()).setScore(base + count - 1 - i);
        }
    }
}