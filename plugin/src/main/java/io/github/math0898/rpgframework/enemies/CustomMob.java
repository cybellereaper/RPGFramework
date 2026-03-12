package io.github.math0898.rpgframework.enemies;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import sugaku.rpg.framework.items.BossDrop;
import sugaku.rpg.framework.items.Rarity;
import io.github.math0898.rpgframework.PlayerManager;
import io.github.math0898.rpgframework.RpgPlayer;

import java.util.Objects;
import java.util.Random;

/**
 * The CustomMob class manages many of the additional things that we need to do to our custom mobs, such as managing
 * armor and custom attributes.
 *
 * @author Sugaku
 */
public abstract class CustomMob {

    /**
     * The prefix sent with every message.
     */
    private final String MESSAGE_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    /**
     * The helmet of the custom mob.
     * -- SETTER --
     *  Sets the helmet of the new mob.
     */
    @Setter
    private ItemStack helm;

    /**
     * The chestplate of the custom mob.
     * -- SETTER --
     *  Sets the chestplate of the new mob.
     */
    @Setter
    private ItemStack chestplate;

    /**
     * The leggings of the custom mob.
     * -- SETTER --
     *  Sets the leggings of the new mob.
     */
    @Setter
    private ItemStack leggings;

    /**
     * The boots of the custom mob.
     * -- SETTER --
     *  Sets the boots of the new mob.
     */
    @Setter
    private ItemStack boots;

    /**
     * The held item of the custom mob.
     * -- SETTER --
     *  Sets the main hand of the new mob.
     */
    @Setter
    private ItemStack hand;

    /**
     * The item held in the off-hand of the custom mob.
     * -- SETTER --
     *  Sets the off-hand of the new mob.
     */
    @Setter
    private ItemStack offHand;

    /**
     * The type of mob the custom mob is.
     */
    private final EntityType mobType;

    /**
     * The name string of the mob.
     */
    private final String name;

    /**
     * The rarity of the boss. Somewhat related to difficulty, but mainly used to color their name.
     */
    private final Rarity rarity;

    /**
     * The base health that this mob has. This can be scaled up when battling parties. Some niche situations can re-roll
     * the enemy's base health which means this can sometimes change after construction.
     * -- SETTER --
     *  Sets the max health of the mob.
     * -- GETTER --
     *  Accessor method for this mob's base health.
     */
    @Getter
    @Setter
    private int baseHealth;

    /**
     * A pointer to the living entity object.
     */
    @Getter
    private LivingEntity entity;

    /**
     * The number of mechanics this mob has, 1-20%,2-40%,3-60%,4-80%,5-onSpawn. It's very strange to put a state machine
     * here in the middle of a CustomMob. Instead, let's define health thresholds at which mechanics should occur in a
     * yaml file and check if we cross any thresholds on damage taken.
     */
    @Setter
    @Getter
    @Deprecated(forRemoval = true)
    private int activeMechanics = 0;

    /**
     * The location to spawn this mob at.
     * -- SETTER --
     *  Sets the location that this mob will be spawned at.
     * -- GETTER --
     *  An accessor method to the location to spawn this CustomMob at.
     */
    @Getter
    @Setter
    private Location spawnPoint;

    /**
     * Has this mob been spawned.
     * -- GETTER --
     *  An accessor method for if the CustomMob has been spawned into the world yet or not.
     */
    @Getter
    private boolean spawned = false;

    /**
     * A basic constructor to aid in the construction of a CustomMob. Contains everything essential to run spawn().
     *
     * @param name The name of the mob.
     * @param mobType The type of mob that should be spawned.
     * @param rarity The rarity of the mob being spawned.
     * @param baseHealth The maximum health of the mob spawned
     */
    public CustomMob (String name, EntityType mobType, Rarity rarity, int baseHealth) {
        this.name = name;
        this.mobType = mobType;
        this.rarity = rarity;
        this.baseHealth = baseHealth;
    }

    /**
     * Sends a message to the given recipient.
     */
    public void send (CommandSender user, String message) {
        user.sendMessage(MESSAGE_PREFIX + message);
    }

    /**
     * Spawn the mob described by the CustomMob object at the given location l.
     *
     * @param l The location the mob should be spawned at.
     */
    @SuppressWarnings("deprecation")
    public void spawn (Location l) {
        Mob e = (Mob) Objects.requireNonNull(l.getWorld()).spawnEntity(l, mobType);

        e.setCustomNameVisible(true);
        e.setCustomName(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + Rarity.toColor(rarity) + name);
        Objects.requireNonNull(e.getEquipment()).setHelmet(this.helm);
        Objects.requireNonNull(e.getEquipment()).setChestplate(this.chestplate);
        Objects.requireNonNull(e.getEquipment()).setLeggings(this.leggings);
        Objects.requireNonNull(e.getEquipment()).setBoots(this.boots);
        Objects.requireNonNull(e.getEquipment()).setItemInMainHand(this.hand);
        Objects.requireNonNull(e.getEquipment()).setItemInOffHand(this.offHand);
        e.setMaxHealth(this.baseHealth); //Deprecated
        e.setHealth(this.baseHealth);

        e.setLootTable(null);
        e.getEquipment().setHelmetDropChance(0.0f);
        e.getEquipment().setChestplateDropChance(0.0f);
        e.getEquipment().setLeggingsDropChance(0.0f);
        e.getEquipment().setBootsDropChance(0.0f);
        e.getEquipment().setItemInMainHandDropChance(0.0f);
        e.getEquipment().setItemInOffHandDropChance(0.0f);
        e.setCanPickupItems(false); //No more stealing gear

        this.entity = e;
        spawned = true;
    }

