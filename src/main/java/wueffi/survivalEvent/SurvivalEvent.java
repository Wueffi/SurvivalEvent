package wueffi.survivalEvent;

import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wueffi.survivalEvent.commands.EventCommands;
import wueffi.survivalEvent.utils.PlaytimeManager;
import wueffi.survivalEvent.utils.WorldSetup;

import java.util.List;

public final class SurvivalEvent extends JavaPlugin {
    public Logger LOGGER = LoggerFactory.getLogger("SurvivalEvent");

    @Override
    public void onEnable() {
        LOGGER.info("Starting up SurvivalEvent");

        if (WorldSetup.setup()) LOGGER.info("Succesfully set up worlds!");
        else LOGGER.info("Failed to set up worlds!");

        PlaytimeManager.init(this);
        LOGGER.info("PlaytimeManager initialized!");

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
    }
}
