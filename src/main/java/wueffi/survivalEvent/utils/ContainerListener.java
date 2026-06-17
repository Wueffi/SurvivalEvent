package wueffi.survivalEvent.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class ContainerListener implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof Container container)) return;

        Player player = event.getPlayer();

        container.customName(Component.text(player.getName()));
        container.update();

        ContainerHandler.addContainer(player.getUniqueId(), block.getLocation());
        player.sendMessage("§aYou claimed this container!");
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Container)) return;

        ContainerHandler.removeContainer(block.getLocation());
    }
}