package net.sylent1305.queuePlus;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class QueuePlusCommand implements SimpleCommand {

    private final QueuePlus plugin;
    private final ProxyServer proxy;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<String, SimpleCommand> subCommands = new HashMap<>();

    public QueuePlusCommand(QueuePlus plugin, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.proxy = proxyServer;

        // Register subcommands
        subCommands.put("reload", new ReloadSubCommand());
        subCommands.put("checkpoints", new CheckPointsSubCommand());
        subCommands.put("join", new JoinQueueSubCommand());
        subCommands.put("leave", new LeaveQueueSubCommand());
        subCommands.put("info", new QueueInfoSubCommand());
        subCommands.put("setpoints", new SetPointsSubCommand());
        subCommands.put("addpoints", new AddPointsSubCommand());
        subCommands.put("removepoints", new RemovePointsSubCommand());
    }

    @Override
    public void execute(SimpleCommand.Invocation invocation) {
        List<String> args = Arrays.asList(invocation.arguments());

        if (args.isEmpty()) {
            invocation.source().sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.usage")));
            return;
        }

        String subCmd = args.get(0).toLowerCase();
        SimpleCommand subCommand = subCommands.get(subCmd);

        if (subCommand == null) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("command", subCmd);
            invocation.source().sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.unknown-command", replacements)));
            return;
        }

        String[] subArgs = args.size() > 1 ? args.subList(1, args.size()).toArray(new String[0]) : new String[0];

        subCommand.execute(new SimpleCommand.Invocation() {
            @Override
            public CommandSource source() {
                return invocation.source();
            }

            @Override
            public String[] arguments() {
                return subArgs;
            }

            @Override
            public String alias() {
                return invocation.alias();
            }
        });
    }

    @Override
    public List<String> suggest(SimpleCommand.Invocation invocation) {
        List<String> args = Arrays.asList(invocation.arguments());

        if (args.isEmpty()) {
            return new ArrayList<>(subCommands.keySet());
        }

        if (args.size() == 1) {
            String partial = args.get(0).toLowerCase();
            List<String> matches = new ArrayList<>();
            for (String key : subCommands.keySet()) {
                if (key.startsWith(partial)) {
                    matches.add(key);
                }
            }
            return matches;
        }

        String subCmd = args.get(0).toLowerCase();
        SimpleCommand subCommand = subCommands.get(subCmd);

        if (subCommand != null) {
            String[] subArgs = args.size() > 1 ? args.subList(1, args.size()).toArray(new String[0]) : new String[0];
            return subCommand.suggest(new SimpleCommand.Invocation() {
                @Override
                public CommandSource source() {
                    return invocation.source();
                }

                @Override
                public String[] arguments() {
                    return subArgs;
                }

                @Override
                public String alias() {
                    return invocation.alias();
                }
            });
        }

        return Collections.emptyList();
    }

    // Subcommand implementations

    private class ReloadSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            if (!source.hasPermission("queueplus.reload")) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.no-permission")));
                return;
            }
            plugin.reloadConfig();
            Map<String, String> replacements = new HashMap<>();
            replacements.put("server", plugin.getTargetServer());
            source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.config.reloaded", replacements)));
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            return Collections.emptyList();
        }
    }

    private class CheckPointsSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            if (args.length < 1) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.usage")));
                return;
            }

            String playerName = args[0];
            Optional<Player> targetOpt = proxy.getPlayer(playerName);
            if (targetOpt.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.player-not-found")));
                return;
            }
            Player target = targetOpt.get();
            int totalPoints = plugin.getTotalPoints(target.getUniqueId());
            int queuePoints = plugin.getQueuePoints(target.getUniqueId());
            int bonusPoints = plugin.getBonusPoints(target.getUniqueId());
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("player", playerName);
            replacements.put("total", String.valueOf(totalPoints));
            replacements.put("queue", String.valueOf(queuePoints));
            replacements.put("bonus", String.valueOf(bonusPoints));
            source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.player-points-info", replacements)));
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                List<String> names = new ArrayList<>();
                for (Player p : proxy.getAllPlayers()) {
                    if (p.getUsername().toLowerCase().startsWith(partial)) {
                        names.add(p.getUsername());
                    }
                }
                return names;
            }
            return Collections.emptyList();
        }
    }

    private class JoinQueueSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player player)) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.player-only")));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.already-in-queue")));
                return;
            }
            plugin.sortedQueue.add(playerUUID);
            plugin.queuePoints.put(playerUUID, 0);
            player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.joined-queue")));
            plugin.sendQueueStatus(player);
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            return Collections.emptyList();
        }
    }

    private class LeaveQueueSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player player)) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.player-only")));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (!plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.not-in-queue")));
                return;
            }
            plugin.sortedQueue.remove(playerUUID);
            plugin.queuePoints.remove(playerUUID);
            player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.left-queue")));
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            return Collections.emptyList();
        }
    }

    private class QueueInfoSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player player)) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.player-only")));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (!plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.not-in-queue")));
                return;
            }
            int pos = plugin.sortedQueue.indexOf(playerUUID) + 1;
            int queuePts = plugin.getQueuePoints(playerUUID);
            int bonusPts = plugin.getBonusPoints(playerUUID);
            int totalPts = plugin.getTotalPoints(playerUUID);
            
            Map<String, String> replacements = new HashMap<>();
            replacements.put("position", String.valueOf(pos));
            replacements.put("queue", String.valueOf(queuePts));
            replacements.put("bonus", String.valueOf(bonusPts));
            replacements.put("total", String.valueOf(totalPts));
            player.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.player-position-info", replacements)));
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            return Collections.emptyList();
        }
    }

    private abstract class PointsSubCommandBase implements SimpleCommand {
        abstract int modifyPoints(int currentPoints, int modValue);

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();
            if (args.length < 2) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.usage")));
                return;
            }

            String playerName = args[0];
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.amount-must-be-number")));
                return;
            }

            Optional<Player> targetOpt = proxy.getPlayer(playerName);
            if (targetOpt.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.commands.player-not-found")));
                return;
            }

            Player target = targetOpt.get();
            UUID targetUUID = target.getUniqueId();
            int currentPoints = plugin.getBonusPoints(targetUUID);
            int newPoints = modifyPoints(currentPoints, amount);
            plugin.setBonusPoints(targetUUID, newPoints);
            
            // Re-sort the queue if the player is in it
            if (plugin.sortedQueue.contains(targetUUID)) {
                // Remove the player from their current position
                plugin.sortedQueue.remove(targetUUID);
                
                // Find the correct position based on total points
                int insertIndex = 0;
                for (UUID uuid : plugin.sortedQueue) {
                    int otherPoints = plugin.getTotalPoints(uuid);
                    if (plugin.getTotalPoints(targetUUID) > otherPoints) {
                        break;
                    }
                    insertIndex++;
                }
                
                // Insert the player at the correct position
                plugin.sortedQueue.add(insertIndex, targetUUID);
                
                // Notify the player of their new position
                int newPosition = insertIndex + 1;
                Map<String, String> replacements = new HashMap<>();
                replacements.put("points", String.valueOf(newPoints));
                replacements.put("position", String.valueOf(newPosition));
                target.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.points-updated", replacements)));
            } else {
                Map<String, String> replacements = new HashMap<>();
                replacements.put("player", playerName);
                replacements.put("points", String.valueOf(newPoints));
                source.sendMessage(miniMessage.deserialize(plugin.pluginConfig.getMessage("messages.queue-actions.player-points-updated", replacements)));
            }
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 1) {
                String partial = args[0].toLowerCase();
                List<String> names = new ArrayList<>();
                for (Player p : proxy.getAllPlayers()) {
                    if (p.getUsername().toLowerCase().startsWith(partial)) {
                        names.add(p.getUsername());
                    }
                }
                return names;
            }
            return Collections.emptyList();
        }
    }

    private class SetPointsSubCommand extends PointsSubCommandBase {
        @Override
        int modifyPoints(int currentPoints, int modValue) {
            return modValue;
        }
    }

    private class AddPointsSubCommand extends PointsSubCommandBase {
        @Override
        int modifyPoints(int currentPoints, int modValue) {
            return currentPoints + modValue;
        }
    }

    private class RemovePointsSubCommand extends PointsSubCommandBase {
        @Override
        int modifyPoints(int currentPoints, int modValue) {
            return Math.max(0, currentPoints - modValue);
        }
    }
}