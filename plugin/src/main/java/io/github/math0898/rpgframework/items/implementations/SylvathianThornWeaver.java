package io.github.math0898.rpgframework.items.implementations;

import io.github.math0898.rpgframework.PlayerManager;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.items.ItemManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static io.github.math0898.rpgframework.RPGFramework.getPlugin;

/**
 * When right-clicked the SylvathianThornWeaver summons an aura of thorns which damages nearby players.
 *
 * @author Sugaku
 */ // todo: Refactor to a single Interact event that then filters down to specific materials, then specific items.
public class SylvathianThornWeaver implements Listener {

    /**
     * The last time in Millis that players have used this item.
     */
    private final Map<UUID, Long> lastUsed = new HashMap<>();

    /**
     * Called whenever a player interacts with the world.
     *
     * @param event The player interact event.
     */
    @EventHandler
    public void onInteract (PlayerInteractEvent event) {
        if (!event.hasItem()) return;
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR)) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!item.getType().equals(Material.EMERALD)) return;
        if (item.equals(ItemManager.getInstance().getItem("feyrith:SylvathianThornWeaver")))
            itemUse(event.getPlayer());
    }

    /**
     * Uses this item on this player.
     *
     * @param player The player to use the event.
     */
    public void itemUse (Player player) {
        Long lng = lastUsed.get(player.getUniqueId());
        if (lng != null)
            if (System.currentTimeMillis() - lng < 60000) {
                player.sendMessage(ChatColor.GRAY + "That is on cooldown for another: " + (60000 + lng - System.currentTimeMillis()) / 1000L + "s.");
                return;
            }
        lastUsed.put(player.getUniqueId(), System.currentTimeMillis());
        Location location = player.getLocation();
        final UUID uuid = player.getUniqueId();
        BukkitTask task1 = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            Player p = Bukkit.getPlayer(uuid);
            p.getWorld().playSound(location, Sound.BLOCK_AZALEA_LEAVES_HIT, 2.0f, 1.0f);
            p.getWorld().playSound(location, Sound.BLOCK_CHERRY_LEAVES_HIT, 2.0f, 1.0f);
            p.getWorld().playSound(location, Sound.BLOCK_AZALEA_LEAVES_BREAK, 2.0f, 1.0f);
            p.getWorld().playSound(location, Sound.BLOCK_GRASS_BREAK, 2.0f, 1.0f);
            p.getWorld().playSound(location, Sound.BLOCK_GRASS_FALL, 2.0f, 1.0f);
        }, 0, 7);
        BukkitTask task2 = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            Random rand = new Random();
            Player p = Bukkit.getPlayer(uuid);
            for (int i = 0; i < 50; i++)
                p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add((rand.nextDouble() * 8.0) - 4.0, (rand.nextDouble() * 2.0) - 1.0, (rand.nextDouble() * 8.0) - 4.0), 2);
        }, 0, 9);
        BukkitTask task3 = Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
            RpgPlayer rpg = PlayerManager.getPlayer(uuid);
            if (rpg == null) return;
            rpg.nearbyEnemyCasterTargets(4.0).forEach((e) -> { // todo: Will need to do something like this for advanced damage.
                e.damage(10.0 / 5.0, rpg.getBukkitPlayer());
                if (e.getNoDamageTicks() == 0) e.setNoDamageTicks(0);
            });
        }, 0, 20);
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            task1.cancel();
            task2.cancel();
            task3.cancel();
        }, 20 * 10);
    }
}
