package io.github.math0898.rpgframework.items;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.Rarity;
import io.github.math0898.utils.StringUtils;
import io.github.math0898.utils.items.ItemBuilder;
import io.github.math0898.utils.items.ItemParser;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An RpgItem is a wrapper for an ItemStack. It contains data on the item's rarity and supports placeholders.
 *
 * @author Sugaku
 */
public class RpgItem { // todo: Custom Model Data support., magic power
 // todo: Expand to include a spell details for on-use items. Cooldown, Damage, DPS, Duration, Range.
    /**
     * A generated ItemStack representation of this RpgItem.
     */
    private ItemStack item;

    /**
     * The rarity of this RpgItem.
     */
    private final Rarity rarity;

    /**
     * The material of this RpgItem.
     */
    private final Material material;

    /**
     * The name of this RpgItem.
     */
    private final String name;

    /**
     * The slot that this ItemStack should be equipped in.
     * -- GETTER --
     * Accessor method for the slot that this equipment belongs in.
     */
    @Getter
    private final EquipmentSlots slot;

    /**
     * The description of the ItemStack.
     */
    private final List<String> description;

    /**
     * The amount of health this ItemStack gives.
     * -- GETTER --
     * Accessor method for the amount of health added onto the player by equipping this item.
     */
    @Getter
    private final int health;

    /**
     * The amount of damage this ItemStack gives.
     * -- GETTER --
     * Accessor method for the damage increase awarded by this item.
     */
    @Getter
    private final int damage;

    /**
     * The amount of armor that this ItemStack gives.
     * -- GETTER --
     * Accessor method for the amount of armor increase awarded by this item.
     */
    @Getter
    private final double armor;

    /**
     * The amount of armor toughness that this ItemStack gives.
     * -- GETTER --
     * Accessor method for toughness stat on this item.
     */
    @Getter
    private final double toughness;

    /**
     * The amount of attack speed that this ItemStack has.
     * -- GETTER --
     * Accessor method for the attack speed of this item.
     */
    @Getter
    private final double attackSpeed;

    /**
     * The color of this leather armor. Stored in ARGB.
     */
    private final int[] color;

    /**
     * The armor type of this RpgItem.
     * -- GETTER --
     * Accessor method for the armor equipment type of this item.
     */
    @Getter
    private final ArmorTypes armorType;

    /**
     * The weapon type of this RpgItem.
     * -- GETTER --
     * Accessor method for the weapon typing.
     */
    @Getter
    private final WeaponType weaponType;

    /**
     * The url of the skin to use if this is a skull.
     */
    private final String skinUrl;

    /**
     * Creates a new RpgItem with the given values.
     *
     * @param material    The material of this RpgItem.
     * @param name        The display name of this RpgItem.
     * @param slot        The equipment slot this item belongs in.
     * @param description Any lore that should be present on this item.
     * @param rarity      The rarity of this item.
     * @param health      Any health that this item may give.
     * @param damage      Any damage stat included on this item.
     * @param armor       The armor value of this item.
     * @param toughness   Toughness on this item.
     * @param attackSpeed The attackSpeed stat of this RpgItem.
     * @param armorType   Any, if, armor type is present.
     * @param weaponType  Any, if, weapon type is present.
     * @param color       The ARGB of this item.
     */
    public RpgItem (Material material,
                    String name,
                    EquipmentSlots slot,
                    List<String> description,
                    Rarity rarity,
                    int health,
                    int damage,
                    double armor,
                    double toughness,
                    double attackSpeed,
                    ArmorTypes armorType,
                    WeaponType weaponType,
                    int[] color) {
        this.material = material;
        this.name = name;
        this.slot = slot;
        this.description = description;
        this.rarity = rarity;
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.toughness = toughness;
        this.attackSpeed = attackSpeed;
        this.armorType = armorType;
        this.weaponType = weaponType;
        this.color = color;
        this.skinUrl = null;
        item = generateItemStack();
    }

