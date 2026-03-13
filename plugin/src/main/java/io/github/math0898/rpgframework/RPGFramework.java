package io.github.math0898.rpgframework;

import io.github.math0898.rpgframework.commands.ArtifactCommand;
import io.github.math0898.rpgframework.commands.Classes;
import io.github.math0898.rpgframework.commands.DebugCommand;
import io.github.math0898.rpgframework.commands.EditorCommand;
import io.github.math0898.rpgframework.commands.GiveCommand;
import io.github.math0898.rpgframework.commands.PartyCommand;
import io.github.math0898.rpgframework.commands.SummonRPG;
import io.github.math0898.rpgframework.commands.Tutorial;
import io.github.math0898.rpgframework.commands.Updates;
import io.github.math0898.rpgframework.damage.AdvancedDamageHandler;
import io.github.math0898.rpgframework.enemies.MobManager;
import io.github.math0898.rpgframework.hooks.HookManager;
import io.github.math0898.rpgframework.items.ItemManager;
import io.github.math0898.rpgframework.parties.PartyManager;
import io.github.math0898.rpgframework.systems.GodEventListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import sugaku.rpg.framework.RPGEventListener;
import sugaku.rpg.mobs.teir1.eiryeras.EiryerasBoss;
import sugaku.rpg.mobs.teir1.feyrith.FeyrithBoss;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class RPGFramework extends JavaPlugin {

    private static final String HOLOGRAM_WARNING_MESSAGE =
            "Neither HolographicDisplays nor DecentHolograms was found.";
    private static final String HOLOGRAM_WARNING_DETAILS =
            "This is a non-fatal issue. Damage numbers will not be shown when hitting mobs.";

    private static RPGFramework instance;

    public static boolean useHolographicDisplays = false;
    public static boolean useDecentHolograms = false;

    @Deprecated
    public static ItemManager itemManager;

    public static void console(String message, ChatColor color) {
        console(message, color, resolveLogLevel(color));
    }

    public static void console(String message, ChatColor color, Level level) {
        RPGFramework plugin = getRequiredInstance();
        plugin.getLogger().log(level, color + message);
    }

    public static RPGFramework getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        long startupStartTimeMillis = System.currentTimeMillis();
        instance = this;

        initializeCoreSystems();
        registerListeners();
        registerCommands();
        initializeManagers();
        detectOptionalPlugins();

        console(
                "Plugin enabled! " + ChatColor.DARK_GRAY + "Took: "
                        + (System.currentTimeMillis() - startupStartTimeMillis) + "ms",
                ChatColor.GREEN
        );
    }

    private void initializeCoreSystems() {
        PartyManager.init();
        PlayerManager.init();
        DataManager.getInstance();
        HookManager.getInstance();
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        for (Listener listener : createListeners()) {
            pluginManager.registerEvents(listener, this);
        }
    }

    private List<Listener> createListeners() {
        return List.of(
                new AdvancedDamageHandler(),
                new GodEventListener(),
                new RPGEventListener(),
                new EiryerasBoss(),
                new FeyrithBoss()
        );
    }

    private void initializeManagers() {
        itemManager = ItemManager.getInstance();
        MobManager.getInstance();
    }

    private void detectOptionalPlugins() {
        PluginManager pluginManager = Bukkit.getPluginManager();

        useHolographicDisplays = pluginManager.isPluginEnabled("HolographicDisplays");
        useDecentHolograms = pluginManager.isPluginEnabled("DecentHolograms");

        if (!useHolographicDisplays && !useDecentHolograms) {
            console(HOLOGRAM_WARNING_MESSAGE, ChatColor.YELLOW);
            console(HOLOGRAM_WARNING_DETAILS, ChatColor.YELLOW);
        }
    }

    private void registerCommands() {
        console("Registering commands.", ChatColor.GRAY);

        for (Supplier<Object> commandFactory : commandFactories()) {
            commandFactory.get();
        }

        console("Commands registered.", ChatColor.GREEN);
    }

    private List<Supplier<Object>> commandFactories() {
        return List.of(
                Tutorial::new,
                Updates::new,
                Classes::new,
                SummonRPG::new,
                GiveCommand::new,
                PartyCommand::new,
                DebugCommand::new,
                EditorCommand::new,
                ArtifactCommand::new
        );
    }

    private static Level resolveLogLevel(ChatColor color) {
        if (color == ChatColor.RED) {
            return Level.SEVERE;
        }
        if (color == ChatColor.YELLOW) {
            return Level.WARNING;
        }
        return Level.INFO;
    }

    private static RPGFramework getRequiredInstance() {
        return Objects.requireNonNull(instance, "RPGFramework has not been initialized yet.");
    }
}
