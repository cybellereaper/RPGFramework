package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.AbstractClass;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents the Berserker class.
 *
 * <p>The Berserker is a melee-focused class built around aggression, mobility,
 * axe specialization, and a short burst of death-defying survivability through
 * Indomitable Spirit.</p>
 *
 * <p>Ability use requires the class item and the required leather armor pieces.</p>
 */
public class BerserkerClass extends AbstractClass {

    /**
     * Enumerates the Berserker's abilities in cooldown index order.
     */
    private enum Ability {
        HASTE,
        RAGE,
        INDOMITABLE_SPIRIT
    }

    private static final Material CLASS_ITEM = Material.ROTTEN_FLESH;
    private static final String ARMOR_REQUIRED_MESSAGE = "Use leather middle pieces to use abilities.";

    private static final int HASTE_COOLDOWN_SECONDS = 30;
    private static final int HASTE_DURATION_SECONDS = 10;
    private static final int HASTE_AMPLIFIER = 2;

    private static final int RAGE_COOLDOWN_SECONDS = 60;
    private static final int RAGE_DURATION_SECONDS = 10;
    private static final int RAGE_AMPLIFIER = 2;

    private static final int INDOMITABLE_SPIRIT_COOLDOWN_SECONDS = 180;
    private static final int INDOMITABLE_SPIRIT_ACTIVE_SECONDS = 5;
    private static final int INDOMITABLE_SPIRIT_STRENGTH_DURATION_SECONDS = 5;
    private static final int INDOMITABLE_SPIRIT_STRENGTH_AMPLIFIER = 3;

    private static final double DEFENSIVE_DAMAGE_REDUCTION = 10.0;
    private static final double BONUS_AXE_DAMAGE = 10.0;

    private static final Set<Material> VALID_AXES = Set.of(
            Material.WOODEN_AXE,
            Material.STONE_AXE,
            Material.IRON_AXE,
            Material.DIAMOND_AXE,
            Material.NETHERITE_AXE,
            Material.GOLDEN_AXE
    );

    private static final Map<EquipmentSlot, Material> REQUIRED_ARMOR = createRequiredArmor();

