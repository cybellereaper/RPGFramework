package io.github.math0898.rpgframework.hooks;

import io.github.math0898.rpgframework.RPGFramework;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import uk.antiperson.stackmob.events.StackMergeEvent;
import uk.antiperson.stackmob.events.StackSpawnEvent;

/**
 * Hooks into the StackMobs plugin to prevent stacking of RPG mobs.
 *
 * @author Sugaku
 */
public class StackMobsHook implements Listener {

    /**
     * Whether the StackMob is present on the server or not.
     */
    private final boolean isStackMob;

    /**
     * The active StackMobsHook instance.
     */
    private static StackMobsHook instance = null;

    /**
     * Creates the StackMobsHook by attempting to hook into StackMobs.
     */
    private StackMobsHook () {
        isStackMob = Bukkit.getPluginManager().isPluginEnabled("StackMob");
        if (isStackMob) {
            RPGFramework.console("Found StackMobs. Registering hook to prevent stacking RPG mobs.", ChatColor.GREEN);
            Bukkit.getPluginManager().registerEvents(this, RPGFramework.getPlugin());
        }
    }

    /**
     * An accessor method for the active StackMobs instance.
     *
     * @return The active StackMobs instance.
     */
    public static StackMobsHook getInstance() {
        if (instance == null) instance = new StackMobsHook();
        return instance;
    }

    /**
     * An accessor method for whether StackMob is present on the server or not.
     *
     * @return Whether StackMob is enabled or not.
     */
    public boolean isStackMob () {
        return isStackMob;
    }

    /**
     * Called whenever StackMob attempts to create a new mob stack.
     *
     * @param event The mob stack spawn event.
     */
    @EventHandler
    public void onStack (StackSpawnEvent event) {
        if (event.getLivingEntity().isCustomNameVisible()) {
            String customName = event.getLivingEntity().getCustomName();
            if (customName == null) return;
            if (customName.contains(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] "))
                event.setCancelled(true);
        }
    }

    /**
     * Called whenever StackMob attempts to merge two mob stacks.
     *
     * @param event The mob stack merge event.
     */
    @EventHandler
    public void onMerge (StackMergeEvent event) {
        LivingEntity e1 = event.getStackEntity().getEntity();
        LivingEntity e2 = event.getNearbyStackEntity().getEntity();
        for (LivingEntity e : new LivingEntity[]{e1, e2})
            if (e.isCustomNameVisible()) {
                String customName = e.getCustomName();
                if (customName == null) return;
                if (customName.contains(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "RPG" + ChatColor.DARK_GRAY + "] ")) {
                    event.setCancelled(true);
                    return;
                }
            }
    }
}
