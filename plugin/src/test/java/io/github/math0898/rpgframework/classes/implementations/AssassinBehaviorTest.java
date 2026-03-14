package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.classes.lua.LuaClassDefinition;
import io.github.math0898.rpgframework.classes.lua.LuaClassScriptEngine;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("PotionEffectType static initialization is unavailable in this JVM-only unit test runtime")
class AssassinBehaviorTest {

    private final LuaClassScriptEngine scriptEngine = new LuaClassScriptEngine();

    @Test
    void assassinDefinitionHasExpectedClassItems() {
        LuaClassDefinition definition = loadAssassinDefinition();

        assertTrue(definition.classItems().contains(Material.GHAST_TEAR));
    }

    @Test
    void assassinDefinitionHasExpectedArmor() {
        LuaClassDefinition definition = loadAssassinDefinition();

        assertEquals(Material.LEATHER_HELMET, definition.requiredArmor().get(EquipmentSlot.HEAD));
        assertEquals(Material.LEATHER_CHESTPLATE, definition.requiredArmor().get(EquipmentSlot.CHEST));
        assertEquals(Material.LEATHER_LEGGINGS, definition.requiredArmor().get(EquipmentSlot.LEGS));
        assertEquals(Material.LEATHER_BOOTS, definition.requiredArmor().get(EquipmentSlot.FEET));
    }

    @Test
    void assassinDefinitionHasExpectedCooldowns() {
        LuaClassDefinition definition = loadAssassinDefinition();

        assertArrayEquals(new int[]{30, 60, 300}, definition.cooldownSeconds());
    }

    @Test
    void assassinDefinitionExposesRuntimeHooks() {
        LuaClassDefinition definition = loadAssassinDefinition();

        assertTrue(definition.hook("passive").isfunction());
        assertTrue(definition.hook("onLeftClick").isfunction());
        assertTrue(definition.hook("onRightClick").isfunction());
        assertTrue(definition.hook("onDeath").isfunction());
        assertTrue(definition.hook("onDamaged").isfunction());
        assertTrue(definition.hook("onAttack").isfunction());
    }

    private LuaClassDefinition loadAssassinDefinition() {
        String script = """
                return {
                  id = "assassin",
                  classItems = { "GHAST_TEAR" },
                  requiredArmor = {
                    HEAD = "LEATHER_HELMET",
                    CHEST = "LEATHER_CHESTPLATE",
                    LEGS = "LEATHER_LEGGINGS",
                    FEET = "LEATHER_BOOTS"
                  },
                  cooldowns = { 30, 60, 300 },
                  passive = function(clazz) end,
                  onLeftClick = function(clazz, event) end,
                  onRightClick = function(clazz, event) end,
                  onDeath = function(clazz) return true end,
                  onDamaged = function(clazz, event) end,
                  onAttack = function(clazz, event) end
                }
                """;

        return scriptEngine.parse(script);
    }
}
