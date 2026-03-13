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

public class AssassinClass extends AbstractClass {

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

    private enum Ability {
        INVISIBILITY,
        POISONED_BLADE,
        HEROIC_DODGE
    }

    public AssassinClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

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

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!canUseAbilities()) {
            return;
        }

        castPoisonedBlade();
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!canUseAbilities()) {
            return;
        }

        castInvisibility();
    }

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

    private void castPoisonedBlade() {
        if (!isAbilityReady(Ability.POISONED_BLADE)) {
            return;
        }

        send(POISONED_BLADE_CAST_MESSAGE);
        restartCooldown(Ability.POISONED_BLADE);
    }

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

    private void applyPoisonedBladeEffects(LivingEntity target) {
        for (PotionEffectType effectType : BEHAVIOR.poisonedBladeEffects()) {
            target.addPotionEffect(new PotionEffect(
                    effectType,
                    toTicks(POISONED_BLADE_EFFECT_DURATION_SECONDS),
                    POISONED_BLADE_EFFECT_AMPLIFIER
            ));
        }
    }

    private boolean canUseAbilities() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    private boolean isAbilityReady(Ability ability) {
        return offCooldown(ability.ordinal());
    }

    private void restartCooldown(Ability ability) {
        getCooldowns()[ability.ordinal()].restart();
    }

    private float getRemainingCooldown(Ability ability) {
        return getCooldowns()[ability.ordinal()].getRemaining();
    }

    private int toTicks(int seconds) {
        return seconds * 20;
    }

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

    boolean shouldNegateIncomingDamage(float heroicDodgeRemaining, float invisibilityRemaining, double dodgeRoll) {
        return heroicDodgeRemaining >= HEROIC_DODGE_IMMUNITY_THRESHOLD
                || invisibilityRemaining >= INVISIBILITY_IMMUNITY_THRESHOLD
                || dodgeRoll <= RANDOM_DODGE_CHANCE;
    }

    double bonusDamage(boolean targetIsPlayer) {
        return targetIsPlayer ? PLAYER_BONUS_DAMAGE : NON_PLAYER_BONUS_DAMAGE;
    }

    boolean isPoisonedBladeActive(float poisonedBladeRemaining) {
        return poisonedBladeRemaining >= POISONED_BLADE_ACTIVE_THRESHOLD;
    }

    List<PotionEffectType> poisonedBladeEffects() {
        return POISONED_BLADE_EFFECTS;
    }

    boolean hasRequiredArmor(EquipmentSlot slot, Material material) {
        Material requiredMaterial = REQUIRED_ARMOR_BY_SLOT.get(slot);
        if (requiredMaterial == null) {
            return true;
        }
        return requiredMaterial == material;
    }

    private static Map<EquipmentSlot, Material> createRequiredArmorBySlot() {
        Map<EquipmentSlot, Material> requiredArmor = new EnumMap<>(EquipmentSlot.class);
        requiredArmor.put(EquipmentSlot.HEAD, Material.LEATHER_HELMET);
        requiredArmor.put(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE);
        requiredArmor.put(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS);
        requiredArmor.put(EquipmentSlot.FEET, Material.LEATHER_BOOTS);
        return requiredArmor;
    }
}
