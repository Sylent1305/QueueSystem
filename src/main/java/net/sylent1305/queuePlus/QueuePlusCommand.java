package net.sylent1305.queuePlus;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.*;

public class QueuePlusCommand implements SimpleCommand {

    private final QueuePlus plugin;
    private final ProxyServer proxy;

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
            invocation.source().sendMessage(Component.text("Usage: /queueplus <subcommand> [args]"));
            return;
        }

        String subCmd = args.get(0).toLowerCase();
        SimpleCommand subCommand = subCommands.get(subCmd);

        if (subCommand == null) {
            invocation.source().sendMessage(Component.text("Unknown subcommand: " + subCmd));
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

        return Collections.emptyList();
    }

    // Subcommand implementations

    private class ReloadSubCommand implements SimpleCommand {
        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            CommandSource source = invocation.source();
            if (!source.hasPermission("queueplus.reload")) {
                source.sendMessage(Component.text("You don't have permission to reload config."));
                return;
            }
            plugin.reloadConfig();
            source.sendMessage(Component.text("QueuePlus config reloaded! Target server: " + plugin.getTargetServer()));
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
                source.sendMessage(Component.text("Usage: /queueplus checkpoints <player>"));
                return;
            }

            String playerName = args[0];
            Optional<Player> targetOpt = proxy.getPlayer(playerName);
            if (targetOpt.isEmpty()) {
                source.sendMessage(Component.text("Player not found or not online."));
                return;
            }
            Player target = targetOpt.get();
            int points = plugin.getPlayerPoints(target.getUniqueId());
            source.sendMessage(Component.text("Player " + playerName + " has " + points + " points."));
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
                source.sendMessage(Component.text("Only players can join the queue."));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(Component.text("You are already in the queue."));
                return;
            }
            plugin.sortedQueue.add(playerUUID);
            plugin.startPoints.put(playerUUID, 0);
            plugin.playerPoints.put(playerUUID, 0);
            player.sendMessage(Component.text("You joined the queue."));
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
                source.sendMessage(Component.text("Only players can leave the queue."));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (!plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(Component.text("You are not in the queue."));
                return;
            }
            plugin.sortedQueue.remove(playerUUID);
            plugin.startPoints.remove(playerUUID);
            plugin.playerPoints.remove(playerUUID);
            player.sendMessage(Component.text("You left the queue."));
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
                source.sendMessage(Component.text("Only players can use this command."));
                return;
            }
            UUID playerUUID = player.getUniqueId();
            if (!plugin.sortedQueue.contains(playerUUID)) {
                player.sendMessage(Component.text("You are not in the queue."));
                return;
            }
            int pos = plugin.sortedQueue.indexOf(playerUUID) + 1;
            int points = plugin.playerPoints.getOrDefault(playerUUID, 0);
            player.sendMessage(Component.text("Your position in the queue: " + pos));
            player.sendMessage(Component.text("Your points: " + points));
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
            if (args.length != 2) {
                source.sendMessage(Component.text("Usage: /queueplus <setpoints|addpoints|removepoints> <player> <points>"));
                return;
            }

            String playerName = args[0];
            int pointsValue;
            try {
                pointsValue = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                source.sendMessage(Component.text("Points must be a valid number."));
                return;
            }

            Optional<Player> targetOpt = proxy.getPlayer(playerName);
            if (targetOpt.isEmpty()) {
                source.sendMessage(Component.text("Player not found or not online."));
                return;
            }

            UUID targetUuid = targetOpt.get().getUniqueId();
            int currentPoints = plugin.playerPoints.getOrDefault(targetUuid, 0);
            int newPoints = modifyPoints(currentPoints, pointsValue);
            plugin.playerPoints.put(targetUuid, newPoints);
            source.sendMessage(Component.text("Updated points for " + playerName + " to " + newPoints));
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
            return currentPoints - modValue;
        }
    }
}