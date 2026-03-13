package io.github.math0898.rpgframework.dungeons;

import io.github.math0898.rpgframework.RPGFramework;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.github.math0898.rpgframework.RPGFramework.console;

/**
 * A DungeonTileset is a collection of tiles that can be used to generate a dungeon.
 *
 * @author Sugaku
 */
public class DungeonTileset {

    /**
     * A list of rooms by their name in the yaml file.
     */
    private final Map<String, Tile> tiles = new HashMap<>();

    /**
     * Creates a new DungeonTileset by reading from the given file.
     *
     * @param file The filepath to pull this Tileset data from.
     */
    public DungeonTileset (String file) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            for (String s : yaml.getKeys(false))
                tiles.put(s, new Tile(yaml.getConfigurationSection(s)));
        } catch (IOException | InvalidConfigurationException exception) {
            console("Failed to load " + file + " => " + exception.getMessage(), ChatColor.YELLOW);
        }
    }

    /**
     * Creates the starting room for this tileset at the given location.
     *
     * @param originPoint The starting point for this tileset.
     */
    public void createStartingRoom (Location originPoint) {
        RPGFramework.console("DungeonTileset: Created Starting Room", ChatColor.AQUA);
        Tile startingTile = tiles.get("starting-room");
        Bukkit.getScheduler().runTask( RPGFramework.getPlugin(),
                () -> startingTile.generate(originPoint));
    }

    /**
     * Generates a dungeon from this tileset with the given generation values.
     */
    public void generate (Location originPoint, int lengthToEnd, double deadEndProb, int maxRooms) {
        RPGFramework.console("DungeonTileset: Dungeon Generated", ChatColor.GREEN);
        // todo: Implement.
    }
}
