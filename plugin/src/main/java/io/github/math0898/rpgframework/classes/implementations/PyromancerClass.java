package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.AbstractClass;
import io.github.math0898.rpgframework.damage.DamageResistance;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the Pyromancer class.
 *
 * <p>The Pyromancer is an offensive fire-based class that specializes in burn effects,
 * bonus fire damage, self-sustain, and a single death-prevention effect through
 * Phoenix Renewal.</p>
 *
 * <p>Ability bindings:</p>
 * <ul>
 *     <li>Left-click with blaze powder: Scorch</li>
 *     <li>Right-click with blaze powder: Kindle</li>
 *     <li>Left-click with blaze rod: Flare Volley</li>
 *     <li>Right-click with blaze rod: Cauterize</li>
 * </ul>
 */
public class PyromancerClass extends AbstractClass {

    /**
     * Enumerates the Pyromancer's abilities in cooldown index order.
     */
    private enum Ability {
        SCORCH,
        KINDLE,
        FLARE_VOLLEY,
        CAUTERIZE,
        PHOENIX_RENEWAL
    }

    private static final Material PRIMARY_ITEM = Material.BLAZE_POWDER;
    private static final Material SECONDARY_ITEM = Material.BLAZE_ROD;

    private static final double SCORCH_RANGE_XZ = 4.5;
    private static final double SCORCH_RANGE_Y = 3.0;
    private static final int SCORCH_FIRE_SECONDS = 5;
    private static final double SCORCH_DAMAGE = 2.0;
    private static final double SCORCH_MAX_HEAL = 2.0;

    private static final int KINDLE_DURATION_SECONDS = 8;

    private static final double FIREBALL_SPEED = 0.9;
    private static final double[] FLARE_VOLLEY_OFFSETS = {-0.18, 0.0, 0.18};

    private static final double CAUTERIZE_HEAL = 4.0;
    private static final int CAUTERIZE_BUFF_SECONDS = 8;

    private static final int PHOENIX_DURATION_SECONDS = 12;
    private static final int PHOENIX_COOLDOWN_SECONDS = 180;
    private static final double PHOENIX_HEAL = 8.0;
    private static final double PHOENIX_DEFENSIVE_REDUCTION = 5.0;

    private static final double BASE_FIRE_DAMAGE = 2.5;
    private static final double BURNING_TARGET_BONUS_DAMAGE = 2.5;
    private static final double KINDLED_BONUS_DAMAGE = 5.0;
    private static final double PHOENIX_BONUS_DAMAGE = 5.0;
    private static final double PHOENIX_ATTACK_HEAL = 1.0;

    private static final int NORMAL_ATTACK_FIRE_SECONDS = 5;
    private static final int PHOENIX_ATTACK_FIRE_SECONDS = 8;

