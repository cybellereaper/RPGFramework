package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.lua.LuaClassDefinition;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public abstract class LuaBackedClass extends AbstractClass {

    private final Map<EquipmentSlot, Material> requiredArmor;

    protected LuaBackedClass(RpgPlayer player, String classKey) {
        super(player);

        LuaClassDefinition definition = LuaClassConfigProvider.getInstance().getRequired(classKey);
        this.requiredArmor = definition.requiredArmor();

        setClassItems(definition.classItems());
        setCooldowns(createCooldowns(definition.cooldownSeconds()));
    }

    protected boolean hasRequiredArmor(EquipmentSlot slot) {
        Material requiredMaterial = requiredArmor.get(slot);
        if (requiredMaterial == null) {
            return true;
        }

        EntityEquipment equipment = getPlayer().getBukkitPlayer().getEquipment();
        if (equipment == null) {
            return false;
        }

        ItemStack equippedItem = equipment.getItem(slot);
        return equippedItem != null && equippedItem.getType() == requiredMaterial;
    }

    private Cooldown[] createCooldowns(int[] cooldownSeconds) {
        Cooldown[] cooldowns = new Cooldown[cooldownSeconds.length];
        for (int i = 0; i < cooldownSeconds.length; i++) {
            cooldowns[i] = new Cooldown(cooldownSeconds[i]);
        }
        return cooldowns;
    }
}