    /**
     * Constructs a new {@code BerserkerClass} for the specified player.
     *
     * @param player the RPG player that owns this class instance
     */
    public BerserkerClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    /**
     * Handles left-click ability casting for the Berserker.
     *
     * <p>If the player is using the correct class item and armor setup, this casts Rage.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the ability
     */
    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!isClassItem(type) || !canUseAbilities()) {
            return;
        }

        castRage();
    }

    /**
     * Handles right-click ability casting for the Berserker.
     *
     * <p>If the player is using the correct class item and armor setup, this casts Haste.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the ability
     */
    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!isClassItem(type) || !canUseAbilities()) {
            return;
        }

        castHaste();
    }

    /**
     * Handles the Berserker's death-prevention passive.
     *
     * <p>If Indomitable Spirit is off cooldown, death is prevented and the player
     * is granted a short Strength buff.</p>
     *
     * @return {@code false} if death is prevented; {@code true} if the player should die normally
     */
    @Override
    public boolean onDeath() {
        if (!isAbilityReady(Ability.INDOMITABLE_SPIRIT)) {
            return true;
        }

        RpgPlayer rpgPlayer = getPlayer();
        Player player = rpgPlayer.getBukkitPlayer();

        send(ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Indomitable Spirit" + ChatColor.GREEN + "!");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
        rpgPlayer.addPotionEffect(
                PotionEffectType.STRENGTH,
                toTicks(INDOMITABLE_SPIRIT_STRENGTH_DURATION_SECONDS),
                INDOMITABLE_SPIRIT_STRENGTH_AMPLIFIER
        );
        restartCooldown(Ability.INDOMITABLE_SPIRIT);

        return false;
    }

    /**
     * Determines whether the Berserker is wearing the correct armor piece in the given slot.
     *
     * @param slot the armor slot to validate
     * @return {@code true} if the slot either has no requirement or contains the required armor piece;
     *         {@code false} otherwise
     */
    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        Material requiredMaterial = REQUIRED_ARMOR.get(slot);
        if (requiredMaterial == null) {
            return true;
        }

        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null) {
            return false;
        }

        ItemStack equippedItem = equipment.getItem(slot);
        return equippedItem != null && equippedItem.getType() == requiredMaterial;
    }

    /**
     * Applies the Berserker's defensive effects when taking damage.
     *
     * <p>While wearing the required armor, incoming primary damage is reduced.
     * While Indomitable Spirit is active, incoming damage is fully negated.</p>
     *
     * @param event the incoming advanced damage event
     */
    @Override
    public void damaged(AdvancedDamageEvent event) {
        if (correctArmor()) {
            event.addDamage(-DEFENSIVE_DAMAGE_REDUCTION, event.getPrimaryDamage());
        }

        if (isIndomitableSpiritActive()) {
            event.setCancelled(true);
        }
    }

    /**
     * Applies the Berserker's offensive attack modifiers.
     *
     * <p>Physical attacks performed with an axe gain additional slash damage.</p>
     *
     * @param event the outgoing advanced damage event
     */
    @Override
    public void attack(AdvancedDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        if (!event.getPrimaryDamage().isPhysical()) {
            return;
        }

        if (!isUsingAxe()) {
            return;
        }

        event.addDamage(BONUS_AXE_DAMAGE, DamageType.SLASH);
    }

    /**
     * Casts Rage.
     *
     * <p>Rage grants the Berserker a temporary Strength effect.</p>
     */
    private void castRage() {
        if (!isAbilityReady(Ability.RAGE)) {
            return;
        }

        send(ChatColor.GREEN + "You've used rage!");
        getPlayer().addPotionEffect(PotionEffectType.STRENGTH, toTicks(RAGE_DURATION_SECONDS), RAGE_AMPLIFIER);
        restartCooldown(Ability.RAGE);
    }

    /**
     * Casts Haste.
     *
     * <p>Haste grants the Berserker a temporary Speed effect.</p>
     */
    private void castHaste() {
        if (!isAbilityReady(Ability.HASTE)) {
            return;
        }

        send(ChatColor.GREEN + "You've used haste!");
        getPlayer().addPotionEffect(PotionEffectType.SPEED, toTicks(HASTE_DURATION_SECONDS), HASTE_AMPLIFIER);
        restartCooldown(Ability.HASTE);
    }

    /**
     * Determines whether the Berserker is currently allowed to use abilities.
     *
     * <p>The Berserker must be wearing the required leather armor pieces.</p>
     *
     * @return {@code true} if abilities can be used; {@code false} otherwise
     */
    private boolean canUseAbilities() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    /**
     * Determines whether the given material is the Berserker's class item.
     *
     * @param material the material to test
     * @return {@code true} if the material is the class item; {@code false} otherwise
     */
    protected boolean isClassItem(Material material) {
        return material == CLASS_ITEM;
    }

    /**
     * Determines whether the Berserker is currently holding a valid axe.
     *
     * @return {@code true} if the held main-hand item is a valid axe; {@code false} otherwise
     */
    private boolean isUsingAxe() {
        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null) {
            return false;
        }

        ItemStack heldItem = equipment.getItem(EquipmentSlot.HAND);
        return heldItem != null && VALID_AXES.contains(heldItem.getType());
    }

    /**
     * Determines whether the specified ability is currently off cooldown.
     *
     * @param ability the ability to check
     * @return {@code true} if the ability is ready to use; {@code false} otherwise
     */
    private boolean isAbilityReady(Ability ability) {
        return offCooldown(ability.ordinal());
    }

    /**
     * Restarts the cooldown for the specified ability.
     *
     * @param ability the ability whose cooldown should be restarted
     */
    private void restartCooldown(Ability ability) {
        getCooldowns()[ability.ordinal()].restart();
    }

    /**
     * Determines whether Indomitable Spirit's active invulnerability window is currently in effect.
     *
     * @return {@code true} if Indomitable Spirit is active; {@code false} otherwise
     */
    private boolean isIndomitableSpiritActive() {
        int remainingCooldown = (int) getCooldowns()[Ability.INDOMITABLE_SPIRIT.ordinal()].getRemaining();
        return remainingCooldown >= INDOMITABLE_SPIRIT_COOLDOWN_SECONDS - INDOMITABLE_SPIRIT_ACTIVE_SECONDS;
    }

    /**
     * Converts a duration in seconds to Minecraft ticks.
     *
     * @param seconds the duration in seconds
     * @return the equivalent duration in ticks
     */
    private int toTicks(int seconds) {
        return seconds * 20;
    }

    /**
     * Creates and initializes the cooldown array for all Berserker abilities.
     *
     * @return the initialized cooldown array ordered by {@link Ability#ordinal()}
     */
    private static Cooldown[] createCooldowns() {
        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        cooldowns[Ability.HASTE.ordinal()] = new Cooldown(HASTE_COOLDOWN_SECONDS);
        cooldowns[Ability.RAGE.ordinal()] = new Cooldown(RAGE_COOLDOWN_SECONDS);
        cooldowns[Ability.INDOMITABLE_SPIRIT.ordinal()] = new Cooldown(INDOMITABLE_SPIRIT_COOLDOWN_SECONDS);
        return cooldowns;
    }

    /**
     * Creates the required armor mapping for Berserker ability use.
     *
     * @return a map of equipment slots to the required leather armor pieces
     */
    private static Map<EquipmentSlot, Material> createRequiredArmor() {
        Map<EquipmentSlot, Material> requiredArmor = new EnumMap<>(EquipmentSlot.class);
        requiredArmor.put(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE);
        requiredArmor.put(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS);
        return requiredArmor;
    }
}
