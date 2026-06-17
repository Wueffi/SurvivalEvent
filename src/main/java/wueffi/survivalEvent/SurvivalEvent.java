package wueffi.survivalEvent;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wueffi.survivalEvent.commands.EventCommands;
import wueffi.survivalEvent.utils.*;

import java.util.List;

public final class SurvivalEvent extends JavaPlugin {
    public Logger LOGGER = LoggerFactory.getLogger("SurvivalEvent");

    @Override
    public void onEnable() {
        LOGGER.info("Starting up SurvivalEvent");

        if (WorldSetup.setup()) LOGGER.info("Succesfully set up worlds!");
        else LOGGER.info("Failed to set up worlds!");

        ContainerHandler.init(this);
        Bukkit.getPluginManager().registerEvents(new ContainerListener(), this);
        LOGGER.info("ContainerHandler initialized!");

        PlaytimeManager.init(this);
        LOGGER.info("PlaytimeManager initialized!");

        PlaytimeScoreboard.init(this);
        LOGGER.info("PlaytimeScoreboard initialized!");

        ItemReportTask.init(this);
        LOGGER.info("ItemReportTask initialized!");

        LocationHandler.init(this);
        Bukkit.getPluginManager().registerEvents(new LocationListener(this), this);
        LOGGER.info("LocationHandler initialized!");

        EventCommands handler = new EventCommands();
        for (String cmd : List.of("playtime", "check", "start", "end")) {
            var pluginCmd = getCommand(cmd);
            pluginCmd.setExecutor(handler);
            pluginCmd.setTabCompleter(handler);
        }
        LOGGER.info("Commands registered");
    }

    @Override
    public void onDisable() {
        if (WorldSetup.unload()) LOGGER.info("Succesfully unloaded worlds!");
        else LOGGER.info("Failed to unload worlds!");

        PlaytimeManager.shutdown();
        LOGGER.info("PlaytimeManager shutdown!");

        PlaytimeScoreboard.shutdown();
        LOGGER.info("PlaytimeScoreBoard shutdown!");

        ItemReportTask.shutdown();
        LOGGER.info("ItemReportTask shutdown!");
    }
}
