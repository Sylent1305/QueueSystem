package net.sylent1305.queuePlus;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;
import java.util.*;

@Plugin(id = "queueplus", name = "QueuePlus", version = "1.0", description = "Points based queue plugin")
public class QueuePlus {

    private final ProxyServer proxyServer;
    private final Logger logger;
    public Config pluginConfig;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final BonusPointsStorage bonusPointsStorage;

    final Map<UUID, Integer> queuePoints = new HashMap<>();
    final List<UUID> sortedQueue = new ArrayList<>();

    @Inject
    public QueuePlus(ProxyServer proxyServer, Logger logger) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        File pluginFolder = new File("plugins/queueplus");
        this.pluginConfig = new Config(new File(pluginFolder, "config.yaml"));
        this.bonusPointsStorage = new BonusPointsStorage(pluginFolder);
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

    public int getTotalPoints(UUID playerUUID) {
        return queuePoints.getOrDefault(playerUUID, 0) + bonusPointsStorage.getBonusPoints(playerUUID);
    }

    public int getQueuePoints(UUID playerUUID) {
        return queuePoints.getOrDefault(playerUUID, 0);
    }

    public int getBonusPoints(UUID playerUUID) {
        return bonusPointsStorage.getBonusPoints(playerUUID);
    }

    public void setBonusPoints(UUID playerUUID, int points) {
        bonusPointsStorage.setBonusPoints(playerUUID, points);
    }

    public void addBonusPoints(UUID playerUUID, int points) {
        bonusPointsStorage.addBonusPoints(playerUUID, points);
    }

    public void removeBonusPoints(UUID playerUUID, int points) {
        bonusPointsStorage.removeBonusPoints(playerUUID, points);
    }

    public void reloadConfig() {
        pluginConfig = new Config(new File("plugins/queueplus/config.yaml"));
        logger.info("Reloaded config, targetServer: " + pluginConfig.getTargetServer());
    }

    public void sendQueueStatus(Player player) {
        int queuePoints = getQueuePoints(player.getUniqueId());
        int bonusPoints = getBonusPoints(player.getUniqueId());
        int totalPoints = getTotalPoints(player.getUniqueId());
        
        Map<String, String> replacements = new HashMap<>();
        replacements.put("points", String.valueOf(queuePoints));
        player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.queue-points", replacements)));
        
        replacements.put("points", String.valueOf(bonusPoints));
        player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.bonus-points", replacements)));
        
        replacements.put("points", String.valueOf(totalPoints));
        player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.total-points", replacements)));
    }

    private void startPointsIncrementTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            for (UUID playerId : sortedQueue) {
                int currentPoints = queuePoints.getOrDefault(playerId, 0);
                queuePoints.put(playerId, currentPoints + 1);
                
                Optional<Player> playerOpt = proxyServer.getPlayer(playerId);
                playerOpt.ifPresent(player -> {
                    if (currentPoints % 10 == 0) { // Show queue status every 10 points
                        StringBuilder queueStatus = new StringBuilder();
                        queueStatus.append(pluginConfig.getMessage("messages.queue-status.header")).append("\n");
                        
                        // Sort players by total points
                        List<UUID> sortedByPoints = new ArrayList<>(sortedQueue);
                        sortedByPoints.sort((uuid1, uuid2) -> {
                            int points1 = getTotalPoints(uuid1);
                            int points2 = getTotalPoints(uuid2);
                            return Integer.compare(points2, points1); // Descending order
                        });
                        
                        // Show top 5 players
                        for (int i = 0; i < Math.min(5, sortedByPoints.size()); i++) {
                            UUID uuid = sortedByPoints.get(i);
                            Optional<Player> p = proxyServer.getPlayer(uuid);
                            p.ifPresent(p2 -> {
                                int totalPoints = getTotalPoints(uuid);
                                int position = sortedQueue.indexOf(uuid) + 1;
                                String prefix = uuid.equals(playerId) ? pluginConfig.getMessage("messages.queue-status.current-player-prefix") : "";
                                
                                Map<String, String> replacements = new HashMap<>();
                                replacements.put("position", String.valueOf(position));
                                replacements.put("username", p2.getUsername());
                                replacements.put("points", String.valueOf(totalPoints));
                                
                                queueStatus.append(prefix)
                                        .append(pluginConfig.getMessage("messages.queue-status.player-entry", replacements))
                                        .append("\n");
                            });
                        }
                        
                        player.sendMessage(miniMessage.deserialize(queueStatus.toString()));
                    }
                });
            }
        }).repeat(Duration.ofSeconds(pluginConfig.getPointsIncrementSeconds())).schedule();
    }

    private void startConnectFirstPlayerTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            if (!sortedQueue.isEmpty()) {
                // Sort queue by total points
                sortedQueue.sort((uuid1, uuid2) -> {
                    int points1 = getTotalPoints(uuid1);
                    int points2 = getTotalPoints(uuid2);
                    return Integer.compare(points2, points1); // Descending order
                });

                UUID firstPlayerId = sortedQueue.get(0);
                Optional<Player> playerOpt = proxyServer.getPlayer(firstPlayerId);
                playerOpt.ifPresent(player -> {
                    proxyServer.getServer(pluginConfig.getTargetServer()).ifPresent(server -> {
                        server.ping().thenAccept(serverPing -> {
                            if (serverPing.getPlayers().isPresent()) {
                                int currentPlayers = serverPing.getPlayers().get().getOnline();
                                int maxPlayers = serverPing.getPlayers().get().getMax();
                                
                                if (currentPlayers < maxPlayers) {
                                    player.createConnectionRequest(server).fireAndForget();
                                    sortedQueue.remove(firstPlayerId);
                                    queuePoints.remove(firstPlayerId);
                                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.server-connection.success")));
                                } else {
                                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.server-connection.server-full")));
                                }
                            }
                        });
                    });
                });
            }
        }).repeat(Duration.ofSeconds(pluginConfig.getConnectFirstPlayerSeconds())).schedule();
    }

    private void startSendPriorityMessagesTask() {
        proxyServer.getScheduler().buildTask(this, () -> {
            for (UUID playerId : sortedQueue) {
                Optional<Player> playerOpt = proxyServer.getPlayer(playerId);
                playerOpt.ifPresent(player -> {
                    int position = sortedQueue.indexOf(playerId) + 1;
                    int queuePts = getQueuePoints(playerId);
                    int bonusPts = getBonusPoints(playerId);
                    int totalPts = getTotalPoints(playerId);
                    
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("position", String.valueOf(position));
                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.position", replacements)));
                    
                    replacements.put("points", String.valueOf(queuePts));
                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.queue-points", replacements)));
                    
                    replacements.put("points", String.valueOf(bonusPts));
                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.bonus-points", replacements)));
                    
                    replacements.put("points", String.valueOf(totalPts));
                    player.sendMessage(miniMessage.deserialize(pluginConfig.getMessage("messages.queue-status.total-points", replacements)));
                });
            }
        }).repeat(Duration.ofSeconds(pluginConfig.getSendPriorityMessagesSeconds())).schedule();
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        queuePoints.remove(playerId);
        sortedQueue.remove(playerId);
    }
}