package io.github.math0898.rpgframework.classes.lua;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

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
    void parseRetainsCallableHookFunctions() {
        LuaClassDefinition definition = engine.parse("""
                return {
                  classItems = { "GHAST_TEAR" },
                  requiredArmor = {},
                  cooldowns = { 1 },
                  onDeath = function(clazz) return false end
                }
                """);

        assertTrue(definition.hook("onDeath").isfunction());
    }

    @Test
    void runtimeContextDoesNotResolveLuaFunctionHooksThroughJavaApi() {
        LuaClassDefinition definition = engine.parse("""
                return {
                  classItems = { "GHAST_TEAR" },
                  requiredArmor = {},
                  cooldowns = { 1 },
                  onLeftClick = function(clazz)
                    return clazz:helper()
                  end
                }
                """);

        LuaTable runtimeContext = definition.createRuntimeContext(CoerceJavaToLua.coerce(new TestLuaApi()));

        assertTrue(definition.hook(runtimeContext, "onLeftClick").isfunction());
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

    @Test
    void runtimeContextSupportsScriptHelpersAndJavaApiMethods() {
        LuaClassDefinition definition = engine.parse("""
                return {
                  classItems = { "GHAST_TEAR" },
                  requiredArmor = {},
                  cooldowns = { 10, 20, 30 },
                  abilities = { TEST = 2 },
                  isEffectWindowActive = function(clazz, abilityIndex, cooldownSeconds, activeWindowSeconds)
                    return clazz:getRemainingCooldown(abilityIndex) >= (cooldownSeconds - activeWindowSeconds)
                  end,
                  onLeftClick = function(clazz)
                    return clazz:isAbilityReady(clazz.abilities.TEST)
                      and clazz:isEffectWindowActive(clazz.abilities.TEST, 20, 8)
                  end
                }
                """);

        TestLuaApi api = new TestLuaApi();
        LuaTable runtimeContext = definition.createRuntimeContext(CoerceJavaToLua.coerce(api));
        LuaValue result = definition.hook(runtimeContext, "onLeftClick").call(runtimeContext);

        assertTrue(result.toboolean());
        assertEquals(2, api.abilityCheckedAtIndex);
        assertEquals(2, api.cooldownCheckedAtIndex);
    }

    private static final class TestLuaApi {

        private int abilityCheckedAtIndex = -1;
        private int cooldownCheckedAtIndex = -1;

        public boolean isAbilityReady(int index) {
            abilityCheckedAtIndex = index;
            return index == 2;
        }

        public float getRemainingCooldown(int index) {
            cooldownCheckedAtIndex = index;
            return 15.0f;
        }
    }
}
