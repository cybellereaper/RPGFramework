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
 * The tutorial class helps to inform players of the unique mechanics that are present in this server. As well as inform
 * existing players of ongoing updates and changes.
 *
 * @author Sugaku
 */
public class Tutorial extends BetterCommand {

    /**
     * A Section is a block of text contained within the tutorial. It can contain multiple subsections that can descend
     * multiple layers.
     *
     * @param title       The title of this section.
     * @param root        Whether this section should be immediately tab completable or must be in tab completion.
     * @param subsections Any sections that are bellow this section.
     * @param msg         The message block that will be sent to players.
     */
    private record Section (String title, boolean root, List<String> subsections, List<String> msg) { }

    /**
     * A map of sections by their reference contained in this plugin.
     */
    private final Map<String, Section> sections = new HashMap<>();

    /**
     * A precomputed list of tab completions we can provide at the root.
     */
    private final List<String> roots = new ArrayList<>();

    /**
     * Creates a new BetterCommand with the given name.
     */
    public Tutorial () {
        super("tutorial");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(RPGFramework.getPlugin().getResource("tutorials.yml"))));
        for (String key : data.getKeys(false))
            sections.put(key.toLowerCase(), new Section(data.getString(key + ".title"), data.getBoolean(key + ".root"), data.getStringList(key + ".subsections"), data.getStringList(key + ".message")));
        sections.forEach((str, s) -> {
            if (s.root())
                roots.add(str);
        } );
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
            sender.sendMessage(ChatColor.BOLD + " ====== Tutorial ====== ");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7This server runs with some extra &2rpg &7mechanics."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7For example, you can join a class with '&9/classes&7'."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7The game has also been rebalanced to reduce &9armor &7and &dregeneration."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7To compensate players are granted additional &chealth &7and &apower &7through &9classes&7."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7There are currently 2 bosses, &aEiryeras &7and &aKrusk&7."));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7For more information type &r/tutorial index&7."));
            sender.sendMessage(ChatColor.BOLD + " ====== Main Page ====== ");
            return true;
        }
        Section section = sections.get(args[args.length - 1].toLowerCase());
        if (section == null) {
            sender.sendMessage(ChatColor.RED + "We could not find that section.");
            return true;
        }
        sender.sendMessage(ChatColor.BOLD + " ====== Tutorial ====== ");
        for (String s : section.msg())
            sender.sendMessage(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', s));
        sender.sendMessage(ChatColor.BOLD + " ====== " + section.title() + " ====== ");
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
        if (args.length <= 1)
            return everythingStartsWith(roots, args[0]);
        Section previous = sections.get(args[args.length - 2]);
        if (previous == null)
            return new ArrayList<>();
        if (previous.subsections() != null)
            return everythingStartsWith(previous.subsections(), args[args.length - 1]);
        return new ArrayList<>();
    }
}
