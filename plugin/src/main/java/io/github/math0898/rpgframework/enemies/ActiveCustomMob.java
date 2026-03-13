package io.github.math0898.rpgframework.enemies;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import io.github.math0898.rpgframework.damage.DamageModifier;
import io.github.math0898.rpgframework.damage.events.VerifiedDeathEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * This is an active instance of the CustomMobEntry.
 *
 * @author Sugaku
 */
public class ActiveCustomMob implements DamageModifier, Listener {

    /**
     * The LivingEntity attached to this ActiveCustomMob.
     */
    protected final LivingEntity entity;

    /**
     * This is the namespaceKey of the mob in CustomMobEntry.
     */
    protected final String namespaceKey;

    /**
     * This is the CustomMobEntry for this active mob.
     */
    protected final CustomMobEntry customMobEntry;

    /**
     * Creates a new ActiveCustomMob with the given entity instance.
     *
     * @param entity       The entity to attach to this specific ActiveCustomMob instance.
     * @param namespaceKey The key for the CustomMobEntry inside the MobManager.
     */
    public ActiveCustomMob (LivingEntity entity, String namespaceKey) {
        this.entity = entity;
        Bukkit.getPluginManager().registerEvents(this, RPGFramework.getPlugin());
        this.namespaceKey = namespaceKey;
        customMobEntry = MobManager.getInstance().getCustomMob(namespaceKey);
    }

    /**
     * Called whenever an AdvancedDamageEvent is fired by RPGFramework.
     *
     * @param event The AdvancedDamageEvent we may be interested in.
     */
    @EventHandler
    public void onAdvancedDamage (AdvancedDamageEvent event) {
        if (entity == null || entity.isDead()) {
            HandlerList.unregisterAll(this);
            return;
        }
        int id = event.getEntity().getEntityId();
        if (id == entity.getEntityId())
            damaged(event);
        else if (event.getBasicEvent() instanceof EntityDamageByEntityEvent attackingEvent)
            if (attackingEvent.getDamager().getEntityId() == entity.getEntityId())
                attack(event);
    }

    /**
     * Called whenever a death is verified by RPGFramework.
     *
     * @param event The VerifiedDeathEvent we may be interested in.
     */
    @EventHandler
    public void onVerifiedDeath (VerifiedDeathEvent event) {
        if (entity == null) {
            HandlerList.unregisterAll(this);
            return;
        }
        int id = event.getEntity().getEntityId();
        if (id == entity.getEntityId()) {
            MobManager.getInstance().reportCustomMobDeath(event.getEntity().getLocation(), namespaceKey);
            HandlerList.unregisterAll(this);
        }
    }

    /**
     * Called whenever this DamageModifier is relevant on a defensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void damaged (AdvancedDamageEvent event) {

    }

    /**
     * Called whenever this DamageModifier is relevant on an offensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void attack (AdvancedDamageEvent event) {

    }

    /**
     * Causes this Mob to "speak" to the given player with the given message.
     *
     * @param player The player to send this message to.
     * @param message The message to send.
     */
    public void speak (Player player, String message) {
        player.sendMessage(customMobEntry.getName() + ChatColor.DARK_GRAY + " > " + ChatColor.GRAY + message);
    }
}