    /**
     * A helpful utility method to set armor in a batch.
     *
     * @param helm The helmet of the new mob.
     * @param chestplate The chestplate of the new mob.
     * @param leggings The leggings of the new mob.
     * @param boots The boots of the new mob.
     */
    public void setArmor (ItemStack helm, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        this.helm = helm;
        this.chestplate = chestplate;
        this.leggings = leggings;
        this.boots = boots;
    }

    /**
     * Returns the custom name of the mob being created.
     *
     * @return The custom name of this mob in it's formatted form.
     */
    public String getCustomName () {
        return ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + rarity + name;
    }

    /**
     * Handles drops of the custom mob. By default, no items are dropped. Use
     * {@link #handleDrops(EntityDeathEvent, BossDrop[], Rarity)} to manage drops with pre-declared BossDrops.
     *
     * @param event The EntityDeathEvent to manage.
     */
    @Deprecated(forRemoval = true)
    public static void handleDrops (EntityDeathEvent event) {
        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    /**
     * Handles the drops of the custom mob. Uses the table of BossDrops combined with the boss's rarity to determine
     * what loot should drop at what rate.
     * // todo: Rarity should instead be replaced with a value like VANILLA_EXPERIENCE. It's strange that it's used for drop rolls.
     * // todo: Add a field to CustomMob to determine how much RPG experience it drops. We will also need an actual equation to determine xp awarded.
     * // todo: Make non-static.
     *
     * @param event The event where the entity died.
     * @param bossDrops The table of drops.
     */
    public static void handleDrops (EntityDeathEvent event, BossDrop[] bossDrops, Rarity rarity) {
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setDroppedExp(25 * rarity.ordinal());
        for (Player p : Bukkit.getOnlinePlayers()) {
            RpgPlayer player = PlayerManager.getPlayer(p.getUniqueId());
            if (player == null) continue;
            if (player.getActiveBossUnsafe() == null) continue;
            System.out.println(player.getActiveBossUnsafe());
            if (player.getActiveBossUnsafe().getEntity() == null) continue;
            System.out.println(player.getActiveBossUnsafe().getEntity());
            if (player.getActiveBossUnsafe().getEntity().getEntityId() == event.getEntity().getEntityId())
                player.giveExperience(Math.max(100 - player.getLevel(), 1));
        }
        World world = event.getEntity().getWorld();
        Random rand = new Random();
        double roll = rand.nextDouble();
        double check = 0.0;
        while (true) {
            for (BossDrop i : bossDrops) {
                if (roll < check + getRate(i.getRarity(), rarity)) {
                    world.dropItemNaturally(event.getEntity().getLocation(), i.getItem());
                    return;
                } else check += getRate(i.getRarity(), rarity);
            }
            roll = rand.nextDouble();
        }
    }

    /**
     * A helpful method to determining the drop rate of items depending on the challenge of the boss. This seems like an
     * odd way to go about boss drops and should probably be removed.
     *
     * @param item The rarity of the item being dropped.
     * @param boss The rarity of the boss being fought.
     * @return The rate at which an item of the given rarity should drop for a boss of the given rarity.
     */
    @Deprecated(forRemoval = true)
    private static double getRate (Rarity item, Rarity boss) {
//        switch (boss) {
//            case UNCOMMON:
//            default:
                return switch (item) {
                    case COMMON -> 0.16;
                    case UNIQUE -> 0.08;
                    case RARE -> 0.04;
                    case LEGENDARY -> 0.02;
                    case MYTHIC -> 0.01;
                    case RELIC -> 0.005;
                };
//            case RARE:
//                return switch (item) {
//                    case COMMON -> 0.16;
//                    case UNCOMMON -> 0.08;
//                    case RARE -> 0.04;
//                    case LEGENDARY -> 0.02;
//                    case HEROIC -> 0.01;
//                    case MYTHIC -> 0.005;
//                };
//        }
//        return 0.0;
    }

    /**
     * Called whenever a player damages a CustomMob.
     *
     * @param attacker The player who attacked this mob.
     */
    public void damaged (Player attacker) {
        if (entity.isInWater()) {
            World world = attacker.getWorld();
            world.strikeLightning(attacker.getLocation());
            send(attacker, "The gods smite you for your attempted cheese.");
        }
    }

    /**
     * Called when a boss's health drops bellow the first threshold.
     *
     * @param attacker The player who caused this drop to occur.
     */
    @Deprecated(forRemoval = true)
    public void firstMechanic (Player attacker) {
        activeMechanics = 0;
    }

    /**
     * Called when a boss's health drops bellow the second threshold.
     *
     * @param attacker The player who caused this drop to occur.
     */
    @Deprecated(forRemoval = true)
    public void secondMechanic (Player attacker) {
        activeMechanics = 1;
    }
}
