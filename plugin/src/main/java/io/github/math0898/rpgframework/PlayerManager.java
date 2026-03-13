package io.github.math0898.rpgframework;

import io.github.math0898.rpgframework.parties.Party;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.github.math0898.rpgframework.RPGFramework.getInstance;

public final class PlayerManager implements Listener {

    private static final String CONSOLE_PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] "
                    + "[" + ChatColor.LIGHT_PURPLE + "PlayMgr" + ChatColor.DARK_GRAY + "] ";

    private static final String[] HONORABLE_DEATH_MESSAGES = {
            " was slain by ",
            " met an honorable death by ",
            " was bested by ",
            " lost in a fight against ",
            ", in an epic duel lost to ",
            " met their fate by the hands of "
    };

    private static final int AUTO_SAVE_PERIOD_TICKS = 60 * 5 * 20;
    private static final int POST_JOIN_HEAL_DELAY_TICKS = 5;
    private static final int PASSIVE_START_DELAY_TICKS = 20 * 20;

    private static final int BASE_REGEN_TICKS_PER_HEART = 20 * 4;
    private static final double TOTEM_SOUND_VOLUME = 0.8f;
    private static final float DEATH_SOUND_PITCH = 2.0f;
    private static final float DEATH_SOUND_VOLUME = 0.8f;

    private static final Map<UUID, RpgPlayer> PLAYERS_BY_ID = new HashMap<>();

    private PlayerManager() {
    }

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PlayerManager(), getInstance());
        Bukkit.getScheduler().runTaskTimerAsynchronously(
                getInstance(),
                PlayerManager::saveAllPlayers,
                AUTO_SAVE_PERIOD_TICKS,
                AUTO_SAVE_PERIOD_TICKS
        );
    }

    public static RpgPlayer getPlayer(UUID uuid) {
        return PLAYERS_BY_ID.get(uuid);
    }

    public static RpgPlayer getPlayer(String name) {
        for (RpgPlayer player : PLAYERS_BY_ID.values()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    public static void addPlayer(RpgPlayer player) {
        if (player == null) {
            return;
        }

        log("Adding player to player list.");
        PLAYERS_BY_ID.put(player.getUuid(), player);
        Bukkit.getScheduler().runTaskLater(getInstance(), player::passive, PASSIVE_START_DELAY_TICKS);
        log("Player added to player list.", ChatColor.GREEN);
    }

    public static void removePlayer(@Nullable RpgPlayer player) {
        if (player == null) {
            return;
        }
        removePlayer(player.getUuid());
    }

    public static void removePlayer(@Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        PLAYERS_BY_ID.remove(uuid);
    }

    public static void scaleHealth(Player player) {
        AttributeInstance maxHealthAttribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = Objects.requireNonNull(maxHealthAttribute).getValue();

        player.setHealthScale(maxHealth);
        scaleRegen(player, 1.0);
    }

    public static void scaleRegen(Player player, double modifier) {
        int regenRate = (int) (BASE_REGEN_TICKS_PER_HEART * modifier);
        player.setSaturatedRegenRate(regenRate);
        player.setUnsaturatedRegenRate(regenRate);
    }

    public static void hunger(EntityExhaustionEvent event) {
        event.setCancelled(true);
    }

    public static void environmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isLethal(player, event)) {
            return;
        }

        if (isHoldingTotem(player)) {
            return;
        }

        RpgPlayer rpgPlayer = getPlayer(player.getUniqueId());
        if (rpgPlayer == null) {
            return;
        }

        if (!rpgPlayer.inCombat()) {
            event.setCancelled(true);
            dishonorableDeath(rpgPlayer, event.getCause());
            return;
        }

        rpgPlayer.damaged(event);
        if (event.isCancelled() || !isLethal(player, event)) {
            return;
        }

        event.setCancelled(true);
        if (!rpgPlayer.revive()) {
            honorableDeath(rpgPlayer);
        }
    }

    public static void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        RpgPlayer rpgPlayer = getPlayer(player.getUniqueId());
        if (rpgPlayer == null) {
            return;
        }

        rpgPlayer.damaged(event);
        if (event.isCancelled() || !isLethal(player, event)) {
            return;
        }

        if (isHoldingTotem(player)) {
            return;
        }

        event.setCancelled(true);
        if (!rpgPlayer.revive()) {
            honorableDeath(rpgPlayer);
        }
    }

    public static void honorableDeath(RpgPlayer player) {
        Entity killer = player.getLastHitBy();
        if (killer == null) {
            dishonorableDeath(player, EntityDamageEvent.DamageCause.CUSTOM);
            return;
        }

        String deathMessage = buildHonorDeathMessage(player, killer);
        allDeaths(player, deathMessage);
    }

    public static void dishonorableDeath(RpgPlayer player, EntityDamageEvent.DamageCause cause) {
        Player bukkitPlayer = player.getBukkitPlayer();
        String deathMessage = player.getPlayerRarity()
                + bukkitPlayer.getName()
                + ChatColor.GRAY
                + " died to "
                + cause.toString().toLowerCase();

        player.dropAll();
        bukkitPlayer.setExp(bukkitPlayer.getExp() / 2);
        bukkitPlayer.setLevel(bukkitPlayer.getLevel() / 2);

        allDeaths(player, deathMessage);
    }

    public static void allDeaths(RpgPlayer player, String message) {
        Player bukkitPlayer = player.getBukkitPlayer();

        log("Death location: " + bukkitPlayer.getLocation());
        teleportToRespawnLocation(bukkitPlayer);
        restorePlayerState(player);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(message);
        }

        log(message);
        bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, DEATH_SOUND_VOLUME, DEATH_SOUND_PITCH);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        RpgPlayer player = new RpgPlayer(event.getPlayer());

        addPlayer(player);
        Bukkit.getScheduler().runTaskLater(getInstance(), player::heal, POST_JOIN_HEAL_DELAY_TICKS);
        DataManager.getInstance().load(player);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        RpgPlayer player = getPlayer(event.getPlayer().getUniqueId());

        DataManager.getInstance().save(player);
        removePlayerFromParty(player);
        removePlayer(player);
    }

    public static void saveAllPlayers() {
        if (PLAYERS_BY_ID.isEmpty()) {
            return;
        }

        DataManager dataManager = DataManager.getInstance();
        for (RpgPlayer player : PLAYERS_BY_ID.values()) {
            dataManager.save(player);
        }
    }

    private static boolean isLethal(Player player, EntityDamageEvent event) {
        return player.getHealth() <= event.getDamage();
    }

    private static boolean isHoldingTotem(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    private static String buildHonorDeathMessage(RpgPlayer victim, Entity killer) {
        Player victimPlayer = victim.getBukkitPlayer();
        String deathFlavor = randomDeathFlavor();

        if (killer instanceof Player killerPlayer) {
            RpgPlayer rpgKiller = getPlayer(killerPlayer.getUniqueId());
            String killerName = rpgKiller != null
                    ? rpgKiller.getPlayerRarity() + killerPlayer.getName()
                    : ChatColor.BLACK + killerPlayer.getName();

            return victim.getPlayerRarity() + victimPlayer.getName() + ChatColor.GRAY + deathFlavor + killerName;
        }

        if (killer.isCustomNameVisible() && killer.getCustomName() != null) {
            victimPlayer.sendMessage(killer.getCustomName() + ChatColor.GRAY + " has left the fight upon your death");
            killer.remove();
            return victim.getPlayerRarity() + victimPlayer.getName() + ChatColor.GRAY + deathFlavor + killer.getCustomName();
        }

        return victim.getPlayerRarity() + victimPlayer.getName() + ChatColor.GRAY + deathFlavor + killer.getName();
    }

    private static String randomDeathFlavor() {
        int index = ThreadLocalRandom.current().nextInt(HONORABLE_DEATH_MESSAGES.length);
        return HONORABLE_DEATH_MESSAGES[index];
    }

    private static void teleportToRespawnLocation(Player player) {
        Location bedSpawn = player.getBedSpawnLocation();
        if (bedSpawn != null) {
            player.teleport(bedSpawn);
            return;
        }

        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            player.teleport(defaultWorld.getSpawnLocation());
        }
    }

    private static void restorePlayerState(RpgPlayer player) {
        Player bukkitPlayer = player.getBukkitPlayer();

        player.heal();
        bukkitPlayer.setFireTicks(0);

        for (PotionEffect effect : bukkitPlayer.getActivePotionEffects()) {
            bukkitPlayer.removePotionEffect(effect.getType());
        }
    }

    private static void removePlayerFromParty(@Nullable RpgPlayer player) {
        if (player == null) {
            return;
        }

        Party party = player.getParty();
        if (party == null) {
            return;
        }

        party.removePlayer(player.getBukkitPlayer());
        player.setParty(null);
    }

    private static void log(String message) {
        log(message, ChatColor.GRAY);
    }

    private static void log(String message, ChatColor color) {
        Bukkit.getServer().getConsoleSender().sendMessage(CONSOLE_PREFIX + color + message);
    }
}
