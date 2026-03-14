package io.github.math0898.rpgframework.classes.lua;

import io.github.math0898.rpgframework.RPGFramework;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class LuaClassDefinitionRegistry {

    private static final Map<String, String> RESOURCE_BY_CLASS_KEY = Map.of(
            "ASSASSIN", "classes/assassin.lua",
            "BARD", "classes/bard.lua",
            "BERSERKER", "classes/berserker.lua",
            "PALADIN", "classes/paladin.lua",
            "PYROMANCER", "classes/pyromancer.lua"
    );

    private final LuaClassScriptEngine scriptEngine;
    private final Map<String, LuaClassDefinition> definitionsByClassKey = new HashMap<>();

    public LuaClassDefinitionRegistry(LuaClassScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    public void loadAll(ClassLoader classLoader) {
        definitionsByClassKey.clear();

        for (Map.Entry<String, String> entry : RESOURCE_BY_CLASS_KEY.entrySet()) {
            loadDefinition(classLoader, entry.getKey(), entry.getValue());
        }
    }

    public LuaClassDefinition get(String classKey) {
        return definitionsByClassKey.get(classKey);
    }

    private void loadDefinition(ClassLoader classLoader, String classKey, String path) {
        try {
            String script = readResource(classLoader, path);
            definitionsByClassKey.put(classKey, scriptEngine.parse(script));
        } catch (RuntimeException exception) {
            RPGFramework.console("Failed to parse Lua class script at " + path + ": " + exception.getMessage(), ChatColor.RED);
        }
    }

    private String readResource(ClassLoader classLoader, String path) {
        try (InputStream stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Missing resource: " + path);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource: " + path, exception);
        }
    }
}
