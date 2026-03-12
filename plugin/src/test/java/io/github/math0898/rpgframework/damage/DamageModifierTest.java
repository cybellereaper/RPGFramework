package io.github.math0898.rpgframework.damage;

import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DamageModifierTest {

    @Test
    void scaleLegacyDamageConvertsAdvancedValuesBackToVanilla() {
        assertEquals(4.0, DamageModifier.scaleLegacyDamage(20.0));
    }

    @Test
    void damagedLegacyMethodDelegatesToAdvancedHookAndAppliesScaledDamage() {
        EntityDamageEvent event = mockDamageEvent(4.0);
        RecordingDamageModifier modifier = new RecordingDamageModifier();

        modifier.damaged(event);

        assertEquals(1, modifier.damagedCalls);
        assertTrue(modifier.lastDamagedEvent != null);
        verify(event).setDamage(4.0);
    }

    @Test
    void attackLegacyMethodDelegatesToAdvancedHookAndAppliesScaledDamage() {
        EntityDamageByEntityEvent event = mockAttackEvent(6.0);
        RecordingDamageModifier modifier = new RecordingDamageModifier();

        modifier.attack(event);

        assertEquals(1, modifier.attackCalls);
        assertTrue(modifier.lastAttackEvent != null);
        verify(event).setDamage(6.0);
    }

    @Test
    void applyLegacyDamageCarriesCancellationBackToOriginalEvent() {
        EntityDamageEvent event = mockDamageEvent(5.0);
        AdvancedDamageEvent advancedDamageEvent = DamageModifier.toAdvancedDamageEvent(event);

        DamageModifier.applyLegacyDamage(event, advancedDamageEvent, advanced -> advanced.setCancelled(true));

        verify(event).setCancelled(true);
        verify(event).setDamage(5.0);
    }

    private static EntityDamageEvent mockDamageEvent(double damage) {
        Entity entity = mock(Entity.class);
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.CUSTOM);
        when(event.getDamage()).thenReturn(damage);
        return event;
    }

    private static EntityDamageByEntityEvent mockAttackEvent(double damage) {
        Entity entity = mock(Entity.class);
        Entity damager = mock(Entity.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getDamager()).thenReturn(damager);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.ENTITY_ATTACK);
        when(event.getDamage()).thenReturn(damage);
        return event;
    }

    private static final class RecordingDamageModifier implements DamageModifier {

        private int damagedCalls;
        private int attackCalls;
        private AdvancedDamageEvent lastDamagedEvent;
        private AdvancedDamageEvent lastAttackEvent;

        @Override
        public void damaged(AdvancedDamageEvent event) {
            damagedCalls++;
            lastDamagedEvent = event;
        }

        @Override
        public void attack(AdvancedDamageEvent event) {
            attackCalls++;
            lastAttackEvent = event;
        }
    }
}
