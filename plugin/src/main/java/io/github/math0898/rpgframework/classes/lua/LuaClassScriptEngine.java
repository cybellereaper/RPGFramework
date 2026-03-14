package io.github.math0898.rpgframework.classes.lua;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public final class LuaClassScriptEngine {

    private static final String KEY_ID = "id";
    private static final String KEY_CLASS_ITEMS = "classItems";
    private static final String KEY_REQUIRED_ARMOR = "requiredArmor";
    private static final String KEY_COOLDOWNS = "cooldowns";

    private static final String KEY_HEAD = "HEAD";
    private static final String KEY_CHEST = "CHEST";
    private static final String KEY_LEGS = "LEGS";
    private static final String KEY_FEET = "FEET";

    public LuaClassDefinition parse(String scriptText) {
        Globals globals = JsePlatform.standardGlobals();
        LuaTable classTable = expectTable(globals.load(scriptText, "class-script").call(), "Script must return a table");

        String id = classTable.get(KEY_ID).optjstring("lua-class");
        Set<Material> classItems = parseClassItems(expectTable(classTable.get(KEY_CLASS_ITEMS), "Missing 'classItems' table"));
        EnumMap<EquipmentSlot, Material> requiredArmor = parseRequiredArmor(
                expectTable(classTable.get(KEY_REQUIRED_ARMOR), "Missing 'requiredArmor' table")
        );
        int[] cooldowns = parseCooldowns(expectTable(classTable.get(KEY_COOLDOWNS), "Missing 'cooldowns' table"));

        return new LuaClassDefinition(id, classItems, requiredArmor, cooldowns, classTable);
    }

    private Set<Material> parseClassItems(LuaTable itemsTable) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (int i = 1; ; i++) {
            LuaValue value = itemsTable.get(i);
            if (value.isnil()) {
                break;
            }
            materials.add(resolveEnum(Material.class, value.checkjstring(), "class item"));
        }
        return materials;
    }

    private EnumMap<EquipmentSlot, Material> parseRequiredArmor(LuaTable armorTable) {
        EnumMap<EquipmentSlot, Material> requiredArmor = new EnumMap<>(EquipmentSlot.class);
        putArmor(armorTable, requiredArmor, KEY_HEAD, EquipmentSlot.HEAD);
        putArmor(armorTable, requiredArmor, KEY_CHEST, EquipmentSlot.CHEST);
        putArmor(armorTable, requiredArmor, KEY_LEGS, EquipmentSlot.LEGS);
        putArmor(armorTable, requiredArmor, KEY_FEET, EquipmentSlot.FEET);
        return requiredArmor;
    }

    private void putArmor(LuaTable armorTable, EnumMap<EquipmentSlot, Material> requiredArmor, String luaKey, EquipmentSlot slot) {
        LuaValue value = armorTable.get(luaKey);
        if (!value.isnil()) {
            requiredArmor.put(slot, resolveEnum(Material.class, value.checkjstring(), "armor material"));
        }
    }

    private int[] parseCooldowns(LuaTable cooldownTable) {
        int[] cooldowns = new int[cooldownTable.length()];
        for (int i = 1; i <= cooldownTable.length(); i++) {
            int cooldown = cooldownTable.get(i).checkint();
            if (cooldown < 0) {
                throw new IllegalArgumentException("Cooldown values must be non-negative");
            }
            cooldowns[i - 1] = cooldown;
        }
        return cooldowns;
    }

    private LuaTable expectTable(LuaValue value, String message) {
        if (value == null || value.isnil() || !value.istable()) {
            throw new IllegalArgumentException(message);
        }
        return (LuaTable) value;
    }

    private <E extends Enum<E>> E resolveEnum(Class<E> enumType, String rawValue, String label) {
        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid " + label + ": " + rawValue, exception);
        }
    }
}
