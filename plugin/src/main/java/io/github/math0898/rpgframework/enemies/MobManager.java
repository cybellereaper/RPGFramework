package io.github.math0898.rpgframework.enemies;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.math0898.rpgframework.RPGFramework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.math0898.rpgframework.RPGFramework.console;

/**
 * The MobManager is called upon to defined CustomMobs and load their stats from files during loading.
 *
 * @author Sugaku
 */
public class MobManager {

    /**
     * The active MobManager instance.
     */
    private static MobManager instance = null;

    /**
     * A map of the mobs by their namespace name.
     */
    private static final Map<String, CustomMobEntry> customMobMap = new HashMap<>();

    /**
     * Gets the active MobManager instance.
     *
     * @return The active MobManager instance.
     */
    public static MobManager getInstance () {
        if (instance == null) instance = new MobManager();
        return instance;
    }

    /**
     * Creates a new MobManager instance by loading and registering custom mobs.
     */
    private MobManager () {
        File itemsDir = new File("./plugins/RPGFramework/mobs/");
        if (!itemsDir.exists()) {
            if (!itemsDir.mkdirs()) {
                console("Failed to create item directories.", ChatColor.YELLOW);
                return;
            }
        }
        for (String itemResources : new String[]{"mobs/boss.yml"})
            RPGFramework.getPlugin().saveResource(itemResources, true); // todo: refactor to reduce scope when adding multiple bosses and sets.
        File[] files = itemsDir.listFiles();
        if (files == null) console("Cannot find any custom mob files.", ChatColor.YELLOW);
        else parseFiles(files);
    }

    /**
     * A utility method to convert a given file name and item name into a camel space namespace string.
     *
     * @param key      The key in the result.
     * @param fileName The namespace in the beginning.
     * @return The resulting namespace key.
     */
    private String toCamelSpaceNamespace (String fileName, String key) {
        String toReturn = fileName.replace(".yml", "").replace(".yaml", "") + ":";
        char[] tmp = key.toCharArray();
        tmp[0] = Character.toUpperCase(tmp[0]);
        for (int i = 1; i < tmp.length; i++)
            if (tmp[i] == '-') {
                if (i < tmp.length - 1)
                    tmp[i + 1] = Character.toUpperCase(tmp[i + 1]);
                i++;
            }
        return toReturn + new String(tmp).replace("-", "");
    }

    /**
     * Accessor method for all the items in the MobManager.
     *
     * @return The list of mobs registered with the MobManager.
     */
    public List<String> getCustomMobNameList () {
        return new ArrayList<>(customMobMap.keySet());
    }

    /**
     * Accessor method for mob by the given name.
     *
     * @param name The name of the mob to get.
     * @return The CustomMob associated with the given name.
     */
    public CustomMobEntry getCustomMob (String name) {
        return customMobMap.get(name);
    }

    /**
     * Checks whether a mob by the given name exists.
     *
     * @param name The name of the mob to check for.
     * @return True if the mob exists.
     */
    public boolean hasMob (String name) {
        return customMobMap.containsKey(name);
    }

    /**
     * Parses all the files given and the items contained.
     *
     * @param files The files to parse.
     */
    public void parseFiles (File[] files) {
        assert files != null;
        for (File f : files) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(f);
                for (String k : yaml.getKeys(false)) {
                    CustomMobEntry mob = null;
                    String goodName = toCamelSpaceNamespace(f.getName(), k);
                    try {
                        mob = new CustomMobEntry(yaml.getConfigurationSection(k), goodName); // todo: We'll need custom implementations of CustomMob based on k.
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    if (mob != null) {
                        customMobMap.put(goodName, mob);
                        console("Registered mob by name: " + goodName, ChatColor.GRAY);
                    } else console("Failed to parse: " + goodName + " in: " + f.getPath(), ChatColor.RED);
                }
            } catch (InvalidConfigurationException | IOException e) {
                console(e.getMessage(), ChatColor.RED);
                console("Failed to parse mob located at: " + f.getPath(), ChatColor.RED);
            }
        }
    }

    /**
     * Called back to when a Mob in the MobManager dies.
     *
     * @param location     The location that the mob died at.
     * @param namespaceKey The namespace key of the mob who died.
     */
    public void reportCustomMobDeath (Location location, String namespaceKey) {
        CustomMobEntry entry = customMobMap.get(namespaceKey);
        if (entry == null) return;
        System.out.println("Death of " + namespaceKey + " at " + location + " reported!");
        entry.handleDeath(location);
    }
}
