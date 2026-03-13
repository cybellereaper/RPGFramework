package io.github.math0898.rpgframework.dungeons;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * A dungeon is an active Dungeon with players running through it. This object listens to events to progress the dungeon,
 * determine winning conditions, and losing conditions.
 *
 * @author Sugaku
 */
public class Dungeon implements Listener {

    // Listen for deaths
    // Spawn mobs/bosses
    // Unlock doors
    // IN_FUTURE: apply boons

    /**
     * The maximum number of rooms for this dungeon.
     */
    private static final int MAX_ROOMS = 25;

    /**
     * The probability and given door leads to a dead end instead of another room. This is of course ignored when we
     * reach the maximum number of rooms.
     */
    private static final double DEAD_END_PROBABILITY = 0.2;

    /**
     * The length to make a straight path for to the final room.
     */
    private static final int GOLDEN_PATH_LENGTH = 4;

    /**
     * Players that are part of this active dungeon.
     */
    private final List<RpgPlayer> players;

    /**
     * The tileset of this dungeon. Used to generate enemies and rooms.
     */
    private final DungeonTileset tileset;


    /**
     * The location that this dungeon begins from.
     */
    private final Location originPoint;

    /**
     * Creates a new dungeon with the given tileset and players.
     *
     * @param players     The players that are part of this dungeon.
     * @param originPoint The starting point to generate this dungeon from.
     * @param tileset     The tileset used to generate this dungeon.
     */
    public Dungeon (List<RpgPlayer> players, Location originPoint, DungeonTileset tileset) {
        this.players = new ArrayList<>(players);
        this.tileset = tileset;
        this.originPoint = originPoint;
        Bukkit.getPluginManager().registerEvents(this, RPGFramework.getPlugin());
        Bukkit.getScheduler().runTaskAsynchronously(RPGFramework.getPlugin(), () -> {
            this.tileset.createStartingRoom(this.originPoint); // todo: This method requires some synchronous actions to complete. It may not be safe to teleport players.
            Bukkit.getScheduler().runTask(RPGFramework.getPlugin(), this::teleportParticipants);
            this.tileset.generate(this.originPoint, GOLDEN_PATH_LENGTH, DEAD_END_PROBABILITY, MAX_ROOMS);
            unlockDungeon();
        });
    }

    /**
     * Teleports dungeon participants to the starting room.
     */
    public void teleportParticipants () {
        for (RpgPlayer p : players)
            p.getBukkitPlayer().teleport(originPoint);
        RPGFramework.console("Dungeon: Teleported Participants", ChatColor.AQUA);
    }

    /**
     * Unlocks the dungeon and allows players to begin progressing through it.
     */
    public void unlockDungeon () {
        RPGFramework.console("Dungeon: Dungeon Unlocked", ChatColor.DARK_GREEN);
        // todo: Implement
    }
}
