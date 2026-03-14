package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.classes.lua.LuaClassDefinition;
import io.github.math0898.rpgframework.classes.lua.LuaClassDefinitionRegistry;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.Set;

public final class LuaClassConfigProvider {

    private static LuaClassConfigProvider instance;

    private final LuaClassDefinitionRegistry registry;

    public LuaClassConfigProvider(LuaClassDefinitionRegistry registry) {
        this.registry = registry;
    }

    public static void initialize(LuaClassConfigProvider provider) {
        instance = provider;
    }

    public static LuaClassConfigProvider getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LuaClassConfigProvider is not initialized");
        }
        return instance;
    }

    public LuaClassDefinition getRequired(String classKey) {
        LuaClassDefinition definition = registry.get(classKey);
        if (definition == null) {
            throw new IllegalStateException("Missing Lua class definition for: " + classKey);
        }
        return definition;
    }

    public int[] cooldowns(String classKey) {
        return getRequired(classKey).cooldownSeconds();
    }

    public Set<Material> classItems(String classKey) {
        return getRequired(classKey).classItems();
    }

    public Map<EquipmentSlot, Material> requiredArmor(String classKey) {
        return getRequired(classKey).requiredArmor();
    }
}
