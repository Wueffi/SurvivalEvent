package wueffi.survivalEvent.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerPointsStore {

    private static final Map<UUID, String> names = new LinkedHashMap<>();
    private static final Map<UUID, Double> points = new LinkedHashMap<>();

    private static JavaPlugin plugin;
    private static File dataFile;

    private PlayerPointsStore() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;

        dataFile = new File(plugin.getDataFolder(), "player_points.yml");
        plugin.getDataFolder().mkdirs();

        load();
    }

    public static void shutdown() {
        save();
    }

    public static void set(UUID uuid, String name, double pts) {
        names.put(uuid, name);
        points.put(uuid, pts);
    }

    public static double get(UUID uuid) {
        return points.getOrDefault(uuid, 0.0);
    }

    public static String getName(UUID uuid) {
        return names.getOrDefault(uuid, "?");
    }

    public static List<Map.Entry<UUID, Double>> getTopN(int n) {
        List<Map.Entry<UUID, Double>> sorted = new ArrayList<>(points.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    public static void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (UUID uuid : points.keySet()) {
            String path = uuid.toString();
            yaml.set(path + ".name", names.get(uuid));
            yaml.set(path + ".points", points.get(uuid));
        }

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player_points.yml: " + e.getMessage());
        }
    }

    private static void load() {
        if (!dataFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);

        for (String key : yaml.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                names.put(uuid, yaml.getString(key + ".name", "?"));
                points.put(uuid, yaml.getDouble(key + ".points", 0.0));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}