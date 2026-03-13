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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the Assassin class.
 *
 * <p>The Assassin is a mobility-focused damage class built around stealth,
 * burst damage, poison-based debuffs, and brief windows of near-immunity.</p>
 *
 * <p>Ability use requires the class item and a full leather armor set.</p>
 */
public class AssassinClass extends AbstractClass {

    /**
     * Shared behavior and rule set for Assassin-specific calculations.
     */
    static final AssassinBehavior BEHAVIOR = new AssassinBehavior();

    private static final Material CLASS_ITEM = Material.GHAST_TEAR;
    private static final String ARMOR_REQUIRED_MESSAGE = "Use full leather armor to use assassin abilities.";

    private static final int INVISIBILITY_DURATION_SECONDS = 10;
    private static final int POISONED_BLADE_EFFECT_DURATION_SECONDS = 10;
    private static final int HEROIC_DODGE_SPEED_DURATION_SECONDS = 10;
    private static final int PASSIVE_SPEED_DURATION_SECONDS = 21;

    private static final int INVISIBILITY_AMPLIFIER = 1;
    private static final int HEROIC_DODGE_SPEED_AMPLIFIER = 2;
    private static final int PASSIVE_SPEED_AMPLIFIER = 1;
    private static final int POISONED_BLADE_EFFECT_AMPLIFIER = 0;

    private static final String POISONED_BLADE_CAST_MESSAGE = ChatColor.GREEN + "You've used poisoned blade!";
    private static final String INVISIBILITY_CAST_MESSAGE = ChatColor.GREEN + "You've used invisibility!";
    private static final String HEROIC_DODGE_CAST_MESSAGE =
            ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Heroic Dodge" + ChatColor.GREEN + "!";

    /**
     * Enumerates the Assassin's abilities in cooldown index order.
     */
    private enum Ability {
        INVISIBILITY,
        POISONED_BLADE,
        HEROIC_DODGE
    }

    /**
     * Constructs a new {@code AssassinClass} for the specified player.
     *
     * @param player the RPG player that owns this class instance
     */
    public AssassinClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    /**
     * Applies the Assassin's defensive effects when taking damage.
     *
     * <p>Incoming damage may be negated while Heroic Dodge or Invisibility is
     * active, or through the Assassin's random dodge chance.</p>
     *
     * @param event the incoming advanced damage event
     */
    @Override
    public void damaged(AdvancedDamageEvent event) {
        double dodgeRoll = ThreadLocalRandom.current().nextDouble();

        if (BEHAVIOR.shouldNegateIncomingDamage(
                getRemainingCooldown(Ability.HEROIC_DODGE),
                getRemainingCooldown(Ability.INVISIBILITY),
                dodgeRoll
        )) {
            event.setCancelled(true);
        }
    }

    /**
     * Applies the Assassin's offensive attack modifiers.
     *
     * <p>Physical attacks gain bonus slash damage. While Poisoned Blade is active,
     * successful attacks also apply additional debuff effects to the target.</p>
     *
     * @param event the outgoing advanced damage event
     */
    @Override
    public void attack(AdvancedDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (!event.getPrimaryDamage().isPhysical()) {
            return;
        }

        event.addDamage(BEHAVIOR.bonusDamage(target instanceof Player), DamageType.SLASH);

        if (!BEHAVIOR.isPoisonedBladeActive(getRemainingCooldown(Ability.POISONED_BLADE))) {
            return;
        }

        applyPoisonedBladeEffects(target);
    }

    /**
     * Applies the Assassin's passive movement bonus while the required armor is worn.
     */
    @Override
    public void passive() {
        if (!correctArmor()) {
            return;
        }

        getPlayer().addPotionEffect(
                PotionEffectType.SPEED,
                toTicks(PASSIVE_SPEED_DURATION_SECONDS),
                PASSIVE_SPEED_AMPLIFIER,
                true,
                false
        );
    }

