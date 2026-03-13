package io.github.math0898.rpgframework.damage;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import eu.decentsoftware.holograms.api.DHAPI;
import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import io.github.math0898.rpgframework.damage.events.LethalDamageEvent;
import io.github.math0898.rpgframework.damage.events.VerifiedDeathEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * This is the end all be all for the AdvancedDamageEvent and where the actual damage happens. It also has the side job
 * of changing normal damage events into advanced ones.
 *
 * @author Sugaku
 */
public class AdvancedDamageHandler implements Listener {

    /**
     * This is an internal counter to prevent hologram name collisions.
     */
    private final AtomicLong hologramCount = new AtomicLong();

    /**
     * Where the conversion from DamageEvent to AdvancedDamageEvent occurs. Any handlers after this point will be
     * ignored since the values are already grabbed.
     *
     * @param event The damage event.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage (EntityDamageEvent event) {
        AdvancedDamageEvent advancedDamageEvent = new AdvancedDamageEvent(event);
        Bukkit.getPluginManager().callEvent(advancedDamageEvent); //Call the event

        double damage = damageCalculation(advancedDamageEvent);
        event.setDamage(damage/5.00);

        if (RPGFramework.useHolographicDisplays || RPGFramework.useDecentHolograms)
            displayDamage(damage, event.getEntity().getLocation());

        if (advancedDamageEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        Entity entity = event.getEntity(); // todo: Clean up.
        event.getFinalDamage();
        if (entity instanceof LivingEntity living)
            if (living.getHealth() <= event.getFinalDamage()) {
                LethalDamageEvent lethalDamageEvent = new LethalDamageEvent(advancedDamageEvent);
                Bukkit.getPluginManager().callEvent(lethalDamageEvent);
                if (lethalDamageEvent.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }

                VerifiedDeathEvent verifiedDeathEvent = new VerifiedDeathEvent(advancedDamageEvent);
                Bukkit.getPluginManager().callEvent(verifiedDeathEvent);
            }
    }

    /**
     * Determines the amount of damage that needs to be applied to the player based on resistances and types of armor.
     *
     * @param advancedDamageEvent The advancedDamageEvent we're calculating the result of.
     * @return The damage that should be dealt to the victim.
     */
    public static double damageCalculation (AdvancedDamageEvent advancedDamageEvent) {
        double damage = 0.00;
        Map<DamageType, Double> damages = advancedDamageEvent.getDamages();
        Map<DamageType, DamageResistance> resistance = advancedDamageEvent.getResistances();
        for (DamageType type: damages.keySet()) {
            double dmg = damages.get(type);
            if (DamageType.archetype(type).equalsIgnoreCase("MAGIC")) dmg = dmg * (1.00 - advancedDamageEvent.getMagicResistance());
            else if (DamageType.archetype(type).equalsIgnoreCase("PHYSICAL")) dmg = dmg * (1.00 - advancedDamageEvent.getPhysicalResistance());
            dmg = DamageResistanceCalculator.applyResistance(dmg, resistance.get(type));
            damage += dmg;
        }
        return damage;
    }

    /**
     * Displays the damage dealt to the entity nearby so that players can see big numbers.
     *
     * @param damage The amount of damage being displayed.
     * @param location The location of the entity that was hit.
     */
    public void displayDamage (Double damage, Location location) {
        Random random = new Random();
        Location locale = new Location(location.getWorld(), location.getX() + random.nextDouble() - 0.50,
                location.getY() + 1.50,
                location.getZ() + random.nextDouble() - 0.50);
        String text = ChatColor.RED + "☆" + ChatColor.YELLOW + String.format("%.1f", damage) + ChatColor.RED + "☆";
        try {
            if (RPGFramework.useHolographicDisplays) {
                Hologram hologram = HologramsAPI.createHologram(RPGFramework.getPlugin(), locale);
                hologram.appendTextLine(text);
                Bukkit.getScheduler().runTaskLater(RPGFramework.getPlugin(), hologram::delete, 5*10);
            }
            if (RPGFramework.useDecentHolograms) {
                String name = "rpgframeworkdamage" + hologramCount.getAndAdd(1L);
                DHAPI.createHologram(name, locale, Collections.singletonList(text));
                Bukkit.getScheduler().runTaskLater(RPGFramework.getPlugin(), () -> DHAPI.removeHologram(name), 5*10);
            }
        } catch (NoClassDefFoundError error) {
            RPGFramework.getPlugin().getLogger().log(Level.WARNING, error.getMessage());
        }
    }
}
