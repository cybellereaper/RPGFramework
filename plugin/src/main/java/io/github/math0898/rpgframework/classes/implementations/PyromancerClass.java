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

import java.util.List;

/**
 * The Pyromancer overwhelms nearby enemies with fire magic and enters short empowered burn phases.
 *
 * @author Sugaku
 */
public class PyromancerClass extends AbstractClass {

    /**
     * Enum names for the Pyromancer's cooldowns.
     */
    private enum Abilities {

        /**
         * A point-blank burst of flame that ignites nearby enemies.
         */
        SCORCH,

        /**
         * A short empowerment window that improves the pyromancer's attacks.
         */
        KINDLE,

        /**
         * A spread of quick moving firebolts.
         */
        FLARE_VOLLEY,

        /**
         * A burst of restorative fire.
         */
        CAUTERIZE,

        /**
         * Phoenix themed cheat death and empowerment.
         */
        PHOENIX_RENEWAL
    }

    private static final int KINDLE_WINDOW_SECONDS = 8;
    private static final int PHOENIX_WINDOW_SECONDS = 12;

    /**
     * Creates a new AbstractClass object which is specific to the given player.
     *
     * @param p The player this class is specific to.
     */
    public PyromancerClass (RpgPlayer p) {
        super(p);
        Cooldown[] cds = new Cooldown[5];
        cds[Abilities.SCORCH.ordinal()] = new Cooldown(12);
        cds[Abilities.KINDLE.ordinal()] = new Cooldown(20);
        cds[Abilities.FLARE_VOLLEY.ordinal()] = new Cooldown(18);
        cds[Abilities.CAUTERIZE.ordinal()] = new Cooldown(35);
        cds[Abilities.PHOENIX_RENEWAL.ordinal()] = new Cooldown(180);
        setCooldowns(cds);
        setClassItems(Material.BLAZE_POWDER, Material.BLAZE_ROD);
    }

    /**
     * Called whenever a player left-clicks while holding a class item.
     *
     * @param event The PlayerInteractEvent that lead to this method being called.
     * @param type  The type of material that was used in this cast.
     */
    @Override
    public void onLeftClickCast (PlayerInteractEvent event, Material type) {
        switch (type) {
            case BLAZE_POWDER -> castScorch();
            case BLAZE_ROD -> castFlareVolley();
        }
    }

    /**
     * Called whenever a player right-clicks while holding a class item.
     *
     * @param event The PlayerInteractEvent that lead to this method being called.
     * @param type  The type of material that was used in this cast.
     */
    @Override
    public void onRightClickCast (PlayerInteractEvent event, Material type) {
        switch (type) {
            case BLAZE_POWDER -> castKindle();
            case BLAZE_ROD -> castCauterize();
        }
    }

