package net.sylent1305.queuePlus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BonusPointsStorage {
    private final File storageFile;
    private final Gson gson;
    private Map<UUID, Integer> bonusPoints;

    public BonusPointsStorage(File pluginFolder) {
        this.storageFile = new File(pluginFolder, "bonus_points.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.bonusPoints = new HashMap<>();
        loadBonusPoints();
    }

    private void loadBonusPoints() {
        if (!storageFile.exists()) {
            saveBonusPoints();
            return;
        }

        try (FileReader reader = new FileReader(storageFile)) {
            bonusPoints = gson.fromJson(reader, new TypeToken<HashMap<UUID, Integer>>(){}.getType());
            if (bonusPoints == null) {
                bonusPoints = new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("Failed to load bonus points: " + e.getMessage());
            bonusPoints = new HashMap<>();
        }
    }

    private void saveBonusPoints() {
        try {
            storageFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(storageFile)) {
                gson.toJson(bonusPoints, writer);
            }
        } catch (IOException e) {
            System.err.println("Failed to save bonus points: " + e.getMessage());
        }
    }

    public int getBonusPoints(UUID playerId) {
        return bonusPoints.getOrDefault(playerId, 0);
    }

    public void setBonusPoints(UUID playerId, int points) {
        bonusPoints.put(playerId, points);
        saveBonusPoints();
    }

    public void addBonusPoints(UUID playerId, int points) {
        int current = getBonusPoints(playerId);
        setBonusPoints(playerId, current + points);
    }

    public void removeBonusPoints(UUID playerId, int points) {
        int current = getBonusPoints(playerId);
        setBonusPoints(playerId, Math.max(0, current - points));
    }
} 