package sugaku.rpg.mobs.teir1.krusk;

import io.github.math0898.rpgframework.enemies.CustomMob;
import io.github.math0898.rpgframework.items.EquipmentSlots;
import io.github.math0898.utils.items.ItemBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import sugaku.rpg.framework.items.Rarity;

/**
 * KruskMinion describes what makes Krusk minions Krusk minions. There is not much extra to Krusk minions outside of
 * their equipment and potential names.
 */
public class KruskMinion extends CustomMob {

    /**
     * Here are the nine names of Krusk's minions. A name is selected randomly and then from that their equipment is
     * selected from the array bellow. The last three are considered tanky bois and have additional health but reduced
     * damage.
     */
    private static final String[] names = { "Bagazvaut", "Thundag", "Druukrug", "Giaborg", "Drinva", "Nerark", "Hrakteth", "Hostruurm", "Thordrol" };

    /**
     * The weapons of each minion of Krusk. Each minion depending on the name will spawn with the same weapon every time.
     */
    private static final Material[] weapons = { Material.NETHERITE_SWORD, Material.IRON_AXE, Material.STICK, Material.IRON_SWORD, Material.IRON_SHOVEL, Material.IRON_SHOVEL, Material.DIAMOND_SWORD, Material.IRON_HOE, Material.DIAMOND_HOE};

    /**
     * Constructor which spawns a KruskMinion at the given location l. It also handles some outside things such as
     * equipping items and max health.
     * @param l The location the Krusk minion should spawn at.
     * @param i The index of the Krusk minion we should spawn at the given location.
     */
    public KruskMinion(Location l, int i) {
        super(names[i] + ", Underling of Krusk", EntityType.ZOMBIE, Rarity.UNIQUE, 40);
        ItemBuilder main = new ItemBuilder(weapons[i]).setUnbreakable(true);
        ItemBuilder boots = new ItemBuilder(Material.LEATHER_BOOTS).setUnbreakable(true);
        ItemBuilder leggings = new ItemBuilder(Material.NETHERITE_LEGGINGS).setUnbreakable(true);
        ItemBuilder chestplate = new ItemBuilder(Material.NETHERITE_CHESTPLATE).setUnbreakable(true);
        ItemBuilder helmet = new ItemBuilder(Material.LEATHER_HELMET).setUnbreakable(true);
        if (i > 5) {
            setBaseHealth(60);
            boots.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, 0.12, EquipmentSlots.FEET);
            boots.addAttributeModifier(Attribute.GENERIC_ARMOR, -17, EquipmentSlots.FEET);
            main.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 1.0, EquipmentSlots.HAND);
            setBaseHealth(60);
         } else {
            boots.addAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED, 0.18, EquipmentSlots.FEET);
            boots.addAttributeModifier(Attribute.GENERIC_ARMOR, -11, EquipmentSlots.FEET);
            leggings.setMaterial(Material.LEATHER_LEGGINGS);
            chestplate.setMaterial(Material.IRON_CHESTPLATE);
            main.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, 2.5, EquipmentSlots.HAND);
        }
        setBoots(boots.build());
        setLeggings(leggings.build());
        setChestplate(chestplate.build());
        setHelm(helmet.build());
        setHand(main.build());
        spawn(l);

        ((Ageable) getEntity()).setAdult();
    }

    /**
     * Does all the drops for Krusk minions. They shouldn't drop any items and instead only drop 10xp which is still
     * more than most hostile mobs.
     * @param e The EntityDeathEvent of the Krusk minion which is being handled.
     */
    public static void handleDrops(EntityDeathEvent e) {

        CustomMob.handleDrops(e);

        e.setDroppedExp(10);
        e.getDrops().clear();
    }
}
