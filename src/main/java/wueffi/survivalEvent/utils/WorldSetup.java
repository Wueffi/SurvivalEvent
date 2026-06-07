package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

public class WorldSetup {
    public static boolean setup() {
        World world = Bukkit.getWorld("world2");

        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator("world2"));
        }

        if (world == null) {
            return false;
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(world.getSpawnLocation().x(), world.getSpawnLocation().y());
        border.setSize(500);
        return true;
    }

    public static boolean unload() {
        World world2 = Bukkit.getWorld("world2");

        if (world2 == null) {
            return false;
        }

        World world = Bukkit.getWorld("world2");

        if (world == null) {
            return false;
        }

        for (Player player : world.getPlayers()) {
            player.teleport(world.getSpawnLocation());
        }
        return true;
    }
}
