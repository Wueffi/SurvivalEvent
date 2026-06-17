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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ItemReportTask {
    private static final long INTERVAL_TICKS = 20L * 300;
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final LinkedHashMap<String, Material> TRACKED = new LinkedHashMap<>();

    static {
        TRACKED.put("wood", Material.OAK_LOG);
        TRACKED.put("stone", Material.COBBLESTONE);
        TRACKED.put("gold", Material.GOLD_INGOT);
        TRACKED.put("door", Material.OAK_DOOR);
        TRACKED.put("planks", Material.OAK_PLANKS);
        TRACKED.put("button", Material.OAK_BUTTON);
        TRACKED.put("fence", Material.OAK_FENCE);
        TRACKED.put("chest boat", Material.OAK_CHEST_BOAT);
        TRACKED.put("hanging sign", Material.OAK_HANGING_SIGN);
        TRACKED.put("boat", Material.OAK_BOAT);
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

        for (Player player : world.getPlayers()) {
            Map<String, Integer> counts = zeroCounts();
            addCounts(player.getInventory(), counts);

            for (Location loc : ContainerHandler.getContainersPerPlayer(player.getUniqueId())) {
                Block block = loc.getBlock();
                if (!(block.getState() instanceof Container container)) continue;
                addCounts(container.getInventory(), counts);
            }

            writeRow(idCounter.getAndIncrement(), player.getName(), timestamp, calculatePoints(), counts);
        }
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

    private static int calculatePoints() {
        return 67;
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

    static int getPlayerPoints(Player player) {
        return calculatePoints();
    }

    private static void writeHeader() throws IOException {
        try (FileWriter fw = new FileWriter(csvFile, true)) {
            fw.write("id,playername,timestamp,points," + String.join(",", TRACKED.keySet()) + "\n");
        }
    }

    private static void writeRow(int id, String name, String timestamp, int points, Map<String, Integer> counts) {
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