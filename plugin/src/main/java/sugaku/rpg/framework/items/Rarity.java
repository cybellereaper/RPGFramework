package sugaku.rpg.framework.items;

import org.bukkit.ChatColor;

/**
 * An enum of rarities available for items and bosses.
 */
@Deprecated(forRemoval = true)
public enum Rarity {

    /**
     * The various rarities available.
     */
    COMMON, UNIQUE, RARE, LEGENDARY, MYTHIC, RELIC;
    
    /**
     * Converts the rarity into a ChatColor.
     *
     * @param r The rarity to convert.
     * @return the ChatColor of the given rarity.
     */
    public static ChatColor toColor(Rarity r) {
        switch (r) {
            case COMMON: return ChatColor.WHITE;
            case UNIQUE: return ChatColor.GREEN;
            case RARE: return ChatColor.BLUE;
            case LEGENDARY: return ChatColor.GOLD;
            case MYTHIC: return ChatColor.LIGHT_PURPLE;
            case RELIC: return ChatColor.RED;
        }
        return ChatColor.BLACK;
    }
}
