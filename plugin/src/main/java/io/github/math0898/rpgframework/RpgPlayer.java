package io.github.math0898.rpgframework;

import io.github.math0898.rpgframework.classes.AbstractClass;
import io.github.math0898.rpgframework.classes.Class;
import io.github.math0898.rpgframework.classes.Classes;
import io.github.math0898.rpgframework.classes.LuaScriptedClass;
import io.github.math0898.rpgframework.classes.implementations.NoneClass;
import io.github.math0898.rpgframework.enemies.CustomMob;
import io.github.math0898.rpgframework.items.EquipmentSlots;
import io.github.math0898.rpgframework.items.ItemManager;
import io.github.math0898.rpgframework.items.RpgItem;
import io.github.math0898.rpgframework.parties.Party;
import io.github.math0898.utils.StringUtils;
import io.github.math0898.utils.Utils;
import io.github.math0898.utils.items.AttributeBuilder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * RPG wrapper around a Bukkit player.
 */
public class RpgPlayer {

    private static final String MESSAGE_PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private static final long OUT_OF_COMBAT_DELAY_MILLIS = 10_000L;
    private static final int PASSIVE_INTERVAL_TICKS = 20 * 20;
    private static final double RPG_TO_MC_SCALAR = 5.0;
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double DEFAULT_HEALTH_SCALE = 20.0;

    private static final UUID LEVEL_HEALTH_MODIFIER_ID = new UUID(100, 234);
    private static final UUID LEVEL_DAMAGE_MODIFIER_ID = new UUID(100, 235);

    private static final double IN_COMBAT_REGEN_SCALE = 1.0;
    private static final double OUT_OF_COMBAT_REGEN_SCALE = 0.25;

    @Getter
    private final List<String> artifactCollection = new ArrayList<>();

    private long fightingSinceMillis = 0L;
    private boolean outOfCombatEffectsApplied = false;

    @Getter
    private Entity lastHitBy;

    @Getter
    private long experience = 0;

    @Getter
    private long level = 1;

    @Getter
    private final UUID uuid;

    @Setter
    @Getter
    private Party pendingParty;

    @Setter
    @Getter
    private Party party;

    @Getter
    private Classes combatClass = Classes.NONE;

    private Class classObject;

    private CustomMob activeBoss;

    @Getter
    private final Player bukkitPlayer;

    @Getter
    private final String name;

    public RpgPlayer(Player player) {
        this.uuid = player.getUniqueId();
        this.bukkitPlayer = player;
        this.name = player.getName();
        this.classObject = new NoneClass(this);
        refresh();
    }

    public void addCollectedArtifacts(List<String> collection) {
        if (collection == null || collection.isEmpty()) {
            return;
        }

        artifactCollection.addAll(collection);
        refresh();
    }

    public void sendMessage(String message) {
        sendMessage(message, true);
    }

    public void sendMessage(String message, boolean includePrefix) {
        String formattedMessage = includePrefix ? MESSAGE_PREFIX + message : message;
        getBukkitPlayer().sendMessage(formattedMessage);
    }

    public void setExperience(long experience) {
        this.experience = experience;
        recalculateLevel();
        refresh();
    }

    public void giveExperience(long awardedExperience) {
        long previousLevel = level;

        experience += awardedExperience;
        recalculateLevel();

        if (level > previousLevel) {
            handleLevelUp();
        }
    }

    public int getGearScore() {
        int gearScore = 0;

        for (ItemStack item : getBukkitPlayer().getInventory().getArmorContents()) {
            gearScore += calculateGearScore(item);
        }

        return gearScore;
    }

    public ChatColor getPlayerRarity() {
        int gearScore = getGearScore();

        if (gearScore <= 100) {
            return ChatColor.WHITE;
        }
        if (gearScore <= 200) {
            return ChatColor.GREEN;
        }
        if (gearScore <= 300) {
            return ChatColor.BLUE;
        }
        if (gearScore <= 400) {
            return ChatColor.GOLD;
        }
        if (gearScore <= 500) {
            return ChatColor.LIGHT_PURPLE;
        }
        return ChatColor.RED;
    }

    public void dropAll() {
        Player player = getBukkitPlayer();
        World world = player.getWorld();

        for (ItemStack item : player.getInventory()) {
            if (item == null) {
                continue;
            }

            try {
                world.dropItem(player.getLocation(), item);
            } catch (IllegalArgumentException ignored) {
            }
        }

        player.getInventory().clear();
    }

