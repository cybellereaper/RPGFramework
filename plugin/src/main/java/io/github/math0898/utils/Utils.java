package io.github.math0898.utils;

import io.github.math0898.rpgframework.RPGFramework;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Substitutes for the main JavaPlugin class when concerned with utils classes and objects.
 *
 * @author Sugaku
 */
public class Utils { // todo: Add loggers and logging styles system. (I want to be able to send to ConsoleSender using ChatColors and a predefined prefix per system.)

    /**
     * Reference to the plugin that this Utils package is added to.
     * -- GETTER --
     *  Accessor method to get the plugin in use.
     *
     * @return The active plugin.
     */
    @Getter
    private static final JavaPlugin plugin = RPGFramework.getPlugin();

    /**
     * This method sends a message to the console assuming the message to simply be informational.
     *
     * @param message The message to send to the console.
     */
    public static void console (String message) {
        console(message, ChatColor.GRAY, Level.INFO);
    }

    /**
     * This method sends a message to the console and infers the level it should be sent at.
     *
     * @param message The message to send to the console.
     * @param color   The main color of the message being sent.
     */
    public static void console (String message, ChatColor color) {
        switch (color) {
            case RED -> console(message, color, Level.SEVERE);
            case YELLOW -> console(message, color, Level.WARNING);
            default -> console(message, color, Level.INFO);
        }
    }

    /**
     * This method sends a message to the console and infers the level it should be sent at.
     *
     * @param message The message to send to the console.
     * @param lvl     The level that the message should be sent at.
     */
    public static void console (String message, Level lvl) {
        if (lvl.equals(Level.SEVERE)) console(message, ChatColor.RED, lvl);
        else if (lvl.equals(Level.WARNING)) console(message, ChatColor.YELLOW, lvl);
        else console(message, ChatColor.GRAY, Level.INFO);
    }

    /**
     * This method sends a message to the console.
     *
     * @param message The message to send to the console.
     * @param color   The main color of the message being sent.
     * @param lvl     The level that the message should be sent at.
     */
    public static void console (String message, ChatColor color, Level lvl) {
        plugin.getLogger().log(lvl, color + message);
    }

    /**
     * This is the standard disclaimer for players who are using beta features. Includes a message to console to log
     * that beta actions are being taken place.
     *
     * @param p The player who is being sent the warning.
     */
    public static void beta (Player p) {
        p.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + "This system is currently in development. We are not responsible for lost items or future buffs/nerfs.");
        console("Player: " + p.getName() + ". Is using beta things.", ChatColor.YELLOW);
    }
}
