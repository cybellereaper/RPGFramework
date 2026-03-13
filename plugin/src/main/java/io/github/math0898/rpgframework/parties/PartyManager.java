package io.github.math0898.rpgframework.parties;

import io.github.math0898.rpgframework.PlayerManager;
import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.Classes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;


/**
 * The party manager handles things relating to parties in general. This includes things like party chat.
 *
 * @author Sugaku
 */
public class PartyManager implements Listener {

    /**
     * The list of players who are currently using party chat.
     */
    private static final ArrayList<Player> partyChatPlayers = new ArrayList<>();

    /**
     * The list of currently active players. When the last player leaves a party it is removed from this list.
     */
    private static final ArrayList<Party> parties = new ArrayList<>();

    /**
     * Initializes the party manager so that it can listen to events.
     */
    public static void init () {
        Bukkit.getPluginManager().registerEvents(new PartyManager(), RPGFramework.getPlugin());
    }

    /**
     * Adds a party to the current list of parties.
     *
     * @param p The party to add to the list.
     */
    public static void addParty (Party p) {
        parties.add(p);
    }

    /**
     * Removes a party from the current list of parties.
     *
     * @param p The party to remove from the list.
     */
    public static void removeParty (Party p) {
        parties.remove(p);
    }

    /**
     * Finds the party that a player is currently in.
     *
     * @param player The player to locate the party of.
     * @return The party this player is a member of. Null otherwise.
     */
    public static Party findParty (Player player) {
        for (Party p : parties) if (p.hasMember(player)) return p;
        return null;
    }

    /**
     * Toggles party chat for the given player. If they are not currently at a party this will fail.
     *
     * @param player The player to toggle party chat for.
     */
    public static void togglePartyChat (Player player) {
        if (partyChatPlayers.contains(player)) {
            player.sendMessage(ChatColor.GREEN + "You have left party chat!"); // todo clean these messages up.
            partyChatPlayers.remove(player);
        } else if (findParty(player) != null) {
            player.sendMessage(ChatColor.GREEN + "You have joined party chat!");
            partyChatPlayers.add(player);
        } else player.sendMessage(ChatColor.RED + "You must be in a party to toggle party chat.");
    }

    /**
     * Handles the even when a player tries to chat. If they have party chat enabled the message will be sent to their
     * party.
     *
     * @param event The chat event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat (AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (partyChatPlayers.contains(p)) {
            event.setCancelled(true);
            Party party = findParty(p);
            if (party == null) return;
            RpgPlayer rpg = PlayerManager.getPlayer(p.getUniqueId());
            String prefix = ChatColor.GREEN + p.getName() + ChatColor.DARK_GRAY + " > " + ChatColor.LIGHT_PURPLE;
            if (rpg == null) prefix = ChatColor.DARK_GRAY + "[" + Classes.NONE.getFormattedName() + ChatColor.DARK_GRAY + "] " + prefix;
            else prefix = ChatColor.DARK_GRAY + "[" + rpg.getCombatClass().getFormattedName() + ChatColor.DARK_GRAY + "] " + prefix;
            party.sendAll(prefix + event.getMessage());
            Bukkit.getConsoleSender().sendMessage(prefix + event.getMessage());
        }
    }

    /**
     * Handles when two players hit one another and checks if they're in the same party. In that case the damage should
     * be negated.
     *
     * @param event The player attack player event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage (EntityDamageByEntityEvent event) {
        Player attacker;
        Player victim;
        if (event.getEntity() instanceof Player) victim = (Player) event.getEntity();
        else return;
        if (event.getDamager() instanceof Arrow arrow) {
            if (arrow.getShooter() instanceof Player) attacker = (Player) arrow.getShooter();
            else return;
        } else if (event.getDamager() instanceof Player ) attacker = (Player) event.getDamager();
        else return;
        Party a = findParty(attacker);
        Party v = findParty(victim);
        if (a != null && v != null) if (a.equals(v)) event.setCancelled(true);
    }
}
