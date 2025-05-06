package net.sylent1305.queuePlus;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private String targetServer = "pokemon"; // default value

    // Timer defaults identical to current values in QueuePlus.java
    private int pointsIncrementSeconds = 1;
    private int connectFirstPlayerSeconds = 5;
    private int sendPriorityMessagesSeconds = 30;

    // Message storage
    private final Map<String, String> messages = new HashMap<>();

    public Config(File configFile) {
        if (!configFile.exists()) {
            saveDefaultConfig(configFile);
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(fis);

            if (data != null) {
                // Load basic settings
                if (data.containsKey("target-server")) {
                    targetServer = (String) data.get("target-server");
                }
                if (data.containsKey("points-increment-seconds")) {
                    Object value = data.get("points-increment-seconds");
                    if (value instanceof Integer) {
                        pointsIncrementSeconds = (Integer) value;
                    } else if (value instanceof String) {
                        try {
                            pointsIncrementSeconds = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid points-increment-seconds value in config, using default: " + pointsIncrementSeconds);
                        }
                    }
                }
                if (data.containsKey("connect-first-player-seconds")) {
                    Object value = data.get("connect-first-player-seconds");
                    if (value instanceof Integer) {
                        connectFirstPlayerSeconds = (Integer) value;
                    } else if (value instanceof String) {
                        try {
                            connectFirstPlayerSeconds = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid connect-first-player-seconds value in config, using default: " + connectFirstPlayerSeconds);
                        }
                    }
                }
                if (data.containsKey("send-priority-messages-seconds")) {
                    Object value = data.get("send-priority-messages-seconds");
                    if (value instanceof Integer) {
                        sendPriorityMessagesSeconds = (Integer) value;
                    } else if (value instanceof String) {
                        try {
                            sendPriorityMessagesSeconds = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid send-priority-messages-seconds value in config, using default: " + sendPriorityMessagesSeconds);
                        }
                    }
                }

                // Load messages
                if (data.containsKey("messages")) {
                    Map<String, Object> messagesData = (Map<String, Object>) data.get("messages");
                    loadMessages(messagesData, "");
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMessages(Map<String, Object> data, String prefix) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map) {
                loadMessages((Map<String, Object>) value, key);
            } else if (value instanceof String) {
                messages.put(key, (String) value);
            }
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            String defaultContent = "# QueuePlus Configuration File\n" +
                    "# This file contains all settings for the QueuePlus plugin\n\n" +
                    "# Server Settings\n" +
                    "# The name of the server players will be queued for\n" +
                    "target-server: pokemon\n\n" +
                    "# Timer Settings\n" +
                    "# How often (in seconds) players in queue receive points\n" +
                    "points-increment-seconds: 1\n" +
                    "# How often (in seconds) the plugin checks if the first player can connect\n" +
                    "connect-first-player-seconds: 5\n" +
                    "# How often (in seconds) players receive their queue status\n" +
                    "send-priority-messages-seconds: 30\n\n" +
                    "# Message Settings\n" +
                    "# All messages support MiniMessage formatting (https://docs.advntr.dev/minimessage/format.html)\n" +
                    "# Available colors: <red>, <green>, <blue>, <yellow>, <gold>, <gray>, <white>\n" +
                    "# Available styles: <bold>, <italic>, <underlined>, <strikethrough>\n\n" +
                    "messages:\n" +
                    "  # Queue Status Messages\n" +
                    "  queue-status:\n" +
                    "    header: \"<yellow>=== Queue Status ===</yellow>\"\n" +
                    "    queue-points: \"<green>Queue Points: <gold>{points}</gold></green>\"\n" +
                    "    bonus-points: \"<green>Bonus Points: <gold>{points}</gold></green>\"\n" +
                    "    total-points: \"<green>Total Points: <gold>{points}</gold></green>\"\n" +
                    "    position: \"<yellow>Your position in queue: <gold>{position}</gold></yellow>\"\n" +
                    "    player-entry: \"<yellow>#{position}</yellow> <white>{username}</white> <gray>({points} points)</gray>\"\n" +
                    "    current-player-prefix: \"<gold>→</gold> \"\n\n" +
                    "  # Server Connection Messages\n" +
                    "  server-connection:\n" +
                    "    success: \"<green>Successfully connected to the server!</green>\"\n" +
                    "    server-full: \"<red>The server is currently full. Please wait in queue...</red>\"\n\n" +
                    "  # Command Messages\n" +
                    "  commands:\n" +
                    "    usage: \"<red>Usage: /queueplus <subcommand> [args]</red>\"\n" +
                    "    unknown-command: \"<red>Unknown command: {command}</red>\"\n" +
                    "    no-permission: \"<red>You don't have permission to use this command.</red>\"\n" +
                    "    player-only: \"<red>Only players can use this command.</red>\"\n" +
                    "    player-not-found: \"<red>Player not found or not connected.</red>\"\n" +
                    "    amount-must-be-number: \"<red>Amount must be a number.</red>\"\n\n" +
                    "  # Queue Action Messages\n" +
                    "  queue-actions:\n" +
                    "    already-in-queue: \"<red>You are already in the queue.</red>\"\n" +
                    "    not-in-queue: \"<red>You are not in the queue.</red>\"\n" +
                    "    joined-queue: \"<green>You have joined the queue.</green>\"\n" +
                    "    left-queue: \"<green>You have left the queue.</green>\"\n" +
                    "    points-updated: \"<green>Your bonus points have been updated to <gold>{points}</gold>. Your new position in queue: <gold>{position}</gold></green>\"\n" +
                    "    player-points-updated: \"<green>{player}'s bonus points have been updated to <gold>{points}</gold>.</green>\"\n" +
                    "    player-points-info: \"<green>Player {player} has <gold>{total}</gold> points (<gold>{queue}</gold> queue + <gold>{bonus}</gold> bonus).</green>\"\n\n" +
                    "  # Configuration Messages\n" +
                    "  config:\n" +
                    "    reloaded: \"<green>Configuration reloaded! Target server: {server}</green>\"\n";
            java.nio.file.Files.writeString(configFile.toPath(), defaultContent);
        } catch (IOException e) {
            System.err.println("Failed to save default config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getTargetServer() {
        return targetServer;
    }

    public int getPointsIncrementSeconds() {
        return pointsIncrementSeconds;
    }

    public int getConnectFirstPlayerSeconds() {
        return connectFirstPlayerSeconds;
    }

    public int getSendPriorityMessagesSeconds() {
        return sendPriorityMessagesSeconds;
    }

    public String getMessage(String path) {
        return messages.getOrDefault(path, "");
    }

    public String getMessage(String path, Map<String, String> replacements) {
        String message = getMessage(path);
        if (message.isEmpty()) return "";
        
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }
}