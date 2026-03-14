package io.github.math0898.rpgframework.classes.lua;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaClassScriptEngineTest {

    private final LuaClassScriptEngine engine = new LuaClassScriptEngine();

    @Test
    void parseBuildsDefinitionFromScript() {
        LuaClassDefinition definition = engine.parse("""
                return {
                  id = "assassin",
                  classItems = { "GHAST_TEAR", "BLAZE_ROD" },
                  requiredArmor = {
                    HEAD = "LEATHER_HELMET",
                    CHEST = "LEATHER_CHESTPLATE"
                  },
                  cooldowns = { 30, 60, 300 }
                }
                """);

        assertEquals("assassin", definition.id());
        assertTrue(definition.classItems().contains(Material.GHAST_TEAR));
        assertEquals(Material.LEATHER_HELMET, definition.requiredArmor().get(EquipmentSlot.HEAD));
        assertArrayEquals(new int[]{30, 60, 300}, definition.cooldownSeconds());
    }

    @Test
    void parseRejectsInvalidMaterialName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> engine.parse("""
                return {
                  classItems = { "NOT_A_MATERIAL" },
                  requiredArmor = {},
                  cooldowns = { 1 }
                }
                """));

        assertTrue(error.getMessage().contains("Invalid class item"));
    }

    @Test
    void parseRejectsNegativeCooldownValue() {
        assertThrows(IllegalArgumentException.class, () -> engine.parse("""
                return {
                  classItems = { "GHAST_TEAR" },
                  requiredArmor = {},
                  cooldowns = { -1 }
                }
                """));
    }
}
