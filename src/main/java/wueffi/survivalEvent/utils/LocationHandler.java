package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class LocationHandler {

    private static final String CONFIG_FILE_NAME = "locations.yml";
    private static JavaPlugin plugin;
    private static File configFile;
    private static FileConfiguration config;

    private LocationHandler() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;

        configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create locations.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public static void saveLocation(Player player) {
        Location loc = player.getLocation();
        String path = "locations." + player.getUniqueId();

        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", (double) loc.getYaw());
        config.set(path + ".pitch", (double) loc.getPitch());

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save location for " + player.getName() + ": " + e.getMessage());
        }
    }

    public static Location loadLocation(Player player) {
        config = YamlConfiguration.loadConfiguration(configFile);

        String path = "locations." + player.getUniqueId();
        if (!config.contains(path)) return null;

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");

        return new Location(Bukkit.getWorld("world2"), x, y, z, yaw, pitch);
    }
}