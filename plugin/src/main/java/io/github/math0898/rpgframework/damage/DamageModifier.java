package io.github.math0898.rpgframework.damage;

import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Objects;

/**
 * DamageModifiers are used to cause special effects whenever an advanced damage event occurs.
 *
 * @author Sugaku
 */
public interface DamageModifier {

    /**
     * Called whenever this DamageModifier is relevant on a defensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    default void damaged (AdvancedDamageEvent event) {

    }

    /**
     * Called whenever this DamageModifier is relevant on an offensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    default void attack (AdvancedDamageEvent event) {

    }

    @Deprecated
    default void damaged (EntityDamageEvent event) {
        applyLegacyDamage(event, toAdvancedDamageEvent(event), this::damaged);
    }

    @Deprecated
    default void attack (EntityDamageByEntityEvent event) {
        applyLegacyDamage(event, toAdvancedDamageEvent(event), this::attack);
    }

    /**
     * Creates an advanced damage event from the deprecated Bukkit damage event.
     *
     * @param event The Bukkit event to convert.
     * @return The advanced damage event wrapping the Bukkit event.
     */
    static AdvancedDamageEvent toAdvancedDamageEvent(EntityDamageEvent event) {
        return new AdvancedDamageEvent(Objects.requireNonNull(event, "event"));
    }

    /**
     * Applies a fully-handled advanced damage result back onto the original Bukkit damage event.
     *
     * @param event The original Bukkit event.
     * @param advancedDamageEvent The advanced damage event derived from it.
     * @param handler The advanced damage handler to invoke.
     */
    static void applyLegacyDamage(EntityDamageEvent event, AdvancedDamageEvent advancedDamageEvent,
                                  java.util.function.Consumer<AdvancedDamageEvent> handler) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(advancedDamageEvent, "advancedDamageEvent");
        Objects.requireNonNull(handler, "handler");
        handler.accept(advancedDamageEvent);
        if (advancedDamageEvent.isCancelled()) {
            event.setCancelled(true);
        }
        event.setDamage(scaleLegacyDamage(AdvancedDamageHandler.damageCalculation(advancedDamageEvent)));
    }

    /**
     * Scales advanced damage values back to vanilla damage values.
     *
     * @param advancedDamage The advanced damage amount.
     * @return The vanilla-scaled damage amount.
     */
    static double scaleLegacyDamage(double advancedDamage) {
        return advancedDamage / 5.0;
    }
}
