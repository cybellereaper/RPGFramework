package io.github.math0898.rpgframework.classes.implementations;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssassinBehaviorTest {

    private final AssassinBehavior behavior = AssassinClass.BEHAVIOR;

    @Test
    void negatesDamageDuringHeroicDodgeWindow() {
        assertTrue(behavior.shouldNegateIncomingDamage(290, 0, 0.99));
    }

    @Test
    void negatesDamageDuringInvisibilityWindow() {
        assertTrue(behavior.shouldNegateIncomingDamage(0, 20, 0.99));
    }

    @Test
    void negatesDamageWhenRandomDodgeProcs() {
        assertTrue(behavior.shouldNegateIncomingDamage(0, 0, 0.10));
    }

    @Test
    void doesNotNegateDamageOutsideAllConditions() {
        assertFalse(behavior.shouldNegateIncomingDamage(289, 19, 0.11));
    }

    @Test
    void bonusDamageDependsOnTargetType() {
        assertEquals(5.0, behavior.bonusDamage(true));
        assertEquals(10.0, behavior.bonusDamage(false));
    }

    @Test
    void poisonedBladeActivationUsesCooldownThreshold() {
        assertTrue(behavior.isPoisonedBladeActive(50));
        assertFalse(behavior.isPoisonedBladeActive(49));
    }

    @Test
    void poisonedBladeEffectsStayInExpectedOrder() {
        assertIterableEquals(
                List.of(PotionEffectType.BLINDNESS, PotionEffectType.POISON, PotionEffectType.SLOWNESS),
                behavior.poisonedBladeEffects()
        );
    }

    @Test
    void armorMappingMatchesFullLeatherSet() {
        assertTrue(behavior.hasRequiredArmor(EquipmentSlot.HEAD, Material.LEATHER_HELMET));
        assertTrue(behavior.hasRequiredArmor(EquipmentSlot.CHEST, Material.LEATHER_CHESTPLATE));
        assertTrue(behavior.hasRequiredArmor(EquipmentSlot.LEGS, Material.LEATHER_LEGGINGS));
        assertTrue(behavior.hasRequiredArmor(EquipmentSlot.FEET, Material.LEATHER_BOOTS));
        assertFalse(behavior.hasRequiredArmor(EquipmentSlot.HEAD, Material.GOLDEN_HELMET));
    }
}