    /**
     * Constructs a new {@code PyromancerClass} for the specified player.
     *
     * @param player the RPG player that owns this class instance
     */
    public PyromancerClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(PRIMARY_ITEM, SECONDARY_ITEM);
    }

    /**
     * Handles left-click ability casting for the Pyromancer's class items.
     *
     * @param event the interaction event that triggered the cast
     * @param material the item used to cast the ability
     * @throws IllegalArgumentException if the provided material is not a supported class item
     */
    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material material) {
        if (material == PRIMARY_ITEM) {
            castScorch();
            return;
        }

        if (material == SECONDARY_ITEM) {
            castFlareVolley();
            return;
        }

        throw new IllegalArgumentException("Unsupported cast item: " + material);
    }

    /**
     * Handles right-click ability casting for the Pyromancer's class items.
     *
     * @param event the interaction event that triggered the cast
     * @param material the item used to cast the ability
     * @throws IllegalArgumentException if the provided material is not a supported class item
     */
    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material material) {
        if (material == PRIMARY_ITEM) {
            castKindle();
            return;
        }

        if (material == SECONDARY_ITEM) {
            castCauterize();
            return;
        }

        throw new IllegalArgumentException("Unsupported cast item: " + material);
    }

    /**
     * Handles the Pyromancer's death-prevention passive.
     *
     * <p>If Phoenix Renewal is off cooldown, death is prevented, the player is healed,
     * cleansed of fire, and granted temporary buffs.</p>
     *
     * @return {@code false} if death is prevented; {@code true} if the player should die normally
     */
    @Override
    public boolean onDeath() {
        if (!isAbilityReady(Ability.PHOENIX_RENEWAL)) {
            return true;
        }

        RpgPlayer rpgPlayer = getPlayer();
        Player player = rpgPlayer.getBukkitPlayer();

        send(ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Phoenix Renewal" + ChatColor.GREEN + "!");
        playSound(player, Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
        spawnParticle(player, Particle.FLAME, 80, 0.6, 0.9, 0.6, 0.03);

        player.setFireTicks(0);
        rpgPlayer.heal(PHOENIX_HEAL);
        applyPhoenixRenewalEffects(rpgPlayer);
        restartCooldown(Ability.PHOENIX_RENEWAL);

        return false;
    }

    /**
     * Applies Pyromancer defensive effects when the player takes damage.
     *
     * <p>The Pyromancer is immune to fire damage. While Phoenix Renewal is active,
     * incoming primary damage is further reduced.</p>
     *
     * @param event the incoming advanced damage event
     */
    @Override
    public void damaged(AdvancedDamageEvent event) {
        event.getEntity().setFireTicks(0);
        event.setResistance(DamageType.FIRE, DamageResistance.IMMUNITY);

        if (isPhoenixEmpowered()) {
            event.addDamage(-PHOENIX_DEFENSIVE_REDUCTION, event.getPrimaryDamage());
        }
    }

    /**
     * Applies the Pyromancer's attack modifiers to an outgoing damage event.
     *
     * <p>Basic attacks deal additional fire damage, ignite the target, and may heal
     * the player while Phoenix Renewal is active.</p>
     *
     * @param event the outgoing advanced damage event
     */
    @Override
    public void attack(AdvancedDamageEvent event) {
        double fireDamage = calculateAttackFireDamage(event);

        if (isPhoenixEmpowered()) {
            getPlayer().heal(PHOENIX_ATTACK_HEAL);
        }

        event.addDamage(fireDamage, DamageType.FIRE);
        event.getEntity().setFireTicks(Math.max(
                event.getEntity().getFireTicks(),
                secondsToTicks(isPhoenixEmpowered() ? PHOENIX_ATTACK_FIRE_SECONDS : NORMAL_ATTACK_FIRE_SECONDS)
        ));
    }

    /**
     * Casts Scorch.
     *
     * <p>Scorch ignites and damages nearby enemy targets, then heals the caster
     * based on the number of enemies hit, up to a fixed maximum.</p>
     */
    private void castScorch() {
        if (!isAbilityReady(Ability.SCORCH)) {
            return;
        }

        RpgPlayer rpgPlayer = getPlayer();
        Player player = rpgPlayer.getBukkitPlayer();
        List<LivingEntity> targets = rpgPlayer.nearbyEnemyCasterTargets(
                SCORCH_RANGE_XZ,
                SCORCH_RANGE_Y,
                SCORCH_RANGE_XZ
        );

        send(ChatColor.GOLD + "Scorch" + ChatColor.GREEN + " erupts around you!");
        spawnParticle(player, Particle.FLAME, 45, 1.8, 0.6, 1.8, 0.02);
        playSound(player, Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.9f);

        int hitCount = 0;
        for (LivingEntity target : targets) {
            applyScorchToTarget(player, target);
            hitCount++;
        }

        if (hitCount > 0) {
            rpgPlayer.heal(Math.min(hitCount, SCORCH_MAX_HEAL));
        }

        restartCooldown(Ability.SCORCH);
    }

    /**
     * Casts Kindle.
     *
     * <p>Kindle grants temporary movement speed and fire resistance, and enables
     * bonus fire damage on the Pyromancer's attacks while active.</p>
     */
    private void castKindle() {
        if (!isAbilityReady(Ability.KINDLE)) {
            return;
        }

        RpgPlayer rpgPlayer = getPlayer();
        Player player = rpgPlayer.getBukkitPlayer();

        send(ChatColor.GOLD + "Kindle" + ChatColor.GREEN + " empowers your flames!");
        rpgPlayer.addPotionEffect(PotionEffectType.SPEED, secondsToTicks(KINDLE_DURATION_SECONDS), 1);
        rpgPlayer.addPotionEffect(PotionEffectType.FIRE_RESISTANCE, secondsToTicks(KINDLE_DURATION_SECONDS), 1);

        playSound(player, Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.3f);
        spawnParticle(player, Particle.FLAME, 30, 0.4, 0.6, 0.4, 0.01);

        restartCooldown(Ability.KINDLE);
    }

    /**
     * Casts Flare Volley.
     *
     * <p>Flare Volley launches three small fireballs in a forward spread.</p>
     */
    private void castFlareVolley() {
        if (!isAbilityReady(Ability.FLARE_VOLLEY)) {
            return;
        }

        Player player = getPlayer().getBukkitPlayer();
        Vector forward = player.getEyeLocation().getDirection().normalize();
        Vector sideways = calculatePerpendicularHorizontalVector(forward);

        send(ChatColor.GOLD + "Flare Volley" + ChatColor.GREEN + " streaks forward!");

        for (double offset : FLARE_VOLLEY_OFFSETS) {
            launchFireball(player, forward, sideways, offset);
        }

        playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.1f);
        restartCooldown(Ability.FLARE_VOLLEY);
    }

    /**
     * Casts Cauterize.
     *
     * <p>Cauterize extinguishes the player, restores health, and grants temporary
     * regeneration and fire resistance.</p>
     */
    private void castCauterize() {
        if (!isAbilityReady(Ability.CAUTERIZE)) {
            return;
        }

        RpgPlayer rpgPlayer = getPlayer();
        Player player = rpgPlayer.getBukkitPlayer();

        send(ChatColor.GOLD + "Cauterize" + ChatColor.GREEN + " seals your wounds.");
        player.setFireTicks(0);
        rpgPlayer.heal(CAUTERIZE_HEAL);
        rpgPlayer.addPotionEffect(PotionEffectType.REGENERATION, secondsToTicks(CAUTERIZE_BUFF_SECONDS), 2);
        rpgPlayer.addPotionEffect(PotionEffectType.FIRE_RESISTANCE, secondsToTicks(CAUTERIZE_BUFF_SECONDS), 1);

        playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.4f);
        spawnParticle(player, Particle.ASH, 25, 0.5, 0.8, 0.5, 0.02);

        restartCooldown(Ability.CAUTERIZE);
    }

    /**
     * Applies Scorch's effects to a single target.
     *
     * @param source the player responsible for the damage
     * @param target the target to damage and ignite
     */
    private void applyScorchToTarget(Player source, LivingEntity target) {
        target.setFireTicks(Math.max(target.getFireTicks(), secondsToTicks(SCORCH_FIRE_SECONDS)));
        target.damage(SCORCH_DAMAGE, source);
        target.getWorld().spawnParticle(
                Particle.LAVA,
                target.getLocation().add(0, 1.0, 0),
                6,
                0.3,
                0.4,
                0.3,
                0.01
        );
    }

    /**
     * Applies the temporary buffs granted by Phoenix Renewal.
     *
     * @param rpgPlayer the player receiving the effects
     */
    private void applyPhoenixRenewalEffects(RpgPlayer rpgPlayer) {
        rpgPlayer.addPotionEffect(PotionEffectType.REGENERATION, secondsToTicks(PHOENIX_DURATION_SECONDS), 3);
        rpgPlayer.addPotionEffect(PotionEffectType.STRENGTH, secondsToTicks(PHOENIX_DURATION_SECONDS), 1);
        rpgPlayer.addPotionEffect(PotionEffectType.SPEED, secondsToTicks(PHOENIX_DURATION_SECONDS), 1);
    }

    /**
     * Calculates the bonus fire damage to add to an outgoing basic attack.
     *
     * <p>Additional damage is granted against already burning targets, while Kindle
     * is active, and while Phoenix Renewal is active.</p>
     *
     * @param event the outgoing advanced damage event
     * @return the amount of fire damage to add
     */
    private double calculateAttackFireDamage(AdvancedDamageEvent event) {
        double fireDamage = BASE_FIRE_DAMAGE;

        if (event.getEntity().getFireTicks() > 0) {
            fireDamage += BURNING_TARGET_BONUS_DAMAGE;
        }

        if (isKindled()) {
            fireDamage += KINDLED_BONUS_DAMAGE;
        }

        if (isPhoenixEmpowered()) {
            fireDamage += PHOENIX_BONUS_DAMAGE;
        }

        return fireDamage;
    }

    /**
     * Launches a single fireball with a horizontal spread offset.
     *
     * @param player the player launching the projectile
     * @param forward the normalized forward direction
     * @param sideways the normalized horizontal perpendicular direction
     * @param offset the horizontal spread offset to apply
     */
    private void launchFireball(Player player, Vector forward, Vector sideways, double offset) {
        Vector velocity = forward.clone()
                .add(sideways.clone().multiply(offset))
                .normalize()
                .multiply(FIREBALL_SPEED);

        SmallFireball fireball = player.launchProjectile(SmallFireball.class, velocity);
        fireball.setIsIncendiary(true);
    }

    /**
     * Computes a normalized horizontal vector perpendicular to the given direction.
     *
     * <p>If the provided direction has no horizontal perpendicular component,
     * a default unit vector is returned.</p>
     *
     * @param direction the source direction vector
     * @return a normalized horizontal perpendicular vector
     */
    private Vector calculatePerpendicularHorizontalVector(Vector direction) {
        Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0));

        if (perpendicular.lengthSquared() == 0) {
            return new Vector(1, 0, 0);
        }

        return perpendicular.normalize();
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
     * Determines whether Kindle's active effect window is currently in effect.
     *
     * @return {@code true} if Kindle is active; {@code false} otherwise
     */
    private boolean isKindled() {
        return isEffectWindowActive(Ability.KINDLE, 20, KINDLE_DURATION_SECONDS);
    }

    /**
     * Determines whether Phoenix Renewal's empowered state is currently active.
     *
     * @return {@code true} if Phoenix Renewal's active window is in effect; {@code false} otherwise
     */
    private boolean isPhoenixEmpowered() {
        return isEffectWindowActive(Ability.PHOENIX_RENEWAL, PHOENIX_COOLDOWN_SECONDS, PHOENIX_DURATION_SECONDS);
    }

    /**
     * Determines whether an ability's active effect window is still running.
     *
     * <p>This is derived from the ability's remaining cooldown time.</p>
     *
     * @param ability the ability to evaluate
     * @param cooldownSeconds the full cooldown duration in seconds
     * @param activeWindowSeconds the duration of the active effect window in seconds
     * @return {@code true} if the ability's active effect window is still active; {@code false} otherwise
     */
    private boolean isEffectWindowActive(Ability ability, int cooldownSeconds, int activeWindowSeconds) {
        return getCooldowns()[ability.ordinal()].getRemaining() >= (cooldownSeconds - activeWindowSeconds);
    }

    /**
     * Creates and initializes the cooldown array for all Pyromancer abilities.
     *
     * @return the initialized cooldown array ordered by {@link Ability#ordinal()}
     */
    private Cooldown[] createCooldowns() {
        Map<Ability, Integer> cooldownDurations = new EnumMap<>(Ability.class);
        cooldownDurations.put(Ability.SCORCH, 12);
        cooldownDurations.put(Ability.KINDLE, 20);
        cooldownDurations.put(Ability.FLARE_VOLLEY, 18);
        cooldownDurations.put(Ability.CAUTERIZE, 35);
        cooldownDurations.put(Ability.PHOENIX_RENEWAL, PHOENIX_COOLDOWN_SECONDS);

        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        for (Ability ability : Ability.values()) {
            cooldowns[ability.ordinal()] = new Cooldown(cooldownDurations.get(ability));
        }

        return cooldowns;
    }

    /**
     * Plays a sound at the player's current location.
     *
     * @param player the player whose location will be used
     * @param sound the sound to play
     * @param volume the playback volume
     * @param pitch the playback pitch
     */
    private void playSound(Player player, Sound sound, float volume, float pitch) {
        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
    }

    /**
     * Spawns particles slightly above the player's current location.
     *
     * @param player the player whose location will be used
     * @param particle the particle type to spawn
     * @param count the number of particles to spawn
     * @param offsetX the horizontal X offset range
     * @param offsetY the vertical offset range
     * @param offsetZ the horizontal Z offset range
     * @param extra the particle extra parameter
     */
    private void spawnParticle(Player player, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1.0, 0),
                count,
                offsetX,
                offsetY,
                offsetZ,
                extra
        );
    }

    }