    public String getFormattedHealth() {
        double maxHealth = getMaxHealth();
        double currentHealth = getBukkitPlayer().getHealth();
        double healthRatio = currentHealth / maxHealth;

        ChatColor color = ChatColor.GREEN;
        if (healthRatio < 0.50) {
            color = ChatColor.RED;
        } else if (healthRatio < 0.75) {
            color = ChatColor.YELLOW;
        }

        return color + String.valueOf(currentHealth);
    }

    public void joinClass(Classes newClass) {
        combatClass = newClass;
        classObject = createClassInstance(newClass);
    }

    public String getArchetype() {
        return switch (combatClass) {
            case ASSASSIN, BERSERKER -> "Fighter";
            case NONE -> "None";
            default -> "Caster";
        };
    }

    public void onInteract(PlayerInteractEvent event) {
        classObject.onInteract(event);
    }

    public boolean revive() {
        return !classObject.onDeath();
    }

    public boolean inCombat() {
        return System.currentTimeMillis() - fightingSinceMillis < OUT_OF_COMBAT_DELAY_MILLIS;
    }

    public void damaged(EntityDamageEvent event) {
        classObject.damaged(event);

        if (!(event instanceof EntityDamageByEntityEvent damageByEntityEvent)) {
            return;
        }

        markCombatActivity(damageByEntityEvent.getDamager());
    }

    public void attacker(EntityDamageByEntityEvent event) {
        markCombatActivity(event.getEntity());
        classObject.attack(event);
    }

    public void passive() {
        try {
            classObject.passive();
            scheduleNextPassiveTick();

            if (!inCombat() && !outOfCombatEffectsApplied) {
                leaveCombat();
            }
        } catch (Exception ignored) {
        }
    }

    public void enteringCombat() {
        if (outOfCombatEffectsApplied) {
            getBukkitPlayer().sendMessage(ChatColor.RED + "Entering Combat");
        }

        PlayerManager.scaleRegen(getBukkitPlayer(), IN_COMBAT_REGEN_SCALE);
        outOfCombatEffectsApplied = false;
    }

