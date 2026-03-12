package sugaku.rpg.mobs.teir1.eiryeras;

import io.github.math0898.rpgframework.items.EquipmentSlots;
import io.github.math0898.utils.Utils;
import io.github.math0898.utils.items.ItemBuilder;
import io.github.math0898.rpgframework.items.ItemManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValueAdapter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sugaku.rpg.framework.items.BossDrop;
import sugaku.rpg.framework.items.Rarity;
import io.github.math0898.rpgframework.enemies.CustomMob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Random;

import static org.bukkit.Material.*;

/**
 * This class in its entirety declares the Eiryeras boss and handles all of its special effects against players.
 */
public class EiryerasBoss extends CustomMob implements Listener {


    /**
     * A simple enum to distinguish between the different types of arrows Eiryeras fires
     */

    enum Arrows { SLOWNESS, KNOCKBACK, POISON }

    /**
     * The arrowType class holds a state of the Arrows enum which can be stored as metadata on a game object.
     */
    static class arrowType extends MetadataValueAdapter {

        /**
         * The actual object which holds the metadata information.
         */
        private final Arrows arrow;

        /**
         * The basic constructor for the arrowType class. It just needs the state it needs to remember.
         *
         * @param owningPlugin The plugin which owns the meta data. Inherited from MetadataValueAdapter.
         * @param data The data the object should hold.
         */
        protected arrowType(Plugin owningPlugin, Arrows data) { super(owningPlugin); arrow = data;}

        /**
         * Serves as the accessor for the actual information held in the object.
         *
         * @return The value of arrow which was given at construction.
         */
        @Override
        public Object value() { return arrow; }

        /**
         * Invalidates the information of the object. There isn't anything to do. Ideally we'd delete the item however
         * we can leave that to java clean up.
         */
        @Override
        public void invalidate() { }
    }

    /**
     * The name of the boss. This is then modified to create the display name including the plugin prefix and color
     * rarity of the boss.
     */
    private static final String name = "Eiryeras, Honored Hunter";

    /**
     * The base damage for any attack done by Eiryeras.
     */
    private static final double baseDamage = 8.0;

    /**
     * The damage a slowness arrow from Eiryeras does.
     */
    private static final double slownessDamage = baseDamage + 0.0; //5.0

    /**
     * The damage a poison arrow from Eiryeras does.
     */
    private static final double poisonDamage = baseDamage - 2.5; //2.5

    /**
     * The damage a stronger, 'knockback', arrow from Eiryeras does.
     */
    private static final double knockbackDamage = baseDamage + 3.0; //8.0

    /**
     * The damage Eiryeras does when striking with their sword.
     */
    private static final double meleeDamage = baseDamage + 5.0; //10.0

    /**
     * Eiryeras' movement speed bonus in melee form.
     */
    private static final double bonusMoveSpeed = 0.25;

    /**
     * An array which stores all of the boss item drops.
     */
    private static final BossDrop[] bossDrops = new BossDrop[]{
            //Rare Eiryeras' Bow
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:HandCraftedBow"), Rarity.RARE),

            //Uncommon Eiryeras' Boots
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:WornGaloshes"), Rarity.UNIQUE),

            //Uncommon Eiryeras' Leggings
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:HideLeggings"), Rarity.UNIQUE),

            //Uncommon Eiryeras' Chestplate
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:HuntersCloak"), Rarity.UNIQUE),

            //Uncommon Eiryeras' Helmet
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:HuntersHood"), Rarity.UNIQUE),

            //Rare Eiryeras' Knife
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:CeremonialKnife"), Rarity.RARE),

            //Legendary Eiryeras' Lore
            new BossDrop(ItemManager.getInstance().getItem("eiryeras:Lore"), Rarity.LEGENDARY)
    };

