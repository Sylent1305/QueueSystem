package net.sylent1305.queuePlus;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class Config {
    private String targetServer = "pokemon"; // default value

    // Timer defaults identical to current values in QueuePlus.java
    private int pointsIncrementSeconds = 1;
    private int connectFirstPlayerSeconds = 5;
    private int sendPriorityMessagesSeconds = 30;

    public Config(File configFile) {
        if (!configFile.exists()) {
            saveDefaultConfig(configFile);
        }

        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(fis);

            if (data != null) {
                if (data.containsKey("target-server")) {
                    targetServer = (String) data.get("target-server");
                }
                if (data.containsKey("points-increment-seconds")) {
                    pointsIncrementSeconds = (Integer) data.get("points-increment-seconds");
                }
                if (data.containsKey("connect-first-player-seconds")) {
                    connectFirstPlayerSeconds = (Integer) data.get("connect-first-player-seconds");
                }
                if (data.containsKey("send-priority-messages-seconds")) {
                    sendPriorityMessagesSeconds = (Integer) data.get("send-priority-messages-seconds");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();
            String defaultContent = "target-server: pokemon\n"
                    + "points-increment-seconds: 1\n"
                    + "connect-first-player-seconds: 5\n"
                    + "send-priority-messages-seconds: 30\n";
            java.nio.file.Files.writeString(configFile.toPath(), defaultContent);
        } catch (IOException e) {
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
}