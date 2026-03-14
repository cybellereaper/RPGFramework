package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.lua.LuaClassDefinition;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LuaScriptedClass extends LuaBackedClass {

    private final String classKey;
    private final LuaClassDefinition definition;
    private final org.luaj.vm2.LuaTable runtimeContext;

    public LuaScriptedClass(RpgPlayer player, String classKey) {
        super(player, classKey);
        this.classKey = classKey;
        this.definition = LuaClassConfigProvider.getInstance().getRequired(classKey);
        this.runtimeContext = definition.createRuntimeContext(CoerceJavaToLua.coerce(this));
    }

    @Override
    public void passive() {
        invokeHook("passive");
    }

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        invokeHook("onLeftClick", CoerceJavaToLua.coerce(event));
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        invokeHook("onRightClick", CoerceJavaToLua.coerce(event));
    }

    @Override
    public boolean onDeath() {
        LuaValue result = invokeHook("onDeath");
        return result.isboolean() ? result.toboolean() : true;
    }

    @Override
    public void damaged(AdvancedDamageEvent event) {
        invokeHook("onDamaged", CoerceJavaToLua.coerce(event));
    }

    @Override
    public void attack(AdvancedDamageEvent event) {
        invokeHook("onAttack", CoerceJavaToLua.coerce(event));
    }

    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        return hasRequiredArmor(slot);
    }

    public boolean isAbilityReady(int cooldownIndex) {
        return offCooldown(cooldownIndex);
    }

    public void restartAbilityCooldown(int cooldownIndex) {
        if (getCooldown(cooldownIndex) != null) {
            getCooldown(cooldownIndex).restart();
        }
    }

    public float getRemainingAbilityCooldown(int cooldownIndex) {
        return getCooldown(cooldownIndex) == null ? 0.0f : getCooldown(cooldownIndex).getRemaining();
    }

    public float getRemainingCooldown(int cooldownIndex) {
        return getRemainingAbilityCooldown(cooldownIndex);
    }

    public boolean hasRequiredArmorSet() {
        return correctArmor();
    }

    public void sendClassMessage(String message) {
        send(message);
    }

    public void addPotion(String potionEffectTypeName, int durationSeconds, int amplifier, boolean ambient, boolean hideParticles) {
        PotionEffectType type = PotionEffectType.getByName(potionEffectTypeName);
        if (type != null) {
            getPlayer().addPotionEffect(type, secondsToTicks(durationSeconds), amplifier, ambient, hideParticles);
        }
    }

    public void addPoisonedBladeEffects(AdvancedDamageEvent event, int durationSeconds) {
        if (!(event.getEntity() instanceof org.bukkit.entity.LivingEntity target)) {
            return;
        }

        List<PotionEffectType> effects = List.of(PotionEffectType.BLINDNESS, PotionEffectType.POISON, PotionEffectType.SLOWNESS);
        for (PotionEffectType effectType : effects) {
            target.addPotionEffect(new PotionEffect(effectType, secondsToTicks(durationSeconds), 0));
        }
    }

    public boolean isPrimaryPhysical(AdvancedDamageEvent event) {
        return event.getPrimaryDamage().isPhysical();
    }

    public boolean targetIsPlayer(AdvancedDamageEvent event) {
        return event.getEntity() instanceof org.bukkit.entity.Player;
    }

    public void addSlashDamage(AdvancedDamageEvent event, double amount) {
        event.addDamage(amount, DamageType.SLASH);
    }

    public void cancelDamageEvent(AdvancedDamageEvent event) {
        event.setCancelled(true);
    }

    public double randomUnit() {
        return ThreadLocalRandom.current().nextDouble();
    }

    private LuaValue invokeHook(String hookName, LuaValue... args) {
        LuaValue hook = definition.hook(runtimeContext, hookName);
        if (hook.isnil()) {
            return LuaValue.NIL;
        }

        LuaValue[] finalArgs = new LuaValue[args.length + 1];
        finalArgs[0] = runtimeContext;
        System.arraycopy(args, 0, finalArgs, 1, args.length);

        try {
            return switch (finalArgs.length) {
                case 0 -> hook.call();
                case 1 -> hook.call(finalArgs[0]);
                case 2 -> hook.call(finalArgs[0], finalArgs[1]);
                default -> hook.invoke(LuaValue.varargsOf(finalArgs)).arg1();
            };
        } catch (RuntimeException exception) {
            RPGFramework.console("Lua hook failed for " + classKey + "." + hookName + ": " + exception.getMessage(), ChatColor.RED);
            return LuaValue.NIL;
        }
    }
}
