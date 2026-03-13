package io.github.math0898.rpgframework.classes;

import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enumerates the currently implemented and planned RPG classes.
 */
public enum Classes { // TODO: Plan GLADIATOR

    ASSASSIN(ChatColor.BLUE, "Assassin"),
    BARD(ChatColor.LIGHT_PURPLE, "Bard"),
    BERSERKER(ChatColor.RED, "Berserker"),
    PALADIN(ChatColor.YELLOW, "Paladin"),
    PYROMANCER(ChatColor.GOLD, "Pyromancer"),
    // MARKSMEN(ChatColor.DARK_GREEN, "Marksmen"), // Requires an implementing class.
    NONE(ChatColor.WHITE, "None");

    private static final Map<String, Classes> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(
                    classType -> classType.name(),
                    Function.identity()
            ));

    private final String displayName;
    private final String formattedName;

    Classes(ChatColor color, String displayName) {
        this.displayName = displayName;
        this.formattedName = color + displayName;
    }

    public String getFormattedName() {
        return formattedName;
    }

    public String getName() {
        return displayName;
    }

    public static Classes fromString(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        return BY_NAME.getOrDefault(normalize(value), NONE);
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase();
    }
}