    private void castScorch () {
        if (!offCooldown(Abilities.SCORCH.ordinal())) return;
        RpgPlayer rpg = getPlayer();
        Player player = rpg.getBukkitPlayer();
        List<LivingEntity> targets = rpg.nearbyEnemyCasterTargets(4.5, 3.0, 4.5);
        send(ChatColor.GOLD + "Scorch" + ChatColor.GREEN + " erupts around you!");
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 45, 1.8, 0.6, 1.8, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.9f);
        int hitCount = 0;
        for (LivingEntity entity : targets) {
            entity.setFireTicks(Math.max(entity.getFireTicks(), 5 * 20));
            entity.damage(2.0, player);
            entity.getWorld().spawnParticle(Particle.LAVA, entity.getLocation().add(0, 1.0, 0), 6, 0.3, 0.4, 0.3, 0.01);
            hitCount++;
        }
        if (hitCount > 0)
            rpg.heal(Math.min(hitCount, 2));
        getCooldowns()[Abilities.SCORCH.ordinal()].restart();
    }

    private void castKindle () {
        if (!offCooldown(Abilities.KINDLE.ordinal())) return;
        Player player = getPlayer().getBukkitPlayer();
        send(ChatColor.GOLD + "Kindle" + ChatColor.GREEN + " empowers your flames!");
        getPlayer().addPotionEffect(PotionEffectType.SPEED, KINDLE_WINDOW_SECONDS * 20, 1);
        getPlayer().addPotionEffect(PotionEffectType.FIRE_RESISTANCE, KINDLE_WINDOW_SECONDS * 20, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.8f, 1.3f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 30, 0.4, 0.6, 0.4, 0.01);
        getCooldowns()[Abilities.KINDLE.ordinal()].restart();
    }

    private void castFlareVolley () {
        if (!offCooldown(Abilities.FLARE_VOLLEY.ordinal())) return;
        Player player = getPlayer().getBukkitPlayer();
        send(ChatColor.GOLD + "Flare Volley" + ChatColor.GREEN + " streaks forward!");
        Vector forward = player.getEyeLocation().getDirection().normalize();
        Vector sideways = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (sideways.lengthSquared() == 0)
            sideways = new Vector(1, 0, 0);
        sideways.normalize();
        for (double offset : new double[]{-0.18, 0.0, 0.18}) {
            Vector velocity = forward.clone().add(sideways.clone().multiply(offset)).normalize().multiply(0.9);
            SmallFireball projectile = player.launchProjectile(SmallFireball.class, velocity);
            projectile.setIsIncendiary(true);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.1f);
        getCooldowns()[Abilities.FLARE_VOLLEY.ordinal()].restart();
    }

    private void castCauterize () {
        if (!offCooldown(Abilities.CAUTERIZE.ordinal())) return;
        RpgPlayer rpg = getPlayer();
        Player player = rpg.getBukkitPlayer();
        send(ChatColor.GOLD + "Cauterize" + ChatColor.GREEN + " seals your wounds.");
        player.setFireTicks(0);
        rpg.heal(4.0);
        rpg.addPotionEffect(PotionEffectType.REGENERATION, 8 * 20, 2);
        rpg.addPotionEffect(PotionEffectType.FIRE_RESISTANCE, 8 * 20, 1);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.4f);
        player.getWorld().spawnParticle(Particle.ASH, player.getLocation().add(0, 1.0, 0), 25, 0.5, 0.8, 0.5, 0.02);
        getCooldowns()[Abilities.CAUTERIZE.ordinal()].restart();
    }

    /**
     * Called when the class user has 'died'.
     *
     * @return Whether a death should be respected or not.
     */
    @Override
    public boolean onDeath () {
        if (offCooldown(Abilities.PHOENIX_RENEWAL.ordinal())) {
            RpgPlayer rpg = getPlayer();
            Player player = rpg.getBukkitPlayer();
            send(ChatColor.GREEN + "You've used " + ChatColor.GOLD + "Phoenix Renewal" + ChatColor.GREEN + "!");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.8f, 1.0f);
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1.0, 0), 80, 0.6, 0.9, 0.6, 0.03);
            player.setFireTicks(0);
            rpg.heal(8.0);
            rpg.addPotionEffect(PotionEffectType.REGENERATION, PHOENIX_WINDOW_SECONDS * 20, 3);
            rpg.addPotionEffect(PotionEffectType.STRENGTH, PHOENIX_WINDOW_SECONDS * 20, 1);
            rpg.addPotionEffect(PotionEffectType.SPEED, PHOENIX_WINDOW_SECONDS * 20, 1);
            getCooldowns()[Abilities.PHOENIX_RENEWAL.ordinal()].restart();
            return false;
        }
        return true;
    }

    /**
     * Called whenever this DamageModifier is relevant on a defensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void damaged (AdvancedDamageEvent event) {
        event.getEntity().setFireTicks(0);
        event.setResistance(DamageType.FIRE, DamageResistance.IMMUNITY);
        if (phoenixEmpowered())
            event.addDamage(-5.0, event.getPrimaryDamage());
    }

    /**
     * Called whenever this DamageModifier is relevant on an offensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void attack (AdvancedDamageEvent event) {
        double fireDamage = 2.5;
        if (event.getEntity().getFireTicks() > 0)
            fireDamage += 2.5;
        if (kindled())
            fireDamage += 5.0;
        if (phoenixEmpowered()) {
            fireDamage += 5.0;
            getPlayer().heal(1.0);
        }
        event.addDamage(fireDamage, DamageType.FIRE);
        event.getEntity().setFireTicks(Math.max(event.getEntity().getFireTicks(), phoenixEmpowered() ? 8 * 20 : 5 * 20));
    }

    private boolean kindled () {
        return getCooldowns()[Abilities.KINDLE.ordinal()].getRemaining() >= (20 - KINDLE_WINDOW_SECONDS);
    }

    private boolean phoenixEmpowered () {
        return getCooldowns()[Abilities.PHOENIX_RENEWAL.ordinal()].getRemaining() >= (180 - PHOENIX_WINDOW_SECONDS);
    }
}
