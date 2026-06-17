package wueffi.survivalEvent.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class LocationListener implements Listener {

    private final JavaPlugin plugin;

    public LocationListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().getName().equals("world")) return;

        World world2 = Bukkit.getWorld("world2");
        if (world2 == null) return;

        event.setRespawnLocation(world2.getSpawnLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        LocationHandler.saveLocation(event.getPlayer());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (!event.getPlugin().equals(plugin)) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            LocationHandler.saveLocation(player);
        }
    }
}