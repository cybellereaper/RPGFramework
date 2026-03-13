package io.github.math0898.rpgframework;

import io.github.math0898.rpgframework.classes.Classes;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.github.math0898.utils.Utils.console;

public final class DataManager {

    private static final String DATA_VERSION = "2.1";
    private static final String RPG_DIRECTORY = "./plugins/RPG";
    private static final String PLAYER_DATA_DIRECTORY = RPG_DIRECTORY + "/PlayerData";

    private static final String KEY_VERSION = "version";
    private static final String KEY_CLASS = "class";
    private static final String KEY_EXPERIENCE = "experience";
    private static final String KEY_ARTIFACTS = "artifacts";

    private static DataManager instance;

    private final Path rpgDirectory;
    private final Path playerDataDirectory;

    private DataManager() {
        this.rpgDirectory = Path.of(RPG_DIRECTORY);
        this.playerDataDirectory = Path.of(PLAYER_DATA_DIRECTORY);
        ensureRequiredDirectoriesExist();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void load(RpgPlayer player) {
        if (player == null) {
            return;
        }

        console("Loading player data for " + player.getName() + ".");

        File playerFile = getPlayerFile(player);
        if (!playerFile.exists()) {
            console("Data not found.", ChatColor.YELLOW);
            return;
        }

        try {
            YamlConfiguration yaml = loadYaml(playerFile);
            PlayerData playerData = readPlayerData(yaml);

            logLoadedData(playerData);
            applyPlayerData(player, playerData);

            console("Loaded.", ChatColor.GREEN);
        } catch (Exception exception) {
            console("Failed to load data for " + player.getName() + ": " + exception.getMessage(), ChatColor.RED);
        }
    }

    public void save(RpgPlayer player) {
        if (player == null) {
            return;
        }

        console("Saving data for " + player.getName() + ".");

        PlayerData playerData = PlayerData.from(player);
        logSavedData(playerData);

        YamlConfiguration yaml = new YamlConfiguration();
        writePlayerData(yaml, playerData);

        try {
            yaml.save(getPlayerFile(player));
            console("Data saved!", ChatColor.GREEN);
        } catch (IOException exception) {
            console("Failed to save data for " + player.getName() + ": " + exception.getMessage(), ChatColor.RED);
        }
    }

    private void ensureRequiredDirectoriesExist() {
        console("Checking for required directories.");

        if (!createDirectoryIfMissing(rpgDirectory, "Top level directory created.", "Could not create top level directory.")) {
            return;
        }

        if (!createDirectoryIfMissing(
                playerDataDirectory,
                "Directory for storing player data created.",
                "Could not create directory to hold player data."
        )) {
            return;
        }

        console("Required directories found or created.", ChatColor.GREEN);
    }

    private boolean createDirectoryIfMissing(Path directory, String successMessage, String failureMessage) {
        if (Files.exists(directory)) {
            return true;
        }

        try {
            Files.createDirectory(directory);
            console(successMessage);
            return true;
        } catch (IOException exception) {
            console(failureMessage, ChatColor.RED);
            return false;
        }
    }

    private File getPlayerFile(RpgPlayer player) {
        return playerDataDirectory.resolve(player.getUuid().toString()).toFile();
    }

    private YamlConfiguration loadYaml(File file) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file);
        return yaml;
    }

    private PlayerData readPlayerData(YamlConfiguration yaml) {
        String version = yaml.getString(KEY_VERSION);
        Classes playerClass = Classes.fromString(yaml.getString(KEY_CLASS, Classes.NONE.name()));
        long experience = yaml.getLong(KEY_EXPERIENCE, 0L);
        List<String> artifacts = yaml.getStringList(KEY_ARTIFACTS);

        return new PlayerData(version, playerClass, experience, artifacts);
    }

    private void writePlayerData(YamlConfiguration yaml, PlayerData playerData) {
        yaml.set(KEY_VERSION, playerData.version());
        yaml.set(KEY_CLASS, playerData.playerClass().name());
        yaml.set(KEY_EXPERIENCE, playerData.experience());
        yaml.set(KEY_ARTIFACTS, playerData.artifacts());
    }

    private void applyPlayerData(RpgPlayer player, PlayerData playerData) {
        player.joinClass(playerData.playerClass());
        player.addCollectedArtifacts(playerData.artifacts());
        player.setExperience(playerData.experience());
    }

    private void logLoadedData(PlayerData playerData) {
        console("File version: " + playerData.version());
        console("Class: " + playerData.playerClass().name());
        console("Experience: " + playerData.experience());
        logArtifacts(playerData.artifacts());
    }

    private void logSavedData(PlayerData playerData) {
        console("Version: " + playerData.version());
        console("Class: " + playerData.playerClass().name());
        console("Experience: " + playerData.experience());
        logArtifacts(playerData.artifacts());
    }

    private void logArtifacts(List<String> artifacts) {
        console("Artifacts:");
        for (String artifact : artifacts) {
            console(" > " + artifact);
        }
    }

    private record PlayerData(
            String version,
            Classes playerClass,
            long experience,
            List<String> artifacts
    ) {
        private static PlayerData from(RpgPlayer player) {
            return new PlayerData(
                    DATA_VERSION,
                    player.getCombatClass(),
                    player.getExperience(),
                    List.copyOf(player.getArtifactCollection())
            );
        }
    }
}
