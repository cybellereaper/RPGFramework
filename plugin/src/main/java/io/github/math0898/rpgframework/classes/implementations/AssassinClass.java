package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.LuaBackedClass;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AssassinClass extends LuaBackedClass {

    static final AssassinBehavior BEHAVIOR = new AssassinBehavior();

    private static final String ARMOR_REQUIRED_MESSAGE = "Use full leather armor to use assassin abilities.";
    private static final int INVISIBILITY_DURATION_SECONDS = 10;
    private static final int POISONED_BLADE_EFFECT_DURATION_SECONDS = 10;
    private static final int HEROIC_DODGE_SPEED_DURATION_SECONDS = 10;
    private static final int PASSIVE_SPEED_DURATION_SECONDS = 21;

    private static final String POISONED_BLADE_CAST_MESSAGE = ChatColor.GREEN + "You've used poisoned blade!";
    private static final String INVISIBILITY_CAST_MESSAGE = ChatColor.GREEN + "You've used invisibility!";
    private static final String HEROIC_DODGE_CAST_MESSAGE =
            ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Heroic Dodge" + ChatColor.GREEN + "!";

    private enum Ability { INVISIBILITY, POISONED_BLADE, HEROIC_DODGE }

    public AssassinClass(RpgPlayer player) {
        super(player, "ASSASSIN");
    }

    @Override
    public void damaged(AdvancedDamageEvent event) {
        if (BEHAVIOR.shouldNegateIncomingDamage(
                getRemainingCooldown(Ability.HEROIC_DODGE),
                getRemainingCooldown(Ability.INVISIBILITY),
                ThreadLocalRandom.current().nextDouble()
        )) {
            event.setCancelled(true);
        }
    }

    @Override
    public void attack(AdvancedDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !event.getPrimaryDamage().isPhysical()) {
            return;
        }

        event.addDamage(BEHAVIOR.bonusDamage(target instanceof Player), DamageType.SLASH);

        if (BEHAVIOR.isPoisonedBladeActive(getRemainingCooldown(Ability.POISONED_BLADE))) {
            applyPoisonedBladeEffects(target);
        }
    }

    @Override
    public void passive() {
        if (!correctArmor()) {
            return;
        }

        getPlayer().addPotionEffect(PotionEffectType.SPEED, secondsToTicks(PASSIVE_SPEED_DURATION_SECONDS), 1, true, false);
    }

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (canUseAbilities() && offCooldown(Ability.POISONED_BLADE)) {
            send(POISONED_BLADE_CAST_MESSAGE);
            restartCooldown(Ability.POISONED_BLADE);
        }
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!canUseAbilities() || !offCooldown(Ability.INVISIBILITY)) {
            return;
        }

        send(INVISIBILITY_CAST_MESSAGE);
        getPlayer().addPotionEffect(PotionEffectType.INVISIBILITY, secondsToTicks(INVISIBILITY_DURATION_SECONDS), 1);
        restartCooldown(Ability.INVISIBILITY);
    }

    @Override
    public boolean onDeath() {
        if (!correctArmor() || !offCooldown(Ability.HEROIC_DODGE)) {
            return true;
        }

        Player player = getPlayer().getBukkitPlayer();
        send(HEROIC_DODGE_CAST_MESSAGE);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
        getPlayer().addPotionEffect(PotionEffectType.SPEED, secondsToTicks(HEROIC_DODGE_SPEED_DURATION_SECONDS), 2);
        restartCooldown(Ability.HEROIC_DODGE);
        return false;
    }

    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        return hasRequiredArmor(slot);
    }

    private boolean canUseAbilities() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    private void applyPoisonedBladeEffects(LivingEntity target) {
        for (PotionEffectType effectType : BEHAVIOR.poisonedBladeEffects()) {
            target.addPotionEffect(new PotionEffect(effectType, secondsToTicks(POISONED_BLADE_EFFECT_DURATION_SECONDS), 0));
        }
    }
}

final class AssassinBehavior {

    private static final double PLAYER_BONUS_DAMAGE = 5.0;
    private static final double NON_PLAYER_BONUS_DAMAGE = 10.0;
    private static final double RANDOM_DODGE_CHANCE = 0.10;
    private static final float HEROIC_DODGE_IMMUNITY_THRESHOLD = 290;
    private static final float INVISIBILITY_IMMUNITY_THRESHOLD = 20;
    private static final float POISONED_BLADE_ACTIVE_THRESHOLD = 50;


    private static final Map<EquipmentSlot, Material> REQUIRED_ARMOR_BY_SLOT = createRequiredArmorBySlot();

    private static final List<PotionEffectType> POISONED_BLADE_EFFECTS = Arrays.asList(
            PotionEffectType.BLINDNESS,
            PotionEffectType.POISON,
            PotionEffectType.SLOWNESS
    );

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
        return requiredMaterial == null || requiredMaterial == material;
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
