package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class ItemReportTask {
    private static final long INTERVAL_TICKS = 20L * 300;
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LinkedHashMap<String, Material> TRACKED = new LinkedHashMap<>();

    static {
        TRACKED.put("Amethyst Shard", Material.AMETHYST_SHARD);
        TRACKED.put("Coal", Material.COAL);
        TRACKED.put("Copper Ingot", Material.COPPER_INGOT);
        TRACKED.put("Diamond", Material.DIAMOND);
        TRACKED.put("Emerald", Material.EMERALD);
        TRACKED.put("Gold Ingot", Material.GOLD_INGOT);
        TRACKED.put("Iron Ingot", Material.IRON_INGOT);
        TRACKED.put("Lapis Lazuli", Material.LAPIS_LAZULI);
        TRACKED.put("Netherite Ingot", Material.NETHERITE_INGOT);
        TRACKED.put("Prismarine Crystals", Material.PRISMARINE_CRYSTALS);
        TRACKED.put("Quartz", Material.QUARTZ);
        TRACKED.put("Redstone", Material.REDSTONE);
        TRACKED.put("Glowstone Dust", Material.GLOWSTONE_DUST);
        TRACKED.put("Resin Brick", Material.RESIN_BRICK);
    }

    private static JavaPlugin plugin;
    private static BukkitTask task;
    private static File csvFile;
    private static final AtomicInteger idCounter = new AtomicInteger(1);

    private ItemReportTask() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        csvFile = new File(plugin.getDataFolder(), "item_report.csv");

        if (!csvFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                csvFile.createNewFile();
                writeHeader();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create item_report.csv: " + e.getMessage());
            }
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, ItemReportTask::run, INTERVAL_TICKS, INTERVAL_TICKS);
    }

    public static void shutdown() {
        if (task == null) return;

        task.cancel();
        task = null;
    }

    private static void run() {
        World world = Bukkit.getWorld("world2");
        if (world == null) return;

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FMT);
        Map<UUID, Map<String, Integer>> playerCounts = new LinkedHashMap<>();

        for (Player player : world.getPlayers()) {
            Map<String, Integer> counts = zeroCounts();
            addCounts(player.getInventory(), counts);

            for (Location loc : ContainerHandler.getContainersPerPlayer(player.getUniqueId())) {
                Block block = loc.getBlock();
                if (!(block.getState() instanceof Container container)) continue;
                addCounts(container.getInventory(), counts);
            }
            playerCounts.put(player.getUniqueId(), counts);
        }

        Map<String, Map<UUID, Integer>> itemMatrix = new LinkedHashMap<>();

        for (String key : TRACKED.keySet()) {
            Map<UUID, Integer> row = new LinkedHashMap<>();
            for (Map.Entry<UUID, Map<String, Integer>> e : playerCounts.entrySet()) {
                row.put(e.getKey(), e.getValue().get(key));
            }
            itemMatrix.put(key, row);
        }

        Map<UUID, Double> scores = calculateScores(itemMatrix);

        for (Player player : world.getPlayers()) {
            UUID uuid = player.getUniqueId();
            double pts = scores.getOrDefault(uuid, 0.0);

            Map<String, Integer> counts = playerCounts.get(uuid);
            PlayerPointsStore.set(uuid, player.getName(), pts);

            writeRow(idCounter.getAndIncrement(), player.getName(), timestamp, pts, counts);
        }

        PlayerPointsStore.save();
    }

    private static Map<UUID, Double> calculateScores(Map<String, Map<UUID, Integer>> items) {
        Map<UUID, Double> scores = new LinkedHashMap<>();

        for (Map.Entry<String, Map<UUID, Integer>> itemEntry : items.entrySet()) {
            Map<UUID, Integer> playerAmounts = itemEntry.getValue();
            double S_k = playerAmounts.values().stream().mapToDouble(Integer::doubleValue).sum();

            if (S_k == 0) continue;

            double weightedTotal = Math.pow(S_k, 0.5);

            for (Map.Entry<UUID, Integer> e : playerAmounts.entrySet()) {
                double w_i_k = e.getValue() / S_k;
                scores.merge(e.getKey(), w_i_k * weightedTotal, Double::sum);
            }
        }
        return scores;
    }

    static Map<String, Integer> scanWorld(World world) {
        Map<String, Integer> totals = zeroCounts();

        for (Player player : world.getPlayers()) {
            addCounts(player.getInventory(), totals);

            for (Location loc : ContainerHandler.getContainersPerPlayer(player.getUniqueId())) {
                Block block = loc.getBlock();

                if (!(block.getState() instanceof Container container)) continue;

                addCounts(container.getInventory(), totals);
            }
        }
        return totals;
    }

    private static Map<String, Integer> zeroCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (String key : TRACKED.keySet()) counts.put(key, 0);
        return counts;
    }

    private static void addCounts(Inventory inv, Map<String, Integer> counts) {
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;

            for (Map.Entry<String, Material> entry : TRACKED.entrySet()) {
                if (entry.getValue() != item.getType()) continue;
                counts.merge(entry.getKey(), item.getAmount(), Integer::sum);
            }
        }
    }

    private static void writeHeader() throws IOException {
        try (FileWriter fw = new FileWriter(csvFile, true)) {
            fw.write("id,playername,timestamp,points," + String.join(",", TRACKED.keySet()) + "\n");
        }
    }

    private static void writeRow(int id, String name, String timestamp, double points, Map<String, Integer> counts) {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(",").append(name).append(",").append(timestamp).append(",").append(points);

        for (int count : counts.values()) sb.append(",").append(count);

        sb.append("\n");

        try (FileWriter fw = new FileWriter(csvFile, true)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            plugin.getLogger().severe("Could not write to item_report.csv: " + e.getMessage());
        }
    }
}