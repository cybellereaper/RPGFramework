package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.AbstractClass;
import io.github.math0898.rpgframework.damage.DamageResistance;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the Paladin class.
 *
 * <p>The Paladin is a defensive support class that trades offensive power for
 * healing, cleansing, ally protection, and broad physical and holy resistance.</p>
 *
 * <p>Spellcasting requires a full set of golden armor and uses a golden shovel
 * as the class item.</p>
 */
public class PaladinClass extends AbstractClass {

    /**
     * Enumerates the Paladin's abilities in cooldown index order.
     */
    private enum Ability {
        PURIFY,
        MEND,
        PROTECTION_OF_THE_HEALER
    }

    private static final Material CLASS_ITEM = Material.GOLDEN_SHOVEL;
    private static final String ARMOR_REQUIRED_MESSAGE = "Use full golden armor to use paladin spells.";

    private static final int MEND_COOLDOWN_SECONDS = 60;
    private static final int MEND_DURATION_SECONDS = 15;
    private static final int MEND_AMPLIFIER = 3;

    private static final int PURIFY_COOLDOWN_SECONDS = 45;
    private static final double PURIFY_HEAL_AMOUNT = 5.0;

    private static final int PROTECTION_COOLDOWN_SECONDS = 300;
    private static final int PROTECTION_DURATION_SECONDS = 10;
    private static final int PROTECTION_REGEN_AMPLIFIER = 4;
    private static final int PROTECTION_HEALTH_BOOST_AMPLIFIER = 1;
    private static final double PROTECTION_HEAL_AMOUNT = 2.0;

    private static final DamageType[] PALADIN_RESISTANCES = {
            DamageType.HOLY,
            DamageType.IMPACT,
            DamageType.SLASH,
            DamageType.UNSPECIFIED,
            DamageType.PUNCTURE
    };

    private static final PotionEffectType[] NEGATIVE_EFFECTS_TO_CLEANSE = {
            PotionEffectType.BLINDNESS,
            PotionEffectType.BAD_OMEN,
            PotionEffectType.NAUSEA,
            PotionEffectType.DARKNESS,
            PotionEffectType.INSTANT_DAMAGE,
            PotionEffectType.HUNGER,
            PotionEffectType.POISON,
            PotionEffectType.SLOWNESS,
            PotionEffectType.LEVITATION,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.UNLUCK,
            PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER
    };

    private static final Map<EquipmentSlot, Material> REQUIRED_ARMOR_BY_SLOT = createRequiredArmorBySlot();