    /**
     * The hunter's knife Eiryeras uses to charge down enemies and deal the finishing blow.
     */
    private static final ItemStack huntersKnife = new ItemBuilder(IRON_SWORD).setDisplayName(" ")
            .addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, meleeDamage, EquipmentSlots.HAND)
            .addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, bonusMoveSpeed, EquipmentSlots.HAND).build();

    /**
     * Makes sure that items and other things required for the boss fight are set up.
     */
    public EiryerasBoss() {
        super(name, EntityType.SKELETON, Rarity.UNIQUE, 150);
        setArmor(new ItemBuilder(LEATHER_HELMET).setColor(255, 60, 115, 31).build(),
                new ItemBuilder(Material.LEATHER_CHESTPLATE).setColor(255,  113, 172, 11).build(),
                new ItemBuilder(Material.LEATHER_LEGGINGS).setColor(255,  93, 161, 124).build(),
                new ItemBuilder(Material.LEATHER_BOOTS).setColor(255,  60, 115, 31).build());
        setHand(new ItemStack(Material.BOW, 1));
    }

    /**
     * Makes sure that items and other things required for the boss fight are set up. <b>This will also spawn the boss.</b>
     *
     * @param location The location to spawn the boss at.
     */
    public EiryerasBoss (Location location) {
        this();
        spawn(location);
    }

    /**
     * Accessor method for the boss.
     *
     * @return The name of the Eiryeras boss.
     */
    public static String getName() { return name; }

    /**
     * Returns the array of boss drop items.
     *
     * @return The boss drop items in the form of an array.
     */
    public static BossDrop[] getBossDrops() { return bossDrops; }

    /**
     * Returns the array of boss drop items stripped of their rarity property.
     *
     * @return The ItemStack array of the items in bossDrops.
     */
    public static Collection<? extends ItemStack> getBossItems() {
        ArrayList<ItemStack> items = new ArrayList<>();
        for (BossDrop b: getBossDrops()) items.add(b.getItem());
        return items;
    }

    /**
     * Handles when an Eiryeras instance fires and arrow. Since we're firing custom arrows we need to add some metadata
     * so we can find them after they hit. This method also handles the logic for when Eiryeras should go in for a kill
     * with the sword.
     *
     * @param event The entity firing a bow event. Filtered to entities with custom names, then to those which contain
     *              the constant 'name' declared above.
     */
    @EventHandler
    public void onShot(EntityShootBowEvent event) {
        if (event.getEntity().getCustomName() != null) if (event.getEntity().getCustomName().contains(name)) {
            JavaPlugin plugin = Utils.getPlugin();
            switch(determineShot(event.getEntity())) {
                case SLOWNESS: event.getProjectile().setMetadata("eiryeras", new arrowType(plugin, Arrows.SLOWNESS)); break;
                case POISON: event.getProjectile().setMetadata("eiryeras", new arrowType(plugin, Arrows.POISON)); break;
                case KNOCKBACK: event.getProjectile().setMetadata("eiryeras", new arrowType(plugin, Arrows.KNOCKBACK)); break;
            }
            for (Entity e: event.getEntity().getNearbyEntities(25, 25, 25)) {
                if (e instanceof Player) if (((Player) e).getPotionEffect(PotionEffectType.POISON) != null
                        && ((Player) e).getPotionEffect(PotionEffectType.SLOWNESS) != null ) {
                    switchToSword(event.getEntity());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> switchToBow(event.getEntity()), 60);
                    break;
                }
            }
        }
    }

    /**
     * Handles the situation when a player gets hit by an Eiryeras arrow.
     *
     * @param e The entity damaged by another entity event which then gets filtered down to situations which are a
     *          player damaged by an arrow. After that its narrowed further by arrows with the "eiryeras" metadata.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public static void onArrowHit(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Arrow) {
            Arrow a = (Arrow) e.getDamager();
            if (a.hasMetadata("eiryeras")) {
                switch((Arrows) Objects.requireNonNull(a.getMetadata("eiryeras").get(0).value())) {
                    case SLOWNESS:
                        e.setDamage(slownessDamage);
                        ((Player) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5*20, 0, true));
                        break;
                    case POISON:
                        e.setDamage(poisonDamage);
                        ((Player) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 8*20, 0, true));
                        ((Player) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2*20, 0, true));
                        break;
                    case KNOCKBACK:
                        e.setDamage(knockbackDamage);
                        break;
                }
            }
        }
    }

    /**
     * Handles the situation when an Eiryeras instance dies. Only kills caused by a player will result in drops.
     *
     * @param event The entity death event first filtered by custom name visible and then by mobs which contain
     *              Eiryeras' name in their name.
     */
    @EventHandler
    public static void onDeath(EntityDeathEvent event) {
        if (event.getEntity().getCustomName() != null) {
            if (event.getEntity().getCustomName().contains(name)) {
                CustomMob.handleDrops(event, bossDrops, Rarity.UNIQUE);
                Random r = new Random();
                LivingEntity e = event.getEntity();
                Objects.requireNonNull(e.getLocation().getWorld()).dropItem(e.getLocation(), new ItemStack(BONE, (int) (r.nextDouble() * 5) + 1));
                Objects.requireNonNull(e.getLocation().getWorld()).dropItem(e.getLocation(), new ItemStack(ARROW, (int) (r.nextDouble() * 5) + 1));
            }
        }
    }

    /**
     * Switches the entity to wielding a sword and gives them a *minor* speed boost.
     *
     * @param e The entity, presumably an instance of Eiryeras, to switch the state of.
     */
    private void switchToSword(LivingEntity e) {
        EntityEquipment equip = e.getEquipment();
        if (equip == null) return;

        equip.setItemInMainHand(huntersKnife);
        equip.setItemInMainHandDropChance(0f);
    }

    /**
     * Switches the entity to wielding a bow and removes the *minor* speed boost.
     *
     * @param e The entity, presumably an instance of Eiryeras, to switch the state of.
     */
    private void switchToBow(LivingEntity e) {
        EntityEquipment equip = e.getEquipment();
        if (equip == null) return;

        equip.setItemInMainHand(new ItemStack(Material.BOW, 1));
        equip.setItemInMainHandDropChance(0f);
    }

    /**
     * Performs some logic to determine the type of arrow Eiryeras will use. If an enemy is too close Eiryeras will use
     * knockback and otherwise alternate between poison and slowness.
     *
     * @param e The entity, presumably an instance of Eiryeras, which the logic is being determined for.
     * @return The arrow to be fired.
     */
    private Arrows determineShot(LivingEntity e) {

        int roll = new Random().nextInt();

        if (roll % 13 == 0) return Arrows.POISON;
        else if (roll % 11 == 0) return Arrows.SLOWNESS;
        else if (roll % 10 == 0) return Arrows.KNOCKBACK;

        if (e.getNearbyEntities(10.0, 10.0, 10.0).size() > 1) return Arrows.KNOCKBACK;
        else if (new Random().nextInt() % 2 == 0) return Arrows.POISON;
        else return Arrows.SLOWNESS;
    }
}