    /**
     * Handles left-click ability casting for the Assassin.
     *
     * <p>If the Assassin is wearing the required armor, this casts Poisoned Blade.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the ability
     */
    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!canUseAbilities()) {
            return;
        }

        castPoisonedBlade();
    }

    /**
     * Handles right-click ability casting for the Assassin.
     *
     * <p>If the Assassin is wearing the required armor, this casts Invisibility.</p>
     *
     * @param event the interaction event that triggered the cast
     * @param type the item used to cast the ability
     */
    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!canUseAbilities()) {
            return;
        }

        castInvisibility();
    }

    /**
     * Handles the Assassin's death-prevention passive.
     *
     * <p>If the Assassin is wearing the required armor and Heroic Dodge is off
     * cooldown, death is prevented and the player gains a temporary Speed boost.</p>
     *
     * @return {@code false} if death is prevented; {@code true} if the player should die normally
     */
    @Override
    public boolean onDeath() {
        if (!correctArmor()) {
            return true;
        }

        if (!isAbilityReady(Ability.HEROIC_DODGE)) {
            return true;
        }

        Player player = getPlayer().getBukkitPlayer();

        send(HEROIC_DODGE_CAST_MESSAGE);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
        getPlayer().addPotionEffect(
                PotionEffectType.SPEED,
                toTicks(HEROIC_DODGE_SPEED_DURATION_SECONDS),
                HEROIC_DODGE_SPEED_AMPLIFIER
        );
        restartCooldown(Ability.HEROIC_DODGE);

        return false;
    }

    /**
     * Determines whether the Assassin is wearing the correct armor piece in the given slot.
     *
     * @param slot the armor slot to validate
     * @return {@code true} if the slot contains the required leather armor piece;
     *         {@code false} otherwise
     */
    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null) {
            return false;
        }

        ItemStack equippedItem = equipment.getItem(slot);
        if (equippedItem == null) {
            return false;
        }

        return BEHAVIOR.hasRequiredArmor(slot, equippedItem.getType());
    }

    /**
     * Casts Poisoned Blade.
     *
     * <p>Poisoned Blade enables the Assassin's attack debuff effects for its active window.</p>
     */
    private void castPoisonedBlade() {
        if (!isAbilityReady(Ability.POISONED_BLADE)) {
            return;
        }

        send(POISONED_BLADE_CAST_MESSAGE);
        restartCooldown(Ability.POISONED_BLADE);
    }

    /**
     * Casts Invisibility.
     *
     * <p>Invisibility grants the Assassin temporary invisibility and contributes
     * to the class's damage-negation behavior while active.</p>
     */
    private void castInvisibility() {
        if (!isAbilityReady(Ability.INVISIBILITY)) {
            return;
        }

        send(INVISIBILITY_CAST_MESSAGE);
        getPlayer().addPotionEffect(
                PotionEffectType.INVISIBILITY,
                toTicks(INVISIBILITY_DURATION_SECONDS),
                INVISIBILITY_AMPLIFIER
        );
        restartCooldown(Ability.INVISIBILITY);
    }

    /**
     * Applies Poisoned Blade's configured debuff effects to a target.
     *
     * @param target the target to receive the debuffs
     */
    private void applyPoisonedBladeEffects(LivingEntity target) {
        for (PotionEffectType effectType : BEHAVIOR.poisonedBladeEffects()) {
            target.addPotionEffect(new PotionEffect(
                    effectType,
                    toTicks(POISONED_BLADE_EFFECT_DURATION_SECONDS),
                    POISONED_BLADE_EFFECT_AMPLIFIER
            ));
        }
    }

    /**
     * Determines whether the Assassin is currently allowed to use abilities.
     *
     * <p>The Assassin must be wearing the full required leather armor set.</p>
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
     * Gets the remaining cooldown time for the specified ability.
     *
     * @param ability the ability whose cooldown should be queried
     * @return the remaining cooldown time in seconds
     */
    private float getRemainingCooldown(Ability ability) {
        return getCooldowns()[ability.ordinal()].getRemaining();
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
     * Creates and initializes the cooldown array for all Assassin abilities.
     *
     * @return the initialized cooldown array ordered by {@link Ability#ordinal()}
     */
    private static Cooldown[] createCooldowns() {
        Map<Ability, Integer> cooldownsByAbility = new EnumMap<>(Ability.class);
        cooldownsByAbility.put(Ability.HEROIC_DODGE, 300);
        cooldownsByAbility.put(Ability.POISONED_BLADE, 60);
        cooldownsByAbility.put(Ability.INVISIBILITY, 30);

        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        for (Ability ability : Ability.values()) {
            cooldowns[ability.ordinal()] = new Cooldown(cooldownsByAbility.get(ability));
        }

        return cooldowns;
    }
}

/**
 * Encapsulates Assassin-specific combat rules, thresholds, and equipment requirements.
 */
final class AssassinBehavior {

    private static final double PLAYER_BONUS_DAMAGE = 5.0;
    private static final double NON_PLAYER_BONUS_DAMAGE = 10.0;
    private static final double RANDOM_DODGE_CHANCE = 0.10;

    private static final float HEROIC_DODGE_IMMUNITY_THRESHOLD = 290;
    private static final float INVISIBILITY_IMMUNITY_THRESHOLD = 20;
    private static final float POISONED_BLADE_ACTIVE_THRESHOLD = 50;

    private static final List<PotionEffectType> POISONED_BLADE_EFFECTS = List.of(
            PotionEffectType.BLINDNESS,
            PotionEffectType.POISON,
            PotionEffectType.SLOWNESS
    );

    private static final Map<EquipmentSlot, Material> REQUIRED_ARMOR_BY_SLOT = createRequiredArmorBySlot();

    /**
     * Determines whether incoming damage should be negated.
     *
     * @param heroicDodgeRemaining remaining Heroic Dodge cooldown time
     * @param invisibilityRemaining remaining Invisibility cooldown time
     * @param dodgeRoll random roll used for passive dodge evaluation
     * @return {@code true} if incoming damage should be negated; {@code false} otherwise
     */
    boolean shouldNegateIncomingDamage(float heroicDodgeRemaining, float invisibilityRemaining, double dodgeRoll) {
        return heroicDodgeRemaining >= HEROIC_DODGE_IMMUNITY_THRESHOLD
                || invisibilityRemaining >= INVISIBILITY_IMMUNITY_THRESHOLD
                || dodgeRoll <= RANDOM_DODGE_CHANCE;
    }

    /**
     * Gets the Assassin's bonus attack damage based on the target type.
     *
     * @param targetIsPlayer {@code true} if the target is a player; {@code false} otherwise
     * @return the additional damage to apply
     */
    double bonusDamage(boolean targetIsPlayer) {
        return targetIsPlayer ? PLAYER_BONUS_DAMAGE : NON_PLAYER_BONUS_DAMAGE;
    }

    /**
     * Determines whether Poisoned Blade is currently active.
     *
     * @param poisonedBladeRemaining remaining Poisoned Blade cooldown time
     * @return {@code true} if Poisoned Blade is active; {@code false} otherwise
     */
    boolean isPoisonedBladeActive(float poisonedBladeRemaining) {
        return poisonedBladeRemaining >= POISONED_BLADE_ACTIVE_THRESHOLD;
    }

    /**
     * Gets the potion effect types applied by Poisoned Blade.
     *
     * @return the list of Poisoned Blade effect types
     */
    List<PotionEffectType> poisonedBladeEffects() {
        return POISONED_BLADE_EFFECTS;
    }

    /**
     * Determines whether the provided armor piece satisfies the requirement for the given slot.
     *
     * @param slot the equipment slot to validate
     * @param material the material equipped in that slot
     * @return {@code true} if the material satisfies the slot requirement; {@code false} otherwise
     */
    boolean hasRequiredArmor(EquipmentSlot slot, Material material) {
        Material requiredMaterial = REQUIRED_ARMOR_BY_SLOT.get(slot);
        if (requiredMaterial == null) {
            return true;
        }
        return requiredMaterial == material;
    }

    /**
     * Creates the required armor mapping for Assassin ability use.
     *
     * @return a map of equipment slots to the required leather armor pieces
     */
    private static Map<EquipmentSlot, Material> createRequiredArmorBySlot() {
        Map<EquipmentSlot, Material> requiredArmor = new EnumMap<>(EquipmentSlot.class);
        requiredArmor.put(EquipmentSlot.HEAD, Material.LEATHER_HELMET);
        requiredArmor.put(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE);
        requiredArmor.put(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS);
        requiredArmor.put(EquipmentSlot.FEET, Material.LEATHER_BOOTS);
        return requiredArmor;
    }
}
