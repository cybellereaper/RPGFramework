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

public class BerserkerClass extends AbstractClass {

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

    public BerserkerClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!isClassItem(type) || !canUseAbilities()) {
            return;
        }

        castRage();
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (!isClassItem(type) || !canUseAbilities()) {
            return;
        }

        castHaste();
    }

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

    @Override
    public void damaged(AdvancedDamageEvent event) {
        if (correctArmor()) {
            event.addDamage(-DEFENSIVE_DAMAGE_REDUCTION, event.getPrimaryDamage());
        }

        if (isIndomitableSpiritActive()) {
            event.setCancelled(true);
        }
    }

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

    private void castRage() {
        if (!isAbilityReady(Ability.RAGE)) {
            return;
        }

        send(ChatColor.GREEN + "You've used rage!");
        getPlayer().addPotionEffect(PotionEffectType.STRENGTH, toTicks(RAGE_DURATION_SECONDS), RAGE_AMPLIFIER);
        restartCooldown(Ability.RAGE);
    }

    private void castHaste() {
        if (!isAbilityReady(Ability.HASTE)) {
            return;
        }

        send(ChatColor.GREEN + "You've used haste!");
        getPlayer().addPotionEffect(PotionEffectType.SPEED, toTicks(HASTE_DURATION_SECONDS), HASTE_AMPLIFIER);
        restartCooldown(Ability.HASTE);
    }

    private boolean canUseAbilities() {
        if (correctArmor()) {
            return true;
        }

        send(ARMOR_REQUIRED_MESSAGE);
        return false;
    }

    protected boolean isClassItem(Material material) {
        return material == CLASS_ITEM;
    }

    private boolean isUsingAxe() {
        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null) {
            return false;
        }

        ItemStack heldItem = equipment.getItem(EquipmentSlot.HAND);
        return heldItem != null && VALID_AXES.contains(heldItem.getType());
    }

    private boolean isAbilityReady(Ability ability) {
        return offCooldown(ability.ordinal());
    }

    private void restartCooldown(Ability ability) {
        getCooldowns()[ability.ordinal()].restart();
    }

    private boolean isIndomitableSpiritActive() {
        int remainingCooldown = (int) getCooldowns()[Ability.INDOMITABLE_SPIRIT.ordinal()].getRemaining();
        return remainingCooldown >= INDOMITABLE_SPIRIT_COOLDOWN_SECONDS - INDOMITABLE_SPIRIT_ACTIVE_SECONDS;
    }

    private int toTicks(int seconds) {
        return seconds * 20;
    }

    private static Cooldown[] createCooldowns() {
        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        cooldowns[Ability.HASTE.ordinal()] = new Cooldown(HASTE_COOLDOWN_SECONDS);
        cooldowns[Ability.RAGE.ordinal()] = new Cooldown(RAGE_COOLDOWN_SECONDS);
        cooldowns[Ability.INDOMITABLE_SPIRIT.ordinal()] = new Cooldown(INDOMITABLE_SPIRIT_COOLDOWN_SECONDS);
        return cooldowns;
    }

    private static Map<EquipmentSlot, Material> createRequiredArmor() {
        Map<EquipmentSlot, Material> requiredArmor = new EnumMap<>(EquipmentSlot.class);
        requiredArmor.put(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE);
        requiredArmor.put(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS);
        return requiredArmor;
    }
}
