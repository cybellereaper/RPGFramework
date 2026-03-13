package io.github.math0898.rpgframework.items;

import io.github.math0898.rpgframework.RPGFramework;
import io.github.math0898.rpgframework.items.implementations.SylvathianThornWeaver;
import io.github.math0898.rpgframework.items.implementations.WrathOfFeyrith;
import org.bukkit.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.github.math0898.rpgframework.RPGFramework.*;

/**
 * The ItemManager is used to crate RPG related items on load.
 *
 * @author Sugaku
 */
public class ItemManager {

    /**
     * A list of RpgItems which have been registered.
     */
    private final Map<String, RpgItem> rpgItems = new HashMap<>();

    /**
     * The active ItemManager instance.
     */
    private static ItemManager instance = new ItemManager();

    /**
     * Creates a new ItemManager.
     */
    private ItemManager () {
        File itemsDir = new File("./plugins/RPGFramework/items/");
        if (!itemsDir.exists()) {
            if (!itemsDir.mkdirs()) {
                console("Failed to create item directories.", ChatColor.YELLOW);
                return;
            }
        }
        for (String itemResources : new String[]{ "items/krusk.yml", "items/other.yml", "items/eiryeras.yml", "items/feyrith.yml", "items/gods.yml", "items/vanilla.yml", "items/seignour.yml"})
            RPGFramework.getPlugin().saveResource(itemResources, true); // todo: refactor to reduce scope when adding multiple bosses and sets.
        File[] files = itemsDir.listFiles();
        if (files == null) console("Cannot find any item files.", ChatColor.YELLOW);
        else parseFiles(files);
        replaceRecipies();
        Bukkit.getScheduler().runTaskAsynchronously(RPGFramework.getPlugin(), this::passives);
        Bukkit.getPluginManager().registerEvents(new SylvathianThornWeaver(), RPGFramework.getPlugin());
        Bukkit.getPluginManager().registerEvents(new WrathOfFeyrith(), RPGFramework.getPlugin());
    }

    /**
     * A static accessor for the active ItemManager instance.
     *
     * @return The active ItemManager.
     */
    public static ItemManager getInstance () {
        if (instance == null) instance = new ItemManager();
        return instance;
    }