    public void leaveCombat() {
        if (!outOfCombatEffectsApplied) {
            getBukkitPlayer().sendMessage(ChatColor.GREEN + "Left Combat");
            getBukkitPlayer().playSound(getBukkitPlayer(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        outOfCombatEffectsApplied = true;
        PlayerManager.scaleRegen(getBukkitPlayer(), OUT_OF_COMBAT_REGEN_SCALE);
    }

    public double getMaxHealth() {
        AttributeInstance attribute = getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute == null ? DEFAULT_MAX_HEALTH : attribute.getValue();
    }

    public void heal(double amount) {
        Player player = getBukkitPlayer();
        player.setHealth(Math.min(player.getHealth() + amount, getMaxHealth()));
    }

    public void heal() {
        getBukkitPlayer().setHealth(getMaxHealth());
    }

    public void resetCooldowns() {
        if (!(classObject instanceof AbstractClass abstractClass)) {
            return;
        }

        for (Cooldown cooldown : abstractClass.getCooldowns()) {
            if (cooldown != null) {
                cooldown.setComplete();
            }
        }
    }

    public void setBoss(CustomMob boss) {
        if (party != null) {
            party.setBoss(boss);
        }
        activeBoss = boss;
    }

    public CustomMob getActiveBossUnsafe() {
        if (party != null) {
            CustomMob partyBoss = party.getActiveBossUnsafe();
            if (partyBoss != null) {
                return partyBoss;
            }
        }

        return activeBoss;
    }

    public CustomMob getActiveBoss() {
        if (party != null) {
            CustomMob partyBoss = party.getActiveBoss();
            if (partyBoss != null) {
                return partyBoss;
            }
        }

        if (activeBoss != null && activeBoss.isSpawned()) {
            Entity bossEntity = activeBoss.getEntity();
            if (!bossEntity.isValid() || bossEntity.isDead()) {
                activeBoss = null;
            }
        }

        return activeBoss;
    }

    public List<RpgPlayer> friendlyCasterTargets() {
        if (party == null) {
            return List.of(this);
        }

        return new ArrayList<>(party.getRpgPlayers());
    }

    public List<LivingEntity> nearbyEnemyCasterTargets(double distance) {
        return nearbyEnemyCasterTargets(distance, distance, distance);
    }

    public List<LivingEntity> nearbyEnemyCasterTargets(double dx, double dy, double dz) {
        List<LivingEntity> nearbyEnemies = new ArrayList<>();
        List<LivingEntity> friendlyEntities = friendlyCasterTargets().stream()
                .map(RpgPlayer::getBukkitPlayer)
                .map(player -> (LivingEntity) player)
                .toList();

        for (Entity entity : getBukkitPlayer().getNearbyEntities(dx, dy, dz)) {
            if (entity instanceof LivingEntity livingEntity && !friendlyEntities.contains(livingEntity)) {
                nearbyEnemies.add(livingEntity);
            }
        }

        return nearbyEnemies;
    }

    public void addPotionEffect(PotionEffectType type, int duration, int level) {
        addPotionEffect(type, duration, level, false, false);
    }

    public void addPotionEffect(PotionEffectType type, int duration, int level, boolean ambient, boolean hideParticles) {
        getBukkitPlayer().addPotionEffect(new PotionEffect(type, duration, level - 1, ambient, hideParticles));
    }

    public void cleanseEffects(PotionEffectType... effects) {
        if (effects == null) {
            return;
        }

        Player player = getBukkitPlayer();
        for (PotionEffectType effect : effects) {
            player.removePotionEffect(effect);
        }
    }

    private void recalculateLevel() {
        long remainingThirtyPointChunks = experience / 30;
        long nextThreshold = 1;
        long computedLevel = 0;

        while (remainingThirtyPointChunks > 0) {
            remainingThirtyPointChunks -= nextThreshold;
            nextThreshold++;

            if (remainingThirtyPointChunks >= 0) {
                computedLevel++;
            }
        }

        level = computedLevel + 1;
    }

    private void handleLevelUp() {
        refresh();
        getBukkitPlayer().playSound(getBukkitPlayer(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.0f);
        sendMessage(ChatColor.GREEN + "You've leveled up! Level: " + getLevel());

        if (level % 5 == 0) {
            sendMessage(StringUtils.convertHexCodes("#D93747") + " +1 Damage");
            return;
        }

        sendMessage(StringUtils.convertHexCodes("#F454DA") + " +5 Health");
    }

    private void refresh() {
        refreshArtifactAttributes();
        refreshLevelAttributes();
    }

    private void refreshArtifactAttributes() {
        if (artifactCollection.isEmpty()) {
            return;
        }

        ItemManager itemManager = ItemManager.getInstance();
        ArtifactTotals artifactTotals = calculateArtifactTotals(itemManager);

        applyScaledAttributeModifier(Attribute.GENERIC_MAX_HEALTH, artifactTotals.health() / RPG_TO_MC_SCALAR);
        applyScaledAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, artifactTotals.damage() / RPG_TO_MC_SCALAR);
        applyScaledAttributeModifier(Attribute.GENERIC_ARMOR, artifactTotals.armor());
        applyScaledAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, artifactTotals.attackSpeed());
        applyScaledAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, artifactTotals.toughness());

        bukkitPlayer.setHealthScale(DEFAULT_HEALTH_SCALE);
        PlayerManager.scaleRegen(bukkitPlayer, IN_COMBAT_REGEN_SCALE);
    }

    private ArtifactTotals calculateArtifactTotals(ItemManager itemManager) {
        double health = 0.0;
        double damage = 0.0;
        double armor = 0.0;
        double toughness = 0.0;
        double attackSpeed = 0.0;

        for (String artifactId : artifactCollection) {
            RpgItem item = itemManager.getRpgItem(artifactId);
            if (item.getSlot() != EquipmentSlots.ARTIFACT) {
                continue;
            }

            health += item.getHealth();
            damage += item.getDamage();
            armor += item.getArmor();
            toughness += item.getToughness();
            attackSpeed += item.getAttackSpeed();
        }

        return new ArtifactTotals(health, damage, armor, toughness, attackSpeed);
    }

    private void refreshLevelAttributes() {
        Player player = getBukkitPlayer();

        updateAttribute(
                player,
                Attribute.GENERIC_MAX_HEALTH,
                LEVEL_HEALTH_MODIFIER_ID,
                healthBonus(level) / RPG_TO_MC_SCALAR,
                attribute -> {
                    player.setHealth(attribute.getValue());
                    player.setHealthScale(DEFAULT_HEALTH_SCALE);
                    return null;
                },
                "health"
        );

        updateAttribute(
                player,
                Attribute.GENERIC_ATTACK_DAMAGE,
                LEVEL_DAMAGE_MODIFIER_ID,
                damageBonus(level) / RPG_TO_MC_SCALAR,
                attribute -> {
                    return null;
                },
                "damage"
        );
    }

    private void updateAttribute(
            Player player,
            Attribute attribute,
            UUID modifierId,
            double amount,
            Function<AttributeInstance, Void> afterUpdate,
            String label
    ) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            RPGFramework.console(
                    "Attempted to update " + player.getName() + "'s " + label + " but " + attribute + " instance is null.",
                    ChatColor.RED
            );
            return;
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierId,
                "",
                amount,
                AttributeModifier.Operation.ADD_NUMBER
        );