    /**
     * Creates a new RpgItem by loading the given configuration section.
     *
     * @param section The configuration section to load.
     */
    public RpgItem (ConfigurationSection section) { // todo: We should add Gear Score, Armor/Weapon Type, ItemID, etc. to a Persistent Data Value to make classes and custom effects easier.
        if (section == null)throw new NullPointerException("Configuration section is null!");
        if (section.contains("meta")) {
            Logger logger = RPGFramework.getPlugin().getLogger();
            logger.log(Level.WARNING, "Using legacy item support.");
            item = new ItemParser(section).build();
        }
        material = Material.valueOf(section.getString("material", "COBBLESTONE"));
        name = section.getString("name", "No name");
        slot = EquipmentSlots.valueOf(section.getString("slot", "HEAD"));
        description = section.getStringList("description");
        rarity = Rarity.valueOf(section.getString("rarity", "COMMON"));
        health = section.getInt("stats.health", 0);
        damage = section.getInt("stats.damage", 0);
        armor = section.getDouble("stats.armor", 0);
        toughness = section.getDouble("stats.toughness", 0);
        attackSpeed = section.getDouble("stats.attack-speed", 0);
        if (section.contains("armor-type")) armorType = ArmorTypes.valueOf(section.getString("armor-type", "HEAVY"));
        else armorType = null;
        if (section.contains("weapon-type")) weaponType = WeaponType.valueOf(section.getString("weapon-type", "SWORD"));
        else weaponType = null;
        String url = null;
        if (section.contains("skull")) {
            url = section.getString("skull.url", null);
            if (url == null) {
                String base64 = section.getString("skull.base64", null);
                if (base64 != null) {
                    String decoded = new String(Base64.getDecoder().decode(base64));
                    url = decoded.substring("{\"textures\":{\"SKIN\":{\"url\":\"".length(), decoded.length() - "\"}}}".length());
                }
            }
        }
        skinUrl = url;
        if (section.contains("color")) color = new int[]{ section.getInt("color.alpha", 255),
                section.getInt("color.red", 0),
                section.getInt("color.green", 0),
                section.getInt("color.blue", 0) };
        else color = null;
        if (item == null) item = generateItemStack();
    }

    /**
     * Generates an ItemStack with the assigned values in this item.
     */
    private ItemStack generateItemStack () {
        return generateItemStack(false);
    }

    /**
     * Generates an ItemStack with the assigned values in this item.
     *
     * @param update Whether placeholders should be incremented and added.
     */
    private ItemStack generateItemStack (boolean update) {
        ItemBuilder builder = new ItemBuilder(material);
        builder.setDisplayName(StringUtils.convertHexCodes(rarity.getHexColor() + name));
        List<String> clone = new ArrayList<>(description);
        for (int i = 0; i < clone.size(); i++) // todo: Placeholder support.
            clone.set(i, StringUtils.convertHexCodes("#CCCCCC" + description.get(i)));
        if (damage != 0 || attackSpeed != 0 || health != 0 || armor != 0 || toughness != 0) {
            clone.add(StringUtils.convertHexCodes("#606060") + " ----- ----- ----- ");
            if (damage != 0) clone.add(StringUtils.convertHexCodes("#D93747") + "Damage: " + damage);
            if (attackSpeed != 0) clone.add(StringUtils.convertHexCodes("#23A5DB") + "Attack Speed: " + attackSpeed);
            if (health != 0) clone.add(StringUtils.convertHexCodes("#F454DA") + "Health: " + health);
            if (armor != 0) clone.add(StringUtils.convertHexCodes("#3FB74A") + "Armor: " + armor);
            if (toughness != 0) clone.add(StringUtils.convertHexCodes("#F2D951") + "Toughness: " + toughness);
        }
        if (!slot.equals(EquipmentSlots.MATERIAL)) {
            clone.add(StringUtils.convertHexCodes("#606060") + " ----- ----- ----- ");
            String details = "#CCCCCC" + StringUtils.capitalize(slot.toString());
            if (armorType != null) details = details + "#606060 | " + armorType.getFormattedName();
            if (weaponType != null) details = details + "#606060 | " + weaponType.getFormattedName();
            details = details + "#606060 | #F2D951" + getGearScore();
            clone.add(StringUtils.convertHexCodes(details));
        }
        builder.setLore(clone.toArray(new String[0]));
        if (skinUrl != null) builder.setSkullSkinUrl(skinUrl);
        builder.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, health / 5.0, slot);
        builder.addAttributeModifier(Attribute.GENERIC_ARMOR, armor, slot);
        builder.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, toughness, slot);
        builder.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, attackSpeed, slot);
        builder.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, damage / 5.0, slot);
        if (color != null) builder.setColor(color);
        builder.setUnbreakable(true);
        builder.addItemFlag(ItemFlag.HIDE_ATTRIBUTES);
        builder.addItemFlag(ItemFlag.HIDE_UNBREAKABLE);
        return builder.build();
    }

    /**
     * A helper method that calculates the gear score of ths RpgItem.
     *
     * @return The gear score of this item.
     */
    public int getGearScore () {
        return (int) ((health * 1.0) + (damage * 5.0) + (armor * 2.0)) + (rarity.ordinal() * 10);
    }

    /**
     * An accessor method to the ItemStack represented in this RpgItem. This will not activate and increment
     * placeholders.
     *
     * @return Returns an ItemStack representation of this item.
     */
    public ItemStack getItemStack () {
        return item;
    }
}
