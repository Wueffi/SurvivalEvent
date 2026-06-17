package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlaytimeManager {
    private static final long TICK_INTERVAL = 20L; // 1 Second
    private static final long SAVE_INTERVAL_TICKS = 300 * TICK_INTERVAL; // 5 Mins
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String CONFIG_FILE_NAME = "playtime.yml";
    private static JavaPlugin plugin;
    private static final Map<UUID, Long> secondsToday = new HashMap<>();
    private static final Map<UUID, String> playerDates  = new HashMap<>();
    private static BukkitTask tickTask;
    private static BukkitTask saveTask;
    private static File configFile;
    private static FileConfiguration config;

    private PlaytimeManager() {}

    public static void init(JavaPlugin javaPlugin) {
        plugin = javaPlugin;

        loadConfig();
        startTickTask();
        startSaveTask();
    }

    public static void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }

        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }

        saveConfig();
    }

    public static long getSecondsToday(UUID uuid) {
        checkAndResetIfNewDay(uuid);
        return secondsToday.getOrDefault(uuid, 0L);
    }

    public static long getSecondsToday(Player player) {
        return getSecondsToday(player.getUniqueId());
    }

    private static void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String todayStr = today();
            World world = Bukkit.getWorld("world2");

            if (world == null) {
                return;
            }

            for (Player player : world.getPlayers()) {
                UUID uuid = player.getUniqueId();

                String lastDate = playerDates.getOrDefault(uuid, todayStr);
                if (!lastDate.equals(todayStr)) {
                    secondsToday.put(uuid, 0L);
                }

                playerDates.put(uuid, todayStr);
                secondsToday.merge(uuid, 1L, Long::sum);
                if (secondsToday.get(uuid) >= 7200) {
                    World spawnWorld = Bukkit.getWorld("world");

                    if (spawnWorld == null) {
                        return;
                    }

                    player.teleport(spawnWorld.getSpawnLocation());
                    player.sendMessage("§cYou have used all of your playtime for today! Come back tommorow");
                }
            }
        }, TICK_INTERVAL, TICK_INTERVAL);
    }

    private static void startSaveTask() {
        saveTask = Bukkit.getScheduler().runTaskTimer(
                plugin, PlaytimeManager::saveConfig,
                SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS
        );
    }

    private static void checkAndResetIfNewDay(UUID uuid) {
        String todayStr = today();
        String lastDate = playerDates.getOrDefault(uuid, todayStr);

        if (!lastDate.equals(todayStr)) {
            secondsToday.put(uuid, 0L);
            playerDates.put(uuid, todayStr);
        }
    }

    private static String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    private static void loadConfig() {
        configFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);

        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create playtime.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.isConfigurationSection("players")) return;

        for (String uuidStr : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String path = "players." + uuidStr;

                String date = config.getString(path + ".date", today());
                long seconds = config.getLong(path + ".seconds", 0L);

                if (date.equals(today())) {
                    secondsToday.put(uuid, seconds);
                    playerDates.put(uuid, date);
                } else {
                    secondsToday.put(uuid, 0L);
                    playerDates.put(uuid, today());
                }

            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    static void saveConfig() {
        if (config == null || configFile == null) return;

        for (Map.Entry<UUID, Long> entry : secondsToday.entrySet()) {
            String uuid = entry.getKey().toString();
            String path = "players." + uuid;

            config.set(path + ".date", playerDates.getOrDefault(entry.getKey(), today()));
            config.set(path + ".seconds", entry.getValue());
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save playtime.yml: " + e.getMessage());
        }
    }
}
