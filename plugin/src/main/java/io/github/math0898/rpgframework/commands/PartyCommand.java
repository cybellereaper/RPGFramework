package io.github.math0898.rpgframework.commands;

import io.github.math0898.rpgframework.PlayerManager;
import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.classes.Classes;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.parties.Party;
import io.github.math0898.rpgframework.parties.PartyManager;
import io.github.math0898.utils.commands.BetterCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * The Party Command is used for interacting and manipulating parties.
 *
 * @author Sugaku
 */
public class PartyCommand extends BetterCommand {

    /**
     * Creates a new BetterCommand with the given name.
     */
    public PartyCommand () {
        super("party", ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY);
    }

    /**
     * Called whenever specifically a player executes this command.
     *
     * @param player The player who ran this command.
     * @param args   The arguments they passed to the command.
     */
    @Override
    public boolean onPlayerCommand (Player player, String[] args) {
        if (args.length == 0) {
            send(player, "Sorry but something about that usage seemed off. Here is a list of options.");
            send(player, "- accept: Accept an incoming invite.", false);
            send(player, "- chat: Toggle party chat.", false);
            send(player, "- info: View status information about party members.", false);
            send(player, "- invite <name>: Invite the given online player.", false);
            send(player, "- kick <name>: Remove the given player from your party.", false);
            send(player, "- leave: Leave the party you are currently in.", false);
            send(player, "- list: Lists members of your party including invitations.", false);
            send(player, "- promote <name>: Designates a new party member as leader.", false);
            send(player, "- summon: Summons party members to your location.", false);
        }
        else switch (args[0]) {
                case "accept" -> acceptSubcommand(player);
                case "info" -> infoSubcommand(player);
                case "invite" -> inviteSubcommand(player, args);
                case "kick" -> kickSubcommand(player, args);
                case "leave" -> leaveSubcommand(player);
                case "list" -> listSubcommand(player);
                case "summon" -> summonSubcommand(player);
                case "chat" -> PartyManager.togglePartyChat(player);
                case "promote" -> promoteSubcommand(player, args);
                default -> send(player, ChatColor.RED + "Command not found.");
            }
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
        send(sender, "Please only run this command as a player.");
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
            String[] Options = { "accept", "chat", "info", "invite", "kick", "leave", "list", "promote", "summon" };
            if (!args[0].isEmpty()) { for (String o: Options) if (o.toLowerCase().startsWith(args[0].toLowerCase())) list.add(o); }
            else list.addAll(Arrays.asList(Options));
        } else if (args[1].equalsIgnoreCase("invite")) Bukkit.getOnlinePlayers().forEach((p) -> list.add(p.getName()));
        else if (args[1].equalsIgnoreCase("kick") || args[1].equalsIgnoreCase("promote")) {
            if (sender instanceof Player player) {
                RpgPlayer rpgPlayer = PlayerManager.getPlayer(player.getUniqueId());
                assert rpgPlayer != null;
                Party party = rpgPlayer.getParty();
                if (party != null) party.getPlayers().forEach((p) -> list.add(p.getName()));
            }
        }
        return list;
    }

    /**
     * Accepts the currently pending invitation to a party.
     *
     * @param sender The player who accepted the invitation.
     */
    private void acceptSubcommand (CommandSender sender) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party pending = rpgPlayer.getPendingParty();
        Party party = rpgPlayer.getParty();
        if (party != null) send(sender, "You are already in a party!");
        else if (pending != null) {
            if (pending.addPlayer((Player) sender)) {
                Classes playerClass = rpgPlayer.getCombatClass();
                pending.sendAll(prefix + "[" + playerClass.getFormattedName() + ChatColor.DARK_GRAY + "] "
                        + ChatColor.GRAY + rpgPlayer.getBukkitPlayer().getName() + " has joined the party!");
                send(sender, "You have joined the party!");
                rpgPlayer.setParty(pending);
            } else send(sender, "Sorry, but that party is full.");
        } else send(sender,"You do not have any party invites.");
    }

    /**
     * Attempts to send information about the players current party members.
     *
     * @param sender The player that should be sent the information. Their party is also the one that should be sent.
     */
    private void infoSubcommand (CommandSender sender) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (party == null) {
            send(sender, ChatColor.RED + "You are not in a party.");
            return;
        }
        send(sender, ChatColor.GRAY + "---- Party Status ----");
        for (Player p : party.getPlayers()) {
            RpgPlayer rpg = PlayerManager.getPlayer(p.getUniqueId());
            if (rpg == null) continue;
            send(sender, ChatColor.GRAY + p.getName() + " -> " + rpg.getFormattedHealth());
        }
    }

    /**
     * Invites the given player to the command sender's party.
     *
     * @param sender The player who is inviting another.
     * @param args The command arguments which should include a player to invite.
     */
    private void inviteSubcommand (CommandSender sender, String[] args) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (args.length == 1) {
            send(sender, "You must list a player as an argument. /party invite <name>");
            return;
        } else if (party == null) {
            party = new Party((Player) sender);
            rpgPlayer.setParty(party);
            PartyManager.addParty(party);
        }
        ArrayList<RpgPlayer> invitedPlayers = new ArrayList<>();
        for (int i = 1; i < args.length; i++) invitedPlayers.add(PlayerManager.getPlayer(args[i]));
        for (RpgPlayer p: invitedPlayers)  {
            if (p == null) {
                send(rpgPlayer.getBukkitPlayer(), ChatColor.RED + "Could not find one or more players. Are they online?");
                break;
            }
            send(p.getBukkitPlayer(), "You have been invited to join " + rpgPlayer.getName() + "'s party.");
            send(rpgPlayer.getBukkitPlayer(), "You have invited " + p.getName() + " to the party.");
            p.setPendingParty(party);
        }
    }

    /**
     * Kicks players listed in args if they're currently part of the party.
     *
     * @param sender The sender of the command. Should be a party leader for anything to happen.
     * @param args The arguments of the command.
     */
    private void kickSubcommand (CommandSender sender, String[] args) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (args.length == 1) {
            send(sender, "You must list a player as an argument. /party kick <name>");
            return;
        } else if (party == null) {
            send(sender, "You cannot kick someone if you're not in a party.");
            return;
        } else if (!party.getLeader().equals(sender)) {
            send(sender, "You cannot kick someone if you're not the party leader.");
            return;
        }
        ArrayList<RpgPlayer> kickedPlayers = new ArrayList<>();
        for (int i = 1; i < args.length; i++) kickedPlayers.add(PlayerManager.getPlayer(args[i]));
        for (RpgPlayer p : kickedPlayers) {
            party.sendAll(prefix + p.getName() + " has been kicked from the party.");
            p.setParty(null);
            party.removePlayer(p.getBukkitPlayer());
        }
    }

    /**
     * The player who executed the command attempts the leave any parties they are a part of.
     *
     * @param sender The player who executed the command.
     */
    private void leaveSubcommand (CommandSender sender) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (party == null) {
            send(sender, "You are not in a party and thus cannot leave the party.");
            return;
        }
        rpgPlayer.setParty(null);
        party.removePlayer((Player) sender);
        send(sender, "You have left the party.");
        Classes playerClass = rpgPlayer.getCombatClass();
        party.sendAll(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] ["
                + playerClass.getFormattedName() + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY
                + rpgPlayer.getBukkitPlayer().getName() + " has left the party.");
    }

    /**
     * Sends the command sender a list of all players currently in the party.
     *
     * @param sender The sender of the command.
     */
    private void listSubcommand (CommandSender sender) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (party == null)
            send(sender, "You do not belong to a party. A party can be started by inviting another player or accepting an invite from someone else.");
        else {
            send(sender, "This is your current party roster:");
            Collection<RpgPlayer> players = party.getRpgPlayers();
            int i = 1;
            for (RpgPlayer p: players) {
                Classes playerClass = rpgPlayer.getCombatClass();
                send(sender, ChatColor.GRAY + "" + i + ". " + ChatColor.DARK_GRAY + "[" + playerClass.getFormattedName() + ChatColor.DARK_GRAY + "] "
                        + p.getPlayerRarity() + p.getName(), false);
                i++;
            }
        }
    }

    /**
     * Will summon all party members to the command sender so long as they are the party leader.
     *
     * @param sender The player who executed the command.
     */
    private void summonSubcommand (CommandSender sender) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (party == null) send(sender, "You do not belong to a party and thus cannot summon your party.");
        else if (!party.getLeader().equals(sender)) send(sender, "You are not the party leader and cannot summon everyone.");
        else {
            party.getPlayers().forEach((p) -> {
                if (!p.equals(party.getLeader())) {
                    p.sendMessage(prefix + "You are being summoned to " + sender.getName() + ". 3s");
                    Bukkit.getScheduler().runTaskLater(RPGFramework.getPlugin(), () -> p.teleport(((Player) sender).getLocation()), 3 * 20);
                }
                else p.sendMessage(prefix + "You are summoning your party. They will be here in 3s");
            });
        }
    }

    /**
     * Attempts to promote a player passed through arguments as leader.
     *
     * @param sender The sender of the command.
     * @param args The arguments given to the command.
     */
    private void promoteSubcommand (CommandSender sender, String[] args) {
        RpgPlayer rpgPlayer = PlayerManager.getPlayer(sender.getName());
        assert rpgPlayer != null;
        Party party = rpgPlayer.getParty();
        if (party == null) send(sender, ChatColor.RED + "You do not belong to a party and thus cannot promote anyone.");
        else if (!party.getLeader().equals(sender)) send(sender, ChatColor.RED + "You are not the party leader and thus cannot designate a new leader.");
        else if (args.length < 2) send(sender, ChatColor.RED + "You must select someone to promote.");
        else {
            Player p = Bukkit.getPlayer(args[1]);
            if (p == null) send(sender, ChatColor.RED + "Player not found!");
            else if (!party.hasMember(p)) send(sender, ChatColor.RED + "That player is not currently part of the party!");
            else {
                party.promote(p);
                party.sendAll(prefix + ChatColor.GREEN + args[1] + " has been promoted to leader!");
            }
        }
    }
}