    /**
     * Constructs a new {@code PaladinClass} for the specified player.
     *
     * @param player the RPG player that owns this class instance
     */
    public PaladinClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    /**
     * Handles left-click spellcasting for the Paladin.
     *
     * <p>If the Paladin is using the proper class item and wearing the required
     * armor set, this casts Mend.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the spell
     */
    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!isSupportedClassItem(type) || !canCastSpells()) {
            return;
        }

        castMend();
    }

    /**
     * Handles right-click spellcasting for the Paladin.
     *
     * <p>If the Paladin is using the proper class item and wearing the required
     * armor set, this casts Purify.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the spell
     */
    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!isSupportedClassItem(type) || !canCastSpells()) {
            return;
        }

        castPurify();
    }

    /**
     * Handles the Paladin's death-prevention passive.
     *
     * <p>If Protection of the Healer is off cooldown, death is prevented and all
     * friendly caster targets receive strong temporary defensive recovery effects.</p>
     *
     * @return {@code false} if death is prevented; {@code true} if the player should die normally
     */
    @Override
    public boolean onDeath() {
        if (!isAbilityReady(Ability.PROTECTION_OF_THE_HEALER)) {
            return true;
        }

        RpgPlayer paladin = getPlayer();
        Player bukkitPlayer = paladin.getBukkitPlayer();
        List<RpgPlayer> allies = paladin.friendlyCasterTargets();
        String casterDisplayName = getCasterDisplayName();

        send(ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Protection of the Healer" + ChatColor.GREEN + "!");
        bukkitPlayer.getWorld().playSound(bukkitPlayer.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);

        for (RpgPlayer ally : allies) {
            applyProtectionOfTheHealer(ally, casterDisplayName);
        }

        restartCooldown(Ability.PROTECTION_OF_THE_HEALER);
        return false;
    }

    /**
     * Determines whether the Paladin is wearing the correct armor piece in the given slot.
     *
     * @param slot the armor slot to validate
     * @return {@code true} if the slot either has no requirement or contains the required armor piece;
     *         {@code false} otherwise
     */
    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        Material requiredMaterial = REQUIRED_ARMOR_BY_SLOT.get(slot);
        if (requiredMaterial == null) {
            return true;
        }

        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null || equipment.getItem(slot) == null) {
            return false;
        }

        return equipment.getItem(slot).getType() == requiredMaterial;
    }

    /**
     * Applies the Paladin's passive damage resistances.
     *
     * @param event the incoming advanced damage event
     */
    @Override
    public void damaged(AdvancedDamageEvent event) {
        for (DamageType damageType : PALADIN_RESISTANCES) {
            event.setResistance(damageType, DamageResistance.RESISTANCE);
        }
    }

    /**
     * Applies the Paladin's reduced offensive output to outgoing attacks.
     *
     * @param event the outgoing advanced damage event
     */
    @Override
    public void attack(AdvancedDamageEvent event) {
        event.addDamage(-2.5, event.getPrimaryDamage());
    }

    /**
     * Casts Mend.
     *
     * <p>Mend grants regeneration to all friendly caster targets for a fixed duration.</p>
     */
    private void castMend() {
        if (!isAbilityReady(Ability.MEND)) {
            return;
        }

        RpgPlayer paladin = getPlayer();
        List<RpgPlayer> allies = paladin.friendlyCasterTargets();
        String casterDisplayName = getCasterDisplayName();

        send(ChatColor.GREEN + "You've used mend!");

        for (RpgPlayer ally : allies) {
            ally.addPotionEffect(
                    PotionEffectType.REGENERATION,
                    toTicks(MEND_DURATION_SECONDS),
                    MEND_AMPLIFIER
            );
            ally.sendMessage(casterDisplayName + ChatColor.GREEN + " has used mend!");
        }

        restartCooldown(Ability.MEND);
    }

    /**
     * Casts Purify.
     *
     * <p>Purify heals all friendly caster targets, extinguishes them, and removes
     * a predefined set of negative potion effects.</p>
     */
    private void castPurify() {
        if (!isAbilityReady(Ability.PURIFY)) {
            return;
        }

        RpgPlayer paladin = getPlayer();
        List<RpgPlayer> allies = paladin.friendlyCasterTargets();
        String casterDisplayName = getCasterDisplayName();

        send(ChatColor.GREEN + "You've used purify!");

        for (RpgPlayer ally : allies) {
            ally.heal(PURIFY_HEAL_AMOUNT);
            ally.getBukkitPlayer().setFireTicks(0);
            ally.cleanseEffects(NEGATIVE_EFFECTS_TO_CLEANSE);
            ally.sendMessage(casterDisplayName + ChatColor.GREEN + " has used purify!");
        }

        restartCooldown(Ability.PURIFY);
    }

    /**
     * Applies Protection of the Healer's effects to a single ally.
     *
     * @param ally the ally receiving the protection effects
     * @param casterDisplayName the formatted display name of the Paladin caster
     */
    private void applyProtectionOfTheHealer(RpgPlayer ally, String casterDisplayName) {
        ally.addPotionEffect(
                PotionEffectType.REGENERATION,
                toTicks(PROTECTION_DURATION_SECONDS),
                PROTECTION_REGEN_AMPLIFIER
        );
        ally.addPotionEffect(
                PotionEffectType.HEALTH_BOOST,
                toTicks(PROTECTION_DURATION_SECONDS),
                PROTECTION_HEALTH_BOOST_AMPLIFIER
        );
        ally.heal(PROTECTION_HEAL_AMOUNT);
        ally.sendMessage(
                casterDisplayName
                        + ChatColor.GREEN
                        + " has used "
                        + ChatColor.GOLD
                        + "Protection of the Healer"
                        + ChatColor.GREEN
                        + "!"
        );
    }

    /**
     * Determines whether the Paladin is currently allowed to cast spells.
     *
     * <p>The Paladin must be wearing the full required golden armor set.</p>
     *
     * @return {@code true} if spellcasting is allowed; {@code false} otherwise
     */
    private boolean canCastSpells() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    /**
     * Determines whether the given material is the Paladin's supported class item.
     *
     * @param material the material to test
     * @return {@code true} if the material is the class item; {@code false} otherwise
     */
    private boolean isSupportedClassItem(Material material) {
        return material == CLASS_ITEM;
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
     * Gets the formatted display name used in ally notifications.
     *
     * @return the caster's rarity-prefixed display name
     */
    private String getCasterDisplayName() {
        RpgPlayer player = getPlayer();
        return player.getPlayerRarity() + player.getName();
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
     * Creates and initializes the cooldown array for all Paladin abilities.
     *
     * @return the initialized cooldown array ordered by {@link Ability#ordinal()}
     */
    private static Cooldown[] createCooldowns() {
        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        cooldowns[Ability.MEND.ordinal()] = new Cooldown(MEND_COOLDOWN_SECONDS);
        cooldowns[Ability.PURIFY.ordinal()] = new Cooldown(PURIFY_COOLDOWN_SECONDS);
        cooldowns[Ability.PROTECTION_OF_THE_HEALER.ordinal()] = new Cooldown(PROTECTION_COOLDOWN_SECONDS);
        return cooldowns;
    }

    /**
     * Creates the required armor mapping for Paladin spellcasting.
     *
     * @return a map of equipment slots to the required golden armor pieces
     */
    private static Map<EquipmentSlot, Material> createRequiredArmorBySlot() {
        Map<EquipmentSlot, Material> armorBySlot = new EnumMap<>(EquipmentSlot.class);
        armorBySlot.put(EquipmentSlot.HEAD, Material.GOLDEN_HELMET);
        armorBySlot.put(EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE);
        armorBySlot.put(EquipmentSlot.LEGS, Material.GOLDEN_LEGGINGS);
        armorBySlot.put(EquipmentSlot.FEET, Material.GOLDEN_BOOTS);
        return armorBySlot;
    }
}
