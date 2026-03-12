package io.github.math0898.rpgframework.commands;

import io.github.math0898.rpgframework.enemies.CustomMobEntry;
import io.github.math0898.utils.commands.BetterCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sugaku.rpg.framework.mobs.MobManager;
import sugaku.rpg.mobs.gods.Inos;
import sugaku.rpg.mobs.teir1.eiryeras.EiryerasBoss;
import sugaku.rpg.mobs.teir1.feyrith.FeyrithBoss;
import sugaku.rpg.mobs.teir1.krusk.KruskBoss;
import sugaku.rpg.mobs.teir1.krusk.KruskMinion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The SummonRPG command is used by admins and developers to summon RPG mobs into the world without a ritual.
 *
 * @author Sugaku
 */
public class SummonRPG extends BetterCommand {

    /**
     * Creates a new BetterCommand with the given name.
     */
    public SummonRPG () {
        super("summonrpg", ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] ");
    }

    /**
     * Called whenever specifically a player executes this command.
     *
     * @param player The player who ran this command.
     * @param args   The arguments they passed to the command.
     */
    @Override
    public boolean onPlayerCommand (Player player, String[] args) { // todo: Poor design and high cognitive complexity.
        if (args.length < 1 || args.length > 2) {
            send(player, ChatColor.RED + "Check your usage. Should be /summonRPG <mob> (amnt)");
            return true;
        }

        int loop = getIntegerParam(1, args, player);
        MobManager mobManager = MobManager.getInstance();

        // todo: This pattern is very trouble some as the number of enemies increases.
        if (args[0].equalsIgnoreCase("Eiryeras")) {
            send(player, ChatColor.GREEN + "Eiryeras," + ChatColor.GRAY + " honored hunter of the Agloytan area has been summoned.");
            for (int i = 0; i < loop; i++) mobManager.addMob(new EiryerasBoss(player.getLocation()));
        } else if (args[0].equalsIgnoreCase("Krusk")) {
            send(player, ChatColor.GREEN + "Krusk,"  + ChatColor.GRAY +  " one of the generals of the undead has been summoned.");
            for (int i = 0; i < loop; i++) mobManager.addMob(new KruskBoss(player.getLocation()));
        } else if (args[0].equalsIgnoreCase("KruskMinion")) {
            send(player, "One of " + ChatColor.GREEN +  "Krusk's" + ChatColor.GRAY + " minions has been summoned.");
            for (int i = 0; i < loop; i++) mobManager.addMob(new KruskMinion(player.getLocation(), (int) (new Random().nextDouble() * 8)));
        } else if (args[0].equalsIgnoreCase("Feyrith")) {
            send(player, "One of " + ChatColor.BLUE +  "Feyrith" + ChatColor.GRAY + " has been summoned.");
            for (int i = 0; i < loop; i++) mobManager.addMob(new FeyrithBoss(player.getLocation()));
        } else if (args[0].equalsIgnoreCase("Inos")) {
            send(player, "One of " + ChatColor.RED +  "Inos" + ChatColor.GRAY + " has been summoned.");
            for (int i = 0; i < loop; i++) mobManager.addMob(new Inos(player.getLocation()));
        } else if (args[0].equalsIgnoreCase("seignour")) {
            send(player, "One of " + ChatColor.BLUE +  "Seignour" + ChatColor.GRAY + " has been summoned.");
            io.github.math0898.rpgframework.enemies.MobManager ioMobManager = io.github.math0898.rpgframework.enemies.MobManager.getInstance();
            CustomMobEntry entry = ioMobManager.getCustomMob("boss:Seignour");
            for (int i = 0; i < loop; i++) entry.spawn(player.getLocation());
        } else send(player, ChatColor.RED + "Sorry but we could not find that mob.");
        return true;
    }

    /**
     * Called whenever an unspecified sender executes this command. This could include console and command blocks.
     *
     * @param sender The sender who ran this command.
     * @param args   The arguments they passed to the command.
     */
    @Override
    public boolean onNonPlayerCommand (CommandSender sender, String[] args) {
        send(sender, ChatColor.RED + "This command can only be ran as a player.");
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
        ArrayList<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(List.of("Eiryeras", "Krusk", "KruskMinion", "Feyrith", "Inos", "Seignour")); // todo: I do not like the idea of manually typing these in.
            return everythingStartsWith(list, args[0], false);
        } else if (args.length == 2)
            list.add("(amnt)");
        return list;
    }
}
