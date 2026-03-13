package io.github.math0898.rpgframework.commands;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.utils.commands.BetterCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.InputStreamReader;
import java.util.*;

/**
 * The Update command helps to notify and allows players to view recent updates.
 *
 * @author Sugaku
 */
public class Updates extends BetterCommand {

    /**
     * The Update record simply helps to store update data in a singular object.
     *
     * @param date  The date that the update was released.
     * @param title The title that the update went by.
     * @param msg   The message associated with this update.
     */
    private record Update (String title, String date, List<String> msg) { }

    /**
     * The update sections that players may be interested.
     */
    private final Map<String, Update> updates = new HashMap<>();

    /**
     * Creates a new BetterCommand with the given name.
     */
    public Updates () {
        super("updates");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(RPGFramework.getPlugin().getResource("updates.yml"))));
        for (String key : data.getKeys(false))
            updates.put(key, new Update(data.getString(key + ".title"), data.getString(key + ".date"), data.getStringList(key + ".message")));
    }

    /**
     * Called whenever specifically a player executes this command.
     *
     * @param player The player who ran this command.
     * @param args   The arguments they passed to the command.
     */
    @Override
    public boolean onPlayerCommand (Player player, String[] args) {
       return onNonPlayerCommand(player, args);
    }

    /**
     * Called whenever an unspecified sender executes this command. This could include console and command blocks.
     *
     * @param sender The sender who ran this command.
     * @param args   The arguments they passed to the command.
     */
    @Override
    public boolean onNonPlayerCommand (CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Please select a specific update.");
            return true;
        }
        Update update = updates.get(args[0]);
        if (update == null) {
            sender.sendMessage(ChatColor.RED + "We could not find that update.");
            return true;
        }
        sender.sendMessage(ChatColor.BOLD + " ====== " + update.title() + " ====== ");
        for (String s : update.msg())
            sender.sendMessage(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));
        sender.sendMessage(ChatColor.BOLD + " ====== " + update.date() + " ====== ");
        return true;
    }

    /**
     * Called whenever a command sender is trying to tab complete a command.
     *
     * @param sender The sender who is tab completing this command.
     * @param args   The current arguments they have typed.
     */
    @Override
    public List<String> simplifiedTab (CommandSender sender, String[] args) {
        if (args.length >= 2) return new ArrayList<>();
        List<String> options = new ArrayList<>(updates.keySet());
        options = everythingStartsWith(options, args[args.length - 1]);
        return options;
    }
}
