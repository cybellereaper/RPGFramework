package io.github.math0898.rpgframework.classes.lua;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaClassDefinitionRegistryTest {

    @Test
    void loadAllLoadsEveryClassScriptWithHooks() {
        LuaClassDefinitionRegistry registry = new LuaClassDefinitionRegistry(new LuaClassScriptEngine());

        registry.loadAll(getClass().getClassLoader());

        assertDefinitionHasHook(registry, "ASSASSIN", "onAttack");
        assertDefinitionHasHook(registry, "BARD", "onDeath");
        assertDefinitionHasHook(registry, "BERSERKER", "onLeftClick");
        assertDefinitionHasHook(registry, "PALADIN", "onRightClick");
        assertDefinitionHasHook(registry, "PYROMANCER", "onDeath");
        assertDefinitionHasHook(registry, "PYROMANCER", "onDamaged");
        assertDefinitionHasHook(registry, "PYROMANCER", "onAttack");
    }

    private void assertDefinitionHasHook(LuaClassDefinitionRegistry registry, String classKey, String hookName) {
        LuaClassDefinition definition = registry.get(classKey);
        assertNotNull(definition, "Missing class definition for " + classKey);
        assertTrue(definition.hook(hookName).isfunction(), "Missing hook " + hookName + " for " + classKey);
    }
}
