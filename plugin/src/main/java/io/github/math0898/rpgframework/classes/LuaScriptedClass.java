package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.lua.LuaClassDefinition;
import io.github.math0898.rpgframework.damage.DamageResistance;
import io.github.math0898.rpgframework.damage.DamageType;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class LuaScriptedClass extends LuaBackedClass {

    private final String classKey;
    private final LuaClassDefinition definition;
    private final LuaTable runtimeContext;

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

    public String getCastItem(PlayerInteractEvent event) {
        if (event == null || event.getItem() == null) {
            return "";
        }
        return event.getItem().getType().name();
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

    public LuaTable getNearbyEnemyCasterTargets(double dx, double dy, double dz) {
        LuaTable table = new LuaTable();
        List<LivingEntity> entities = getPlayer().nearbyEnemyCasterTargets(dx, dy, dz);
        for (int i = 0; i < entities.size(); i++) {
            table.set(i + 1, CoerceJavaToLua.coerce(entities.get(i)));
        }
        return table;
    }

    public void playSound(String soundName, float volume, float pitch) {
        Sound sound = parseEnum(Sound.class, soundName);
        if (sound == null) {
            return;
        }

        Player player = getPlayer().getBukkitPlayer();
        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
    }

    public void spawnParticle(String particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        Particle particle = parseEnum(Particle.class, particleName);
        if (particle == null) {
            return;
        }

        Player player = getPlayer().getBukkitPlayer();
        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0, 1.0, 0),
                count,
                offsetX,
                offsetY,
                offsetZ,
                extra
        );
    }

    public void spawnParticleAtEntity(LivingEntity entity, String particleName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        if (entity == null) {
            return;
        }

        Particle particle = parseEnum(Particle.class, particleName);
        if (particle == null) {
            return;
        }

        entity.getWorld().spawnParticle(
                particle,
                entity.getLocation().add(0, 1.0, 0),
                count,
                offsetX,
                offsetY,
                offsetZ,
                extra
        );
    }

    public void launchSmallFireball(double horizontalOffset, double speed) {
        Player player = getPlayer().getBukkitPlayer();
        Vector forward = player.getEyeLocation().getDirection().normalize();
        Vector sideways = calculatePerpendicularHorizontalVector(forward);

        Vector velocity = forward.clone()
                .add(sideways.clone().multiply(horizontalOffset))
                .normalize()
                .multiply(speed);

        SmallFireball fireball = player.launchProjectile(SmallFireball.class, velocity);
        fireball.setIsIncendiary(true);
    }

    public void heal(double amount) {
        getPlayer().heal(amount);
    }

    public void setFireTicks(int ticks) {
        getPlayer().getBukkitPlayer().setFireTicks(Math.max(0, ticks));
    }

    public void extinguishAndImmuneFire(AdvancedDamageEvent event) {
        if (event == null) {
            return;
        }

        event.setResistance(DamageType.FIRE, DamageResistance.IMMUNITY);
        event.getEntity().setFireTicks(0);
    }

    public void reducePrimaryDamage(AdvancedDamageEvent event, double amount) {
        if (event == null || amount <= 0) {
            return;
        }

        event.addDamage(-amount, event.getPrimaryDamage());
    }

    public void addFireDamage(AdvancedDamageEvent event, double amount) {
        if (event == null || amount == 0) {
            return;
        }

        event.addDamage(amount, DamageType.FIRE);
    }

    public void addPoisonedBladeEffects(AdvancedDamageEvent event, int durationSeconds) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
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
        return event.getEntity() instanceof Player;
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

    private Vector calculatePerpendicularHorizontalVector(Vector direction) {
        Vector perpendicular = direction.clone().crossProduct(new Vector(0, 1, 0));
        if (perpendicular.lengthSquared() == 0) {
            return new Vector(1, 0, 0);
        }
        return perpendicular.normalize();
    }

    private <E extends Enum<E>> E parseEnum(java.lang.Class<E> enumType, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
