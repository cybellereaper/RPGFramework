package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import io.github.math0898.rpgframework.damage.events.LethalDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Base implementation for player classes.
 */
public abstract class AbstractClass implements Class, Listener {

    private static final String COOLDOWN_MESSAGE_TEMPLATE = "That ability is on cooldown for another %ss.";

    private final RpgPlayer player;
    private Cooldown[] cooldowns = new Cooldown[0];
    private final Set<Material> classItems = EnumSet.noneOf(Material.class);

    protected AbstractClass(RpgPlayer player) {
        this(player, true);
    }

    protected AbstractClass(RpgPlayer player, boolean registerListener) {
        this.player = player;
        registerListenerIfNeeded(registerListener);
    }

    public RpgPlayer getPlayer() {
        return player;
    }

    protected void setCooldowns(Cooldown[] cooldowns) {
        this.cooldowns = cooldowns == null ? new Cooldown[0] : Arrays.copyOf(cooldowns, cooldowns.length);
    }

    public Cooldown[] getCooldowns() {
        return Arrays.copyOf(cooldowns, cooldowns.length);
    }

    protected Cooldown getCooldown(int index) {
        if (index < 0 || index >= cooldowns.length) {
            return null;
        }
        return cooldowns[index];
    }

    protected boolean offCooldown(int index) {
        Cooldown cooldown = getCooldown(index);
        if (cooldown == null || player == null) {
            return true;
        }

        if (cooldown.isComplete()) {
            return true;
        }

        send(String.format(COOLDOWN_MESSAGE_TEMPLATE, cooldown.getRemaining()));
        return false;
    }

    protected boolean offCooldown(Enum<?> ability) {
        return offCooldown(ability.ordinal());
    }

    protected float getRemainingCooldown(Enum<?> ability) {
        Cooldown cooldown = getCooldown(ability.ordinal());
        return cooldown == null ? 0.0f : cooldown.getRemaining();
    }

    protected void restartCooldown(Enum<?> ability) {
        Cooldown cooldown = getCooldown(ability.ordinal());
        if (cooldown != null) {
            cooldown.restart();
        }
    }

    protected static int secondsToTicks(int seconds) {
        return seconds * 20;
    }

    protected void send(String message) {
        if (player != null) {
            player.sendMessage(message);
        }
    }


    protected void setClassItems(Set<Material> materials) {
        classItems.clear();
        if (materials == null) {
            return;
        }

        materials.stream()
                .filter(Objects::nonNull)
                .forEach(classItems::add);
    }

    protected void setClassItems(Material... materials) {
        if (materials == null) {
            return;
        }

        Arrays.stream(materials)
                .filter(Objects::nonNull)
                .forEach(classItems::add);
    }

    protected boolean isClassItem(Material material) {
        return classItems.contains(material);
    }

    protected Set<Material> getClassItems() {
        return Collections.unmodifiableSet(classItems);
    }

    @Override
    public void passive() {
    }

    @Override
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }

        Material material = item.getType();
        if (!isClassItem(material)) {
            return;
        }

        Action action = event.getAction();
        if (isRightClick(action)) {
            onRightClickCast(event, material);
            return;
        }

        if (isLeftClick(action)) {
            onLeftClickCast(event, material);
        }
    }

    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
    }

    public void onRightClickCast(PlayerInteractEvent event, Material type) {
    }

    @Deprecated
    @Override
    public boolean onDeath() {
        return true;
    }

    @EventHandler
    public void onLethalDamage(LethalDamageEvent event) {
        if (player == null) {
            return;
        }

        if (!player.getBukkitPlayer().isOnline()) {
            HandlerList.unregisterAll(this);
        }
    }

    @Override
    public boolean correctArmor() {
        return correctArmor(EquipmentSlot.HEAD)
                && correctArmor(EquipmentSlot.CHEST)
                && correctArmor(EquipmentSlot.LEGS)
                && correctArmor(EquipmentSlot.FEET);
    }

    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        return true;
    }

    @Override
    public void damaged(AdvancedDamageEvent event) {
    }

    @Override
    public void attack(AdvancedDamageEvent event) {
    }

    private void registerListenerIfNeeded(boolean registerListener) {
        if (!registerListener) {
            return;
        }

        RPGFramework plugin = RPGFramework.getPlugin();
        if (plugin != null) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    private boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    private boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }
}
