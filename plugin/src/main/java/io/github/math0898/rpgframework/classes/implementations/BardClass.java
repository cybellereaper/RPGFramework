package io.github.math0898.rpgframework.classes.implementations;

import io.github.math0898.rpgframework.Cooldown;
import io.github.math0898.rpgframework.RpgPlayer;
import io.github.math0898.rpgframework.classes.AbstractClass;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class BardClass extends AbstractClass {

    private enum Ability {
        HYM,
        LIFE_OF_MUSIC
    }

    private enum BardBuff {
        REGENERATION(ChatColor.LIGHT_PURPLE + "Regeneration", PotionEffectType.REGENERATION),
        SWIFTNESS(ChatColor.AQUA + "Swiftness", PotionEffectType.SPEED),
        STRENGTH(ChatColor.RED + "Strength", PotionEffectType.STRENGTH);

        private final String displayName;
        private final PotionEffectType potionEffectType;

        BardBuff(String displayName, PotionEffectType potionEffectType) {
            this.displayName = displayName;
            this.potionEffectType = potionEffectType;
        }

        public String getDisplayName() {
            return displayName;
        }

        public PotionEffectType getPotionEffectType() {
            return potionEffectType;
        }

        public BardBuff next() {
            return switch (this) {
                case REGENERATION -> SWIFTNESS;
                case SWIFTNESS -> STRENGTH;
                case STRENGTH -> REGENERATION;
            };
        }
    }

    private static final Material CLASS_ITEM = Material.NOTE_BLOCK;

    private static final int HYM_COOLDOWN_SECONDS = 30;
    private static final int LIFE_OF_MUSIC_COOLDOWN_SECONDS = 300;

    private static final int HYM_DURATION_SECONDS = 45;
    private static final int HYM_AMPLIFIER = 1;

    private static final double LIFE_OF_MUSIC_HEAL_AMOUNT = 10.0;
    private static final int LIFE_OF_MUSIC_DURATION_SECONDS = 10;
    private static final int LIFE_OF_MUSIC_AMPLIFIER = 2;

    private static final float TOTEM_SOUND_VOLUME = 0.8f;
    private static final float TOTEM_SOUND_PITCH = 1.0f;

    private static final String BLOCK_PLACEMENT_WARNING = "Shift + right click if you are trying to place that block.";
    private static final String HYM_CAST_MESSAGE = ChatColor.GREEN + "You've used hym!";
    private static final String LIFE_OF_MUSIC_CAST_MESSAGE =
            ChatColor.GREEN + "You've used " + ChatColor.GOLD + "A Life of Music" + ChatColor.GREEN + "!";
    private static final String HYM_BROADCAST_SUFFIX = ChatColor.GREEN + " has used hym!";

    private BardBuff selectedBuff = BardBuff.REGENERATION;

    public BardClass(RpgPlayer player) {
        super(player);
        setCooldowns(createCooldowns());
        setClassItems(CLASS_ITEM);
    }

    @Override
    public void onRightClickCast(PlayerInteractEvent event, Material type) {
        if (shouldPreventBlockPlacement(event)) {
            return;
        }

        cycleSelectedBuff();
        send(selectedBuff.getDisplayName());
    }

    @Override
    public void onLeftClickCast(PlayerInteractEvent event, Material type) {
        if (!isAbilityReady(Ability.HYM)) {
            return;
        }

        RpgPlayer bard = getPlayer();
        List<RpgPlayer> allies = bard.friendlyCasterTargets();
        String casterDisplayName = bard.getPlayerRarity() + bard.getName();

        send(HYM_CAST_MESSAGE);

        for (RpgPlayer ally : allies) {
            applyHymBuff(ally);
            ally.sendMessage(casterDisplayName + HYM_BROADCAST_SUFFIX);
        }

        restartCooldown(Ability.HYM);
    }

    @Override
    public boolean onDeath() {
        if (!isAbilityReady(Ability.LIFE_OF_MUSIC)) {
            return true;
        }

        RpgPlayer bard = getPlayer();
        Player player = bard.getBukkitPlayer();

        send(LIFE_OF_MUSIC_CAST_MESSAGE);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, TOTEM_SOUND_VOLUME, TOTEM_SOUND_PITCH);

        bard.heal(LIFE_OF_MUSIC_HEAL_AMOUNT);
        applyLifeOfMusicBuffs(bard);
        restartCooldown(Ability.LIFE_OF_MUSIC);

        return false;
    }

    private boolean shouldPreventBlockPlacement(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        if (event.getPlayer().isSneaking()) {
            return false;
        }

        event.setCancelled(true);
        send(BLOCK_PLACEMENT_WARNING);
        return true;
    }

    private void cycleSelectedBuff() {
        selectedBuff = selectedBuff.next();
    }

    private void applyHymBuff(RpgPlayer target) {
        target.addPotionEffect(
                selectedBuff.getPotionEffectType(),
                toTicks(HYM_DURATION_SECONDS),
                HYM_AMPLIFIER
        );
    }

    private void applyLifeOfMusicBuffs(RpgPlayer target) {
        target.addPotionEffect(PotionEffectType.REGENERATION, toTicks(LIFE_OF_MUSIC_DURATION_SECONDS), LIFE_OF_MUSIC_AMPLIFIER);
        target.addPotionEffect(PotionEffectType.SPEED, toTicks(LIFE_OF_MUSIC_DURATION_SECONDS), LIFE_OF_MUSIC_AMPLIFIER);
        target.addPotionEffect(PotionEffectType.STRENGTH, toTicks(LIFE_OF_MUSIC_DURATION_SECONDS), LIFE_OF_MUSIC_AMPLIFIER);
    }

    private boolean isAbilityReady(Ability ability) {
        return offCooldown(ability.ordinal());
    }

    private void restartCooldown(Ability ability) {
        getCooldowns()[ability.ordinal()].restart();
    }

    private int toTicks(int seconds) {
        return seconds * 20;
    }

    private static Cooldown[] createCooldowns() {
        Cooldown[] cooldowns = new Cooldown[Ability.values().length];
        cooldowns[Ability.HYM.ordinal()] = new Cooldown(HYM_COOLDOWN_SECONDS);
        cooldowns[Ability.LIFE_OF_MUSIC.ordinal()] = new Cooldown(LIFE_OF_MUSIC_COOLDOWN_SECONDS);
        return cooldowns;
    }
}