        removeMatchingModifier(instance, modifier);
        instance.addModifier(modifier);
        afterUpdate.apply(instance);
    }

    private void applyScaledAttributeModifier(Attribute attribute, double amount) {
        if (amount == 0.0) {
            return;
        }

        AttributeInstance instance = bukkitPlayer.getAttribute(attribute);
        if (instance == null) {
            return;
        }

        AttributeModifier modifier = AttributeBuilder.attributeModifier(attribute, amount, null);
        instance.removeModifier(modifier);
        instance.addModifier(modifier);
    }

    private void removeMatchingModifier(AttributeInstance instance, AttributeModifier modifier) {
        Collection<AttributeModifier> modifiers = instance.getModifiers();
        if (!modifiers.isEmpty()) {
            instance.removeModifier(modifier);
        }
    }

    private int calculateGearScore(ItemStack item) {
        if (item == null) {
            return 0;
        }

        int score = item.getEnchantments().size() > 1 ? 15 : 0;

        if (isModifiedVanilla(item)) {
            return score + getModifiedVanillaArmorScore(item.getType());
        }

        return score + 20;
    }

    private boolean isModifiedVanilla(ItemStack item) {
        try {
            return Objects.requireNonNull(Objects.requireNonNull(item.getItemMeta()).getLore()).contains("Modified Vanilla");
        } catch (NullPointerException ignored) {
            return false;
        }
    }

    private int getModifiedVanillaArmorScore(org.bukkit.Material material) {
        return switch (material) {
            case LEATHER_BOOTS, LEATHER_LEGGINGS, LEATHER_CHESTPLATE, LEATHER_HELMET -> 5;
            case CHAINMAIL_BOOTS, CHAINMAIL_LEGGINGS, CHAINMAIL_CHESTPLATE, CHAINMAIL_HELMET -> 6;
            case GOLDEN_BOOTS, GOLDEN_LEGGINGS, GOLDEN_CHESTPLATE, GOLDEN_HELMET -> 8;
            case IRON_BOOTS, IRON_LEGGINGS, IRON_CHESTPLATE, IRON_HELMET -> 10;
            case DIAMOND_BOOTS, DIAMOND_LEGGINGS, DIAMOND_CHESTPLATE, DIAMOND_HELMET -> 13;
            case NETHERITE_BOOTS, NETHERITE_LEGGINGS, NETHERITE_CHESTPLATE, NETHERITE_HELMET -> 15;
            default -> 0;
        };
    }

    private Class createClassInstance(Classes selectedClass) {
        return switch (selectedClass) {
            case NONE -> new NoneClass(this);
            default -> new LuaScriptedClass(this, selectedClass.name());
        };
    }

    private void markCombatActivity(Entity opponent) {
        fightingSinceMillis = System.currentTimeMillis();
        enteringCombat();
        lastHitBy = opponent;
    }

    private void scheduleNextPassiveTick() {
        JavaPlugin plugin = Utils.getPlugin();
        Bukkit.getScheduler().runTaskLater(plugin, this::passive, PASSIVE_INTERVAL_TICKS);
    }

    private static double healthBonus(long level) {
        return ((level - 1) - (level / 5.0)) * 5;
    }

    private static double damageBonus(long level) {
        return level / 5.0;
    }

    private record ArtifactTotals(
            double health,
            double damage,
            double armor,
            double toughness,
            double attackSpeed
    ) {
    }
}
