package io.github.math0898.rpgframework.dungeons;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.github.math0898.rpgframework.RPGFramework.console;

/**
 * The dungeon manager handles moving players to and from their dungeons. This includes win detection and dungeon
 * generation among many other things.
 *
 * @author Sugaku
 */
public class DungeonManager {

    /**
     * The active DungeonManager instance.
     */
    private static DungeonManager dungeonManager;

    /**
     * A list of DungeonTilesets that are known to this DungeonMaster.
     */
    private final List<DungeonTileset> dungeonTilesets = new ArrayList<>();

    /**
     * A list of active dungeons.
     */
    private final List<Dungeon> dungeons = new ArrayList<>();

    /**
     * Creates a new DungeonManager object.
     */
    private DungeonManager () {
        File tilesetDir = new File("./plugins/RPGFramework/tilesets/");
        if (!tilesetDir.exists()) {
            if (!tilesetDir.mkdirs()) {
                console("Failed to create tilesets directories.", ChatColor.YELLOW);
                return;
            }
        }
        for (String itemResources : new String[]{ "tilesets/TestTileset.yml" })
            RPGFramework.getPlugin().saveResource(itemResources, true); // todo: refactor to reduce scope when adding multiple tilesets.
        File[] files = tilesetDir.listFiles();
        if (files == null) console("Cannot find any tileset files.", ChatColor.YELLOW);
        for (File f : files)
            dungeonTilesets.add(new DungeonTileset(f.getAbsolutePath()));
    }

    /**
     * Accessor method for the DungeonManager singleton instance.
     */
    public static DungeonManager getInstance () {
        if (dungeonManager == null) dungeonManager = new DungeonManager();
        return dungeonManager;
    }

    /**
     * Sends the given player, and their party, to a new dungeon that is in the process of being built.
     *
     * @param rpgPlayer The player starting a dungeon to play.
     */
    public void joinDungeon (RpgPlayer rpgPlayer) {
        List<RpgPlayer> players = new ArrayList<>();
        if (rpgPlayer.getParty() != null)
            players.addAll(rpgPlayer.getParty().getRpgPlayers()); // The passed player should be part of this collection.
        else players.add(rpgPlayer); // todo: Check that none of these players are currently in an existing dungeon.

        DungeonTileset randomTileset = dungeonTilesets.get(new Random().nextInt(0, dungeonTilesets.size()));
        Dungeon dungeon = new Dungeon(players, new Location(Bukkit.getWorld("world"), -574, 65, -485), randomTileset); // todo: Create and algorithm to actually determine starting location.
        dungeons.add(dungeon);
        RPGFramework.console("Dungeon Manager: Created Dungeon", ChatColor.AQUA);
    }
}
