package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ContainerHandler {
    private static final String CONFIG_FILE_NAME = "containers.yml";
    private static JavaPlugin plugin;
    private static File configFile;
    private static FileConfiguration config;

    private ContainerHandler() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create containers.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void addContainer(UUID uuid, Location loc) {
        String key = "containers." + uuid;

        List<String> list = config.getStringList(key);
        String entry = toKey(loc);
        if (list.contains(entry)) return;

        list.add(entry);
        config.set(key, list);
        save();
    }

    public static void removeContainer(Location loc) {
        if (!config.isConfigurationSection("containers")) return;
        String entry = toKey(loc);

        for (String uuid : config.getConfigurationSection("containers").getKeys(false)) {
            String key = "containers." + uuid;

            List<String> list = config.getStringList(key);
            if (!list.remove(entry)) continue;

            config.set(key, list);
            save();
            return;
        }
    }

    public static List<Location> getContainersPerPlayer(UUID uuid) {
        if (config == null) return List.of();
        return config.getStringList("containers." + uuid).stream()
                .map(ContainerHandler::fromKey)
                .collect(Collectors.toList());
    }

    private static String toKey(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private static Location fromKey(String s) {
        String[] p = s.split(":");
        return new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
    }

    private static void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save containers.yml: " + e.getMessage());
        }
    }
}