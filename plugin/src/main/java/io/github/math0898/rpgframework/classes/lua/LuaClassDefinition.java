package io.github.math0898.rpgframework.classes.lua;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

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
    private final LuaTable scriptTable;

    public LuaClassDefinition(
            String id,
            Set<Material> classItems,
            Map<EquipmentSlot, Material> requiredArmor,
            int[] cooldownSeconds,
            LuaTable scriptTable
    ) {
        this.id = id;
        this.classItems = classItems == null
                ? EnumSet.noneOf(Material.class)
                : EnumSet.copyOf(classItems);
        this.requiredArmor = requiredArmor == null
                ? new EnumMap<>(EquipmentSlot.class)
                : new EnumMap<>(requiredArmor);
        this.cooldownSeconds = cooldownSeconds == null ? new int[0] : cooldownSeconds.clone();
        this.scriptTable = scriptTable;
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

    public LuaValue hook(String functionName) {
        return scriptTable.get(functionName);
    }

    public LuaValue hook(LuaTable runtimeContext, String functionName) {
        if (runtimeContext == null) {
            return LuaValue.NIL;
        }

        LuaValue hook = runtimeContext.rawget(functionName);
        return hook.isfunction() ? hook : LuaValue.NIL;
    }

    public LuaTable createRuntimeContext(LuaValue javaApi) {
        LuaTable runtimeContext = new LuaTable();
        LuaValue key = LuaValue.NIL;

        while (true) {
            LuaValue next = scriptTable.next(key).arg1();
            if (next.isnil()) {
                break;
            }

            runtimeContext.set(next, scriptTable.get(next));
            key = next;
        }

        LuaTable metatable = new LuaTable();
        metatable.set("__index", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                if (javaApi == null || javaApi.isnil()) {
                    return LuaValue.NIL;
                }

                LuaValue key = args.arg(2);
                LuaValue member = javaApi.get(key);
                if (!member.isfunction()) {
                    return member;
                }

                return new VarArgFunction() {
                    @Override
                    public Varargs invoke(Varargs methodArgs) {
                        int narg = methodArgs.narg();
                        LuaValue[] forwardedArgs = new LuaValue[Math.max(1, narg)];
                        forwardedArgs[0] = javaApi;
                        for (int i = 2; i <= narg; i++) {
                            forwardedArgs[i - 1] = methodArgs.arg(i);
                        }
                        return member.invoke(LuaValue.varargsOf(forwardedArgs));
                    }
                };
            }
        });
        runtimeContext.setmetatable(metatable);
        return runtimeContext;
    }
}
