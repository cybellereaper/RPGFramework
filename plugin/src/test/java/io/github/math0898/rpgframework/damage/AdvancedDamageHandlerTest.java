package io.github.math0898.rpgframework.damage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdvancedDamageHandlerTest {

    @Test
    void applyResistanceHandlesAllDefinedResistanceStates() {
        assertEquals(0.0, DamageResistanceCalculator.applyResistance(15.0, DamageResistance.IMMUNITY));
        assertEquals(7.5, DamageResistanceCalculator.applyResistance(15.0, DamageResistance.RESISTANCE));
        assertEquals(15.0, DamageResistanceCalculator.applyResistance(15.0, DamageResistance.NORMAL));
        assertEquals(22.5, DamageResistanceCalculator.applyResistance(15.0, DamageResistance.SUSCEPTIBILITY));
        assertEquals(30.0, DamageResistanceCalculator.applyResistance(15.0, DamageResistance.VULNERABILITY));
    }

    @Test
    void applyResistanceRejectsNullResistance() {
        assertThrows(IllegalArgumentException.class, () -> DamageResistanceCalculator.applyResistance(15.0, null));
    }
}