    /**
     * Awards this item to the given player. Supports placeholder text.
     *
     * @param player The player to award the item to.
     * @param name The name of the item to award.
     */
    @Deprecated
    public void awardItem (Player player, String name) {
        ItemStack item = rpgItems.get(name).getItemStack();
        if (item == null) return;
        // todo: Refactor to consider multiple placeholders.
        ItemMeta meta = item.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        assert meta != null;
        meta.setDisplayName(meta.getDisplayName().replace("%player%", player.getName()));
        List<String> tmp = new ArrayList<>();
        List<String> lore = meta.getLore();
        if (lore != null) {
            lore.forEach((l) -> tmp.add(l.replace("%player%", player.getName())));
            meta.setLore(tmp);
        }
        item.setItemMeta(meta);
        Map<Integer, ItemStack> failed = player.getInventory().addItem(item);
        failed.forEach((i, stack) -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    /**
     * A utility method to convert a given file name and item name into a camel space namespace string.
     *
     * @param key      The key in the result.
     * @param fileName The namespace in the beginning.
     * @return The resulting namespace key.
     */
    private String toCamelSpaceNamespace (String fileName, String key) {
        String toReturn = fileName.replace(".yml", "").replace(".yaml", "") + ":";
        char[] tmp = key.toCharArray();
        tmp[0] = Character.toUpperCase(tmp[0]);
        for (int i = 1; i < tmp.length; i++)
            if (tmp[i] == '-') {
                if (i < tmp.length - 1)
                    tmp[i + 1] = Character.toUpperCase(tmp[i + 1]);
                i++;
            }
        return toReturn + new String(tmp).replace("-", "");
    }

    /**
     * Accessor method for all the items in the ItemManager.
     *
     * @return The list of items registered with the ItemManager.
     */
    public List<String> getItemNames () {
        return new ArrayList<>(rpgItems.keySet());
    }

    /**
     * Accessor method for items by the given name.
     *
     * @param name The name of the item to get.
     * @return The ItemStack of the RpgItem associated with the given name.
     */
    public ItemStack getItem (String name) {
        return rpgItems.get(name).getItemStack();
    }

    /**
     * Attempts to find an RpgItem that matches the given ItemStack. This is a relatively expensive method that should
     * be run infrequently and with consideration.
     *
     * @param item The ItemStack that is the result from the RpgItem we're locating.
     * @return Null if no item found, otherwise the located item id.
     */
    public String findRpgItem (ItemStack item) {
        for (String k : rpgItems.keySet())
            if (rpgItems.get(k).getItemStack().isSimilar(item))
                return k;
        return null;
    }

    /**
     * Accessor method for RPGItems by the given name.
     *
     * @param name The name of the item to get.
     * @return The RpgItem associated with the given name.
     */
    public RpgItem getRpgItem (String name) {
        return rpgItems.get(name);
    }

    /**
     * Checks whether an item by the given name exists.
     *
     * @param name The name of the item to check for.
     * @return True if the item exists.
     */
    public boolean hasItem (String name) {
        return rpgItems.containsKey(name);
    }

    /**
     * Passive check. Periodically ran to check if players are using armor that gives them special effects.
     * todo: Refactor to be async and per player.
     */
    public void passives () {
        if (!RPGFramework.getPlugin().isEnabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            EntityEquipment equipment = p.getEquipment();
            if (equipment == null) continue;
            ItemStack helm = p.getEquipment().getHelmet();
            if (helm == null) continue;
            if (helm.getType().equals(Material.LEATHER_HELMET)) {
                if (helm.equals(getItem("other:HelmetOfDarkness"))) {
                    Bukkit.getScheduler().runTask(RPGFramework.getPlugin(), () -> {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 21 * 20, 255, true, false));
                        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 21 * 20, 255, true, false));
                    });
                }
            }
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(RPGFramework.getPlugin(), this::passives, 20 * 20);
    }

    /**
     * Parses all the files given and the items contained.
     *
     * @param files The files to parse.
     */
    public void parseFiles (File[] files) {
        assert files != null;
        for (File f : files) {
            try {
                YamlConfiguration yaml = new YamlConfiguration();
                yaml.load(f);
                for (String k : yaml.getKeys(false)) {
                    RpgItem i = null;
                    try {
                        i = new RpgItem(yaml.getConfigurationSection(k));
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                    String goodName = toCamelSpaceNamespace(f.getName(), k);
                    if (i != null) {
                        rpgItems.put(goodName, i);
                        console("Registered item by name: " + goodName, ChatColor.GRAY);
                    } else console("Failed to parse: " + goodName + " in: " + f.getPath(), ChatColor.RED);
                }
            } catch (InvalidConfigurationException | IOException e) {
                console(e.getMessage(), ChatColor.RED);
                console("Failed to parse item located at: " + f.getPath(), ChatColor.RED);
            }
        }
    }

    /**
     * Rates the given item on a scale of 0-120. Assumes it is the correct gear for a player.
     *
     * @param item The item to rate on a scale of 0-120.
     */
    @Deprecated
    public int rateItem (ItemStack item) {
        return 0;
    }

    /**
     * Replaces Minecraft armor recipies with RPG equivalents.
     */
    public void replaceRecipies () {
        List<Recipe> recipes = Bukkit.getServer().getRecipesFor(new ItemStack(Material.DIAMOND_HELMET));
        recipes.forEach((r) -> {
            if (r instanceof Keyed key)
                Bukkit.removeRecipe(key.getKey());
        });
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(RPGFramework.getPlugin(), "diamond-helmet"), getItem("vanilla:DiamondHelmet"));
        recipe.shape("AAA", "ABA", "BBB");
        recipe.setIngredient('A', Material.DIAMOND);
        Bukkit.addRecipe(recipe);
    }
}
