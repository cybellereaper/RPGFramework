package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractClassTest {

    @Test
    void correctArmorRequiresEveryArmorSlot() {
        TestAbstractClass clazz = new TestAbstractClass(true, true, true, true);
        assertTrue(clazz.correctArmor());

        TestAbstractClass missingBoots = new TestAbstractClass(true, true, true, false);
        assertFalse(missingBoots.correctArmor());
    }

    @Test
    void getCooldownReturnsNullWhenIndexIsInvalid() {
        TestAbstractClass clazz = new TestAbstractClass(true, true, true, true);
        clazz.configureCooldowns(new Cooldown(30));

        assertTrue(clazz.offCooldown(0));
        assertNull(clazz.lookupCooldown(-1));
        assertNull(clazz.lookupCooldown(1));
        assertTrue(clazz.offCooldown(1));
    }

    @Test
    void setCooldownsFallsBackToEmptyArrayWhenNull() {
        TestAbstractClass clazz = new TestAbstractClass(true, true, true, true);

        clazz.configureCooldowns((Cooldown[]) null);

        assertEquals(0, clazz.getCooldowns().length);
        assertTrue(clazz.offCooldown(0));
    }

    @Test
    void setClassItemsSkipsNullsAndDuplicates() {
        TestAbstractClass clazz = new TestAbstractClass(true, true, true, true);

        clazz.configureClassItems(Material.GHAST_TEAR, null, Material.GHAST_TEAR, Material.BLAZE_ROD);

        assertTrue(clazz.classItemIndex(Material.GHAST_TEAR));
        assertTrue(clazz.classItemIndex(Material.BLAZE_ROD));
        assertFalse(clazz.classItemIndex(Material.STICK));
    }

    private static final class TestAbstractClass extends AbstractClass {

        private final boolean head;
        private final boolean chest;
        private final boolean legs;
        private final boolean feet;

        private TestAbstractClass(boolean head, boolean chest, boolean legs, boolean feet) {
            super(null, false);
            this.head = head;
            this.chest = chest;
            this.legs = legs;
            this.feet = feet;
        }

        private void configureCooldowns(Cooldown... cooldowns) {
            setCooldowns(cooldowns);
        }

        private void configureClassItems(Material... materials) {
            setClassItems(materials);
        }

        private Cooldown lookupCooldown(int index) {
            return getCooldown(index);
        }

        private boolean classItemIndex(Material material) {
            return isClassItem(material);
        }

        @Override
        public boolean correctArmor(EquipmentSlot slot) {
            return switch (slot) {
                case HEAD -> head;
                case CHEST -> chest;
                case LEGS -> legs;
                case FEET -> feet;
                default -> true;
            };
        }

        @Override
        public void damaged(AdvancedDamageEvent event) {
        }

        @Override
        public void attack(AdvancedDamageEvent event) {
        }
    }
}
