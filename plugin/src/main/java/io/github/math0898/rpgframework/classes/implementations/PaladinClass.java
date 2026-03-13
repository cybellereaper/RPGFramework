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

public class PaladinClass extends AbstractClass {

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

    public PaladinClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!isSupportedClassItem(type) || !canCastSpells()) {
            return;
        }

        castMend();
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!isSupportedClassItem(type) || !canCastSpells()) {
            return;
        }

        castPurify();
    }

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

    @Override
    public void damaged(AdvancedDamageEvent event) {
        for (DamageType damageType : PALADIN_RESISTANCES) {
            event.setResistance(damageType, DamageResistance.RESISTANCE);
        }
    }

    @Override
    public void attack(AdvancedDamageEvent event) {
        event.addDamage(-2.5, event.getPrimaryDamage());
    }

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

    private boolean canCastSpells() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    private boolean isSupportedClassItem(Material material) {
        return material == CLASS_ITEM;
    }

    private boolean isAbilityReady(Ability ability) {
        return offCooldown(ability.ordinal());
    }

    private void restartCooldown(Ability ability) {
        getCooldowns()[ability.ordinal()].restart();
    }

    private String getCasterDisplayName() {
        RpgPlayer player = getPlayer();
        return player.getPlayerRarity() + player.getName();
    }

    private int toTicks(int seconds) {
        return seconds * 20;
    }

    private static Cooldown[] createCooldowns() {
        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        cooldowns[Ability.MEND.ordinal()] = new Cooldown(MEND_COOLDOWN_SECONDS);
        cooldowns[Ability.PURIFY.ordinal()] = new Cooldown(PURIFY_COOLDOWN_SECONDS);
        cooldowns[Ability.PROTECTION_OF_THE_HEALER.ordinal()] = new Cooldown(PROTECTION_COOLDOWN_SECONDS);
        return cooldowns;
    }

    private static Map<EquipmentSlot, Material> createRequiredArmorBySlot() {
        Map<EquipmentSlot, Material> armorBySlot = new EnumMap<>(EquipmentSlot.class);
        armorBySlot.put(EquipmentSlot.HEAD, Material.GOLDEN_HELMET);
        armorBySlot.put(EquipmentSlot.CHEST, Material.GOLDEN_CHESTPLATE);
        armorBySlot.put(EquipmentSlot.LEGS, Material.GOLDEN_LEGGINGS);
        armorBySlot.put(EquipmentSlot.FEET, Material.GOLDEN_BOOTS);
        return armorBySlot;
    }
}
