package io.github.math0898.rpgframework.classes;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.damage.events.AdvancedDamageEvent;
import io.github.math0898.rpgframework.damage.events.LethalDamageEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The AbstractClass implements the basics of what makes up a class, and some
 * protected utility methods, to make
 * specific implementations easier.
 *
 * @author Sugaku
 */
public abstract class AbstractClass implements Class, Listener {

    /**
     * The RpgPlayer that this AbstractClass is referencing.
     */
    private final RpgPlayer player;

    /**
     * An array of cooldowns.
     */
    private Cooldown[] cooldowns = new Cooldown[0]; // todo: Each implementing class could use a self-defined enum with
                                                    // ability names to index.

    /**
     * A list of items that are considered class items.
     */
    private final List<Material> classItems = new ArrayList<>();

    /**
     * Creates a new AbstractClass object which is specific to the given player.
     *
     * @param p The player this class is specific to.
     */
    public AbstractClass(RpgPlayer p) {
        this(p, true);
    }

    /**
     * Creates a new AbstractClass object which is specific to the given player.
     *
     * @param p                The player this class is specific to.
     * @param registerListener Whether this class should register itself as a Bukkit
     *                         listener.
     */
    protected AbstractClass(RpgPlayer p, boolean registerListener) {
        player = p;
        if (registerListener && RPGFramework.getInstance() != null) {
            Bukkit.getPluginManager().registerEvents(this, RPGFramework.getInstance());
        }
    }

    /**
     * An accessor method to get the RpgPlayer that this class references.
     *
     * @return The RpgPlayer that this class references.
     */
    public RpgPlayer getPlayer() {
        return player;
    }

    /**
     * Sets all the cooldowns that will be part of this AbstractClass.
     *
     * @param cooldowns The array of cooldowns that are part of this class.
     */
    protected void setCooldowns(Cooldown[] cooldowns) {
        this.cooldowns = cooldowns == null ? new Cooldown[0] : cooldowns;
    }

    /**
     * An accessor method for the entire cooldown array.
     *
     * @return The cooldown array that is a member of this class.
     */
    public Cooldown[] getCooldowns() {
        return cooldowns;
    }

    /**
     * A helpful utility method that checks if a cooldown is finished and sends a
     * message if it still is on cooldown.
     *
     * @param i The index of the cooldown we're interested in.
     * @return True if the ability is off cooldown.
     */
    protected boolean offCooldown(int i) {
        Cooldown cooldown = getCooldown(i);
        if (player == null || cooldown == null)
            return true; // Not sure how we got here.
        if (cooldown.isComplete())
            return true;
        send("That ability is on cooldown for another " + cooldown.getRemaining() + "s.");
        return false;
    }

    /**
     * Gets a specific cooldown entry if it exists.
     *
     * @param index The cooldown index.
     * @return The cooldown at the given index or null if it does not exist.
     */
    protected Cooldown getCooldown(int index) {
        if (index < 0 || index >= cooldowns.length)
            return null;
        return cooldowns[index];
    }

    /**
     * A helpful utility method to send a message within the AbstractClass instead
     * of on the RpgPlayer object.
     *
     * @param message The message to send to the player.
     */
    protected void send(String message) {
        if (player == null)
            return;
        player.sendMessage(message);
    }

    /**
     * Sets the class items for this class so that {@link #isClassItem(Material)}
     * can be used.
     *
     * @param mats The materials to add as class items.
     */
    protected void setClassItems(Material... mats) {
        if (mats != null)
            for (Material m : mats)
                if (m != null && !classItems.contains(m))
                    classItems.add(m);
    }

    /**
     * Checks whether the given item is a class item or not.
     *
     * @param mat The material to check whether it is a class item.
     * @return The index of the class item in the ClassItems array. -1 If it is not
     *         a class item.
     */
    protected int isClassItem(Material mat) {
        return classItems.indexOf(mat);
    }

    /**
     * Called once every 20 seconds to apply a passive effect to the player.
     */
    @Override
    public void passive() {

    }

    /**
     * Called whenever the player attached to this class object interacts with the
     * world.
     *
     * @param event The player interact event.
     */
    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (!event.hasItem())
            return;
        ItemStack item = event.getItem();
        if (item == null)
            return;
        Material type = item.getType();
        if (isClassItem(type) == -1)
            return;
        switch (event.getAction()) {
            case RIGHT_CLICK_BLOCK, RIGHT_CLICK_AIR -> onRightClickCast(event, type);
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> onLeftClickCast(event, type);
            default -> {
            }
        }
    }

    /**
     * Called whenever a player left-clicks while holding a class item. To reach
     * this method, the player must be holding
     * a class item. No promises are made if they're wearing armor or not.
     *
     * @param event The PlayerInteractEvent that lead to this method being called.
     * @param type  The type of material that was used in this cast.
     */
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {

    }

    /**
     * Called whenever a player right-clicks while holding a class item. To reach
     * this method, the player must be
     * holding a class item. No promises are made if they're wearing armor or not.
     *
     * @param event The PlayerInteractEvent that lead to this method being called.
     * @param type  The type of material that was used in this cast.
     */
    public void onRightClickCast(PlayerInteractEvent event, Material type) {

    }

    /**
     * Called when the class user has 'died'.
     *
     * @return Whether a death should be respected or not.
     */
    @Deprecated
    @Override
    public boolean onDeath() {
        return true;
    }

    /**
     * Called whenever a lethal amount of damage would be inflicted upon the holder
     * of this class.
     *
     * @param event The LethalDamageEvent to consider.
     */
    @EventHandler
    public void onLethalDamage(LethalDamageEvent event) {
        if (player != null && !player.getBukkitPlayer().isOnline())
            HandlerList.unregisterAll(this);
    }

    /**
     * Checks whether this player is wearing the armor type for their class or not.
     *
     * @return True if the player is wearing an entire suit of the right armor.
     */
    @Override
    public boolean correctArmor() {
        return correctArmor(EquipmentSlot.HEAD)
                && correctArmor(EquipmentSlot.CHEST)
                && correctArmor(EquipmentSlot.LEGS)
                && correctArmor(EquipmentSlot.FEET);
    }

    /**
     * Checks a specific slot to see if this player is wearing their class armor or
     * not.
     *
     * @param slot The slot to check to see if it is the correct armor type.
     * @return True if the player is wearing the correct armor for that slot.
     */
    @Override
    public boolean correctArmor(EquipmentSlot slot) {
        return true;
    }

    /**
     * Called whenever this DamageModifier is relevant on a defensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void damaged(AdvancedDamageEvent event) {

    }

    /**
     * Called whenever this DamageModifier is relevant on an offensive front.
     *
     * @param event The AdvancedDamageEvent to consider.
     */
    @Override
    public void attack(AdvancedDamageEvent event) {

    }
}
