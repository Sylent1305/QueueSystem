package net.sylent1305.queuePlus;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;
import java.util.*;

@Plugin(id = "queueplus", name = "QueuePlus", version = "1.0", description = "Points based queue plugin")
public class QueuePlus {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private Config pluginConfig;

    final Map<UUID, Integer> playerPoints = new HashMap<>();
    final Map<UUID, Integer> startPoints = new HashMap<>();
    final List<UUID> sortedQueue = new ArrayList<>();

    @Inject
    public QueuePlus(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.pluginConfig = new Config(new File("plugins/queueplus/config.yaml"));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        registerCommands();
        startPointsIncrementTask();
        startConnectFirstPlayerTask();
        startSendPriorityMessagesTask();
        logger.info("QueuePlus plugin initialized.");
    }

    private void registerCommands() {
        proxyServer.getCommandManager().register(
                proxyServer.getCommandManager().metaBuilder("queueplus").build(),
                new QueuePlusCommand(this, proxyServer)
        );
    }

    public String getTargetServer() {
        return pluginConfig.getTargetServer();
    }

    public int getPlayerPoints(UUID playerUUID) {
        return playerPoints.getOrDefault(playerUUID, 0);
    }

    public void reloadConfig() {
        pluginConfig = new Config(new File("plugins/queueplus/config.yaml"));
        logger.info("Reloaded config, targetServer: " + pluginConfig.getTargetServer());
    }

    public void sendQueueStatus(Player player) {
        int points = getPlayerPoints(player.getUniqueId());
        player.sendMessage(Component.text("You have " + points + " points").color(NamedTextColor.GREEN));
    }

    private void startPointsIncrementTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            // Increment points logic
        }).repeat(Duration.ofSeconds(pluginConfig.getPointsIncrementSeconds())).schedule();
    }

    private void startConnectFirstPlayerTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            // Connect first player logic (omitted)
        }).repeat(Duration.ofSeconds(pluginConfig.getConnectFirstPlayerSeconds())).schedule();
    }

    private void startSendPriorityMessagesTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            // Send priority messages logic (omitted)
        }).repeat(Duration.ofSeconds(pluginConfig.getSendPriorityMessagesSeconds())).schedule();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        playerPoints.remove(playerId);
        startPoints.remove(playerId);
        sortedQueue.remove(playerId);
    }

}