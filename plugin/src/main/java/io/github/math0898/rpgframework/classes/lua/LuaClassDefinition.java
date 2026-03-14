package io.github.math0898.rpgframework.classes.lua;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class LuaClassDefinition {

    private final String id;
    private final Set<Material> classItems;
    private final Map<EquipmentSlot, Material> requiredArmor;
    private final int[] cooldownSeconds;

    public LuaClassDefinition(
            String id,
            Set<Material> classItems,
            Map<EquipmentSlot, Material> requiredArmor,
            int[] cooldownSeconds
    ) {
        this.id = id;
        this.classItems = classItems == null
                ? EnumSet.noneOf(Material.class)
                : EnumSet.copyOf(classItems);
        this.requiredArmor = requiredArmor == null
                ? new EnumMap<>(EquipmentSlot.class)
                : new EnumMap<>(requiredArmor);
        this.cooldownSeconds = cooldownSeconds == null ? new int[0] : cooldownSeconds.clone();
    }

    public String id() {
        return id;
    }

    public Set<Material> classItems() {
        return Collections.unmodifiableSet(classItems);
    }

    public Map<EquipmentSlot, Material> requiredArmor() {
        return Collections.unmodifiableMap(requiredArmor);
    }

    public int[] cooldownSeconds() {
        return cooldownSeconds.clone();
    }
}
