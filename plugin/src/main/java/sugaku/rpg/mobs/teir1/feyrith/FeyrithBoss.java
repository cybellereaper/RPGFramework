package sugaku.rpg.mobs.teir1.feyrith;

import io.github.math0898.rpgframework.enemies.CustomMob;
import io.github.math0898.rpgframework.items.ItemManager;
import io.github.math0898.utils.Utils;
import io.github.math0898.utils.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sugaku.rpg.framework.items.BossDrop;
import sugaku.rpg.framework.items.Rarity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Material.BONE;
import static org.bukkit.Material.COAL;

public class FeyrithBoss extends CustomMob implements Listener {

    private static final String NAME = "Feyrith, Apprentice Mage";
    private static final String PROJECTILE_METADATA_KEY = "feyrith";
    private static final int MAX_HEALTH = 350;
    private static final double BASE_DAMAGE = 50.0;
    private static final double LIGHTNING_DAMAGE = 1.25 * BASE_DAMAGE / 5.0;
    private static final double WAVE_DAMAGE = BASE_DAMAGE / 5.0;
    private static final double FIREBALL_DAMAGE = BASE_DAMAGE / 5.0;
    private static final double PLAYER_SEARCH_RADIUS_XZ = 12.0;
    private static final double PLAYER_SEARCH_RADIUS_Y = 6.0;
    private static final int AI_INTERVAL_TICKS = 4 * 20;
    private static final int LIGHTNING_DELAY_TICKS = 2 * 20;
    private static final int FIREBALL_VOLLEY_DELAY_TICKS = 6;
    private static final int WAVE_STEP_TICKS = 5;
    private static final int WAVE_DURATION_TICKS = 4 * 20;
    private static final int WAVE_DAMAGE_INTERVAL_TICKS = 20;
    private static final int WAVE_SOUND_INTERVAL_TICKS = 10;
    private static final double FIREBALL_SPEED = 0.65;

    private static final BossDrop[] bossDrops = new BossDrop[]{
            new BossDrop(ItemManager.getInstance().getItem("feyrith:SylvathianThornWeaver"), Rarity.RARE),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:FireGemstone"), Rarity.UNIQUE),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:WrathOfFeyrith"), Rarity.RARE),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:MageKaftan"), Rarity.RARE),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:RoyalClogs"), Rarity.MYTHIC),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:VisionaryCoif"), Rarity.UNIQUE),
            new BossDrop(ItemManager.getInstance().getItem("feyrith:ComfortableBreeches"), Rarity.RARE)
    };

    private final AtomicBoolean planInFlight = new AtomicBoolean(false);
    private BukkitTask aiTask;
    private volatile int phase = 1;

    public FeyrithBoss() {
        super(NAME, EntityType.WITHER_SKELETON, Rarity.RARE, MAX_HEALTH);

        setArmor(new ItemStack(Material.AIR),
                buildArmorPiece(Material.LEATHER_CHESTPLATE, 25, 64, 255, 0.75),
                buildArmorPiece(Material.LEATHER_LEGGINGS, 25, 64, 255, 1.0),
                buildArmorPiece(Material.LEATHER_BOOTS, 25, 64, 255, 0.5));
    }

    public FeyrithBoss(Location location) {
        this();
        spawn(location);
    }

    @Override
    public void spawn(Location location) {
        super.spawn(location);

        LivingEntity entity = getEntity();
        if (entity == null) {
            return;
        }

        entity.setGravity(false);
        entity.setVelocity(new Vector(0, 0, 0));

        AttributeInstance knockbackResistance = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) {
            knockbackResistance.setBaseValue(1.0);
        }

        startAiLoop();
    }

    private void startAiLoop() {
        cancelAiLoop();
        tickAi();
        aiTask = Bukkit.getScheduler().runTaskTimer(plugin(), this::tickAi, AI_INTERVAL_TICKS, AI_INTERVAL_TICKS);
    }

    private void cancelAiLoop() {
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        planInFlight.set(false);
    }

    private void tickAi() {
        LivingEntity entity = getEntity();
        if (!isActive(entity)) {
            cancelAiLoop();
            return;
        }

        if (!planInFlight.compareAndSet(false, true)) {
            return;
        }

        FeyrithBrain.Snapshot snapshot = captureSnapshot(entity);
        Bukkit.getScheduler().runTaskAsynchronously(plugin(), () -> {
            FeyrithBrain.Plan plan = FeyrithBrain.planTurn(snapshot, ThreadLocalRandom.current());
            Bukkit.getScheduler().runTask(plugin(), () -> {
                try {
                    applyPlan(plan);
                } finally {
                    planInFlight.set(false);
                }
            });
        });
    }

    private FeyrithBrain.Snapshot captureSnapshot(LivingEntity entity) {
        List<FeyrithBrain.Point> players = findNearbyPlayers(entity).stream()
                .map(player -> toPoint(player.getLocation()))
                .toList();
        return new FeyrithBrain.Snapshot(entity.getHealth(), MAX_HEALTH, toPoint(entity.getLocation()), players);
    }

    private void applyPlan(FeyrithBrain.Plan plan) {
        LivingEntity entity = getEntity();
        if (!isActive(entity)) {
            cancelAiLoop();
            return;
        }

        phase = plan.phase();
        teleport(entity, plan);

        List<Player> players = findNearbyPlayers(entity);
        switch (plan.attack()) {
            case LIGHTNING -> lightningAttack(entity, players);
            case WAVE -> waveAttack(entity);
            case FIREBALL -> fireballAttack(entity, players);
        }
    }

    private void lightningAttack(LivingEntity entity, Collection<Player> players) {
        dyeArmor(25, 64, 255, 0.75, 1.0, 0.5);

        for (Player player : players) {
            particlesVert(Particle.FALLING_WATER, player.getLocation(), 30, 5);
            UUID playerId = player.getUniqueId();
            Bukkit.getScheduler().runTaskLater(plugin(), () -> strikeTrackedPlayer(entity, playerId), LIGHTNING_DELAY_TICKS);
        }
    }

    private void strikeTrackedPlayer(LivingEntity caster, UUID playerId) {
        if (!isActive(caster)) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.isDead() || !player.isValid()) {
            return;
        }

        World world = player.getWorld();
        Location strikeLocation = player.getLocation();
        world.strikeLightningEffect(strikeLocation);
        player.damage(LIGHTNING_DAMAGE, caster);
    }

    private void waveAttack(LivingEntity entity) {
        dyeArmor(25, 255, 64, 0.75, 1.0, 0.5);

        new BukkitRunnable() {
            private int elapsedTicks;

            @Override
            public void run() {
                if (!isActive(entity)) {
                    cancel();
                    return;
                }

                Location location = entity.getLocation();
                if (elapsedTicks % WAVE_SOUND_INTERVAL_TICKS == 0) {
                    playWaveSounds(location);
                }

                spawnWaveParticles(location);

                if (elapsedTicks % WAVE_DAMAGE_INTERVAL_TICKS == 0) {
                    entity.getNearbyEntities(4.5, 4.0, 4.5).stream()
                            .filter(Player.class::isInstance)
                            .map(Player.class::cast)
                            .forEach(player -> player.damage(WAVE_DAMAGE, entity));
                }

                elapsedTicks += WAVE_STEP_TICKS;
                if (elapsedTicks >= WAVE_DURATION_TICKS) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin(), 0L, WAVE_STEP_TICKS);
    }

    private void fireballAttack(LivingEntity entity, Collection<Player> players) {
        dyeArmor(255, 64, 25, 1.0, 0.75, 0.5);

        int volleys = phase >= 3 ? 2 : 1;
        for (Player player : players) {
            UUID playerId = player.getUniqueId();
            for (int volley = 0; volley < volleys; volley++) {
                long delay = (long) volley * FIREBALL_VOLLEY_DELAY_TICKS;
                Bukkit.getScheduler().runTaskLater(plugin(), () -> launchFireball(entity, playerId), delay);
            }
        }
    }

    private void launchFireball(LivingEntity caster, UUID playerId) {
        if (!isActive(caster)) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || player.isDead() || !player.isValid() || !Objects.equals(player.getWorld(), caster.getWorld())) {
            return;
        }

        Vector direction = player.getEyeLocation().toVector().subtract(caster.getEyeLocation().toVector());
        if (direction.lengthSquared() == 0.0) {
            return;
        }

        Fireball fireball = caster.launchProjectile(Fireball.class, direction.normalize().multiply(FIREBALL_SPEED));
        fireball.setGravity(false);
        fireball.setYield(0.0f);
        fireball.setIsIncendiary(false);
        fireball.setMetadata(PROJECTILE_METADATA_KEY, new FixedMetadataValue(plugin(), true));
    }

    private void teleport(LivingEntity entity, FeyrithBrain.Plan plan) {
        Location originalLocation = entity.getLocation().clone();
        World world = originalLocation.getWorld();
        if (world == null) {
            return;
        }

        Location targetLocation = resolveTeleportLocation(world, plan, originalLocation.getY());
        entity.teleport(targetLocation);
        particlesVert(Particle.PORTAL, originalLocation, 20, 4);
        particlesVert(Particle.PORTAL, targetLocation, 20, 4);
    }

    private Location resolveTeleportLocation(World world, FeyrithBrain.Plan plan, double fallbackY) {
        int x = (int) Math.floor(plan.anchor().x()) + plan.offsetX();
        int z = (int) Math.floor(plan.anchor().z()) + plan.offsetZ();
        int anchorY = (int) Math.floor(plan.anchor().y());
        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight() - 3;

        for (int offset = -3; offset < 10; offset++) {
            int y = anchorY + offset;
            if (y < minHeight || y > maxHeight) {
                continue;
            }

            if (world.getBlockAt(x, y, z).isPassable()
                    && world.getBlockAt(x, y + 1, z).isPassable()
                    && world.getBlockAt(x, y + 2, z).isPassable()) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }

        double safeY = Math.max(minHeight, Math.min(maxHeight, Math.floor(fallbackY)));
        return new Location(world, x + 0.5, safeY, z + 0.5);
    }

    private List<Player> findNearbyPlayers(LivingEntity entity) {
        return entity.getNearbyEntities(PLAYER_SEARCH_RADIUS_XZ, PLAYER_SEARCH_RADIUS_Y, PLAYER_SEARCH_RADIUS_XZ).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(player -> !player.isDead())
                .toList();
    }

    private void particlesVert(Particle particle, Location location, int count, int height) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        for (int i = 0; i < count; i++) {
            double yOffset = i * (height / (double) count);
            world.spawnParticle(particle, location.getX(), location.getY() + yOffset, location.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private void spawnWaveParticles(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    location.getX() + random.nextDouble(-4.0, 4.0),
                    location.getY() + random.nextDouble(-1.0, 1.0),
                    location.getZ() + random.nextDouble(-4.0, 4.0),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private void playWaveSounds(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        world.playSound(location, Sound.BLOCK_AZALEA_LEAVES_HIT, 2.0f, 1.0f);
        world.playSound(location, Sound.BLOCK_CHERRY_LEAVES_HIT, 2.0f, 1.0f);
        world.playSound(location, Sound.BLOCK_AZALEA_LEAVES_BREAK, 2.0f, 1.0f);
        world.playSound(location, Sound.BLOCK_GRASS_BREAK, 2.0f, 1.0f);
        world.playSound(location, Sound.BLOCK_GRASS_FALL, 2.0f, 1.0f);
    }

    private void dyeArmor(int red, int green, int blue, double chestMultiplier, double legsMultiplier, double bootsMultiplier) {
        LivingEntity entity = getEntity();
        if (entity == null) {
            return;
        }

        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return;
        }

        equipment.setChestplate(buildArmorPiece(Material.LEATHER_CHESTPLATE, red, green, blue, chestMultiplier));
        equipment.setLeggings(buildArmorPiece(Material.LEATHER_LEGGINGS, red, green, blue, legsMultiplier));
        equipment.setBoots(buildArmorPiece(Material.LEATHER_BOOTS, red, green, blue, bootsMultiplier));
    }

    private static ItemStack buildArmorPiece(Material material, int red, int green, int blue, double multiplier) {
        return new ItemBuilder(material).setColor(new int[]{
                255,
                scaledColor(red, multiplier),
                scaledColor(green, multiplier),
                scaledColor(blue, multiplier)
        }).build();
    }

    private static int scaledColor(int channel, double multiplier) {
        return Math.max(0, Math.min(255, (int) Math.round(channel * multiplier)));
    }

    private static FeyrithBrain.Point toPoint(Location location) {
        return new FeyrithBrain.Point(location.getX(), location.getY(), location.getZ());
    }

    private static boolean isActive(LivingEntity entity) {
        return entity != null && entity.isValid() && !entity.isDead();
    }

    private static JavaPlugin plugin() {
        return Utils.getPlugin();
    }

    public static String getName() {
        return NAME;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void EntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!(event.getDamager() instanceof Fireball fireball) || !fireball.hasMetadata(PROJECTILE_METADATA_KEY)) {
            return;
        }

        player.setFireTicks(40);
        event.setDamage(FIREBALL_DAMAGE);
        fireball.remove();
    }

    @EventHandler
    public static void onDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity.getCustomName() == null || !entity.getCustomName().contains(NAME)) {
            return;
        }

        CustomMob.handleDrops(event, bossDrops, Rarity.RARE);

        ThreadLocalRandom random = ThreadLocalRandom.current();
        World world = entity.getWorld();
        world.dropItem(entity.getLocation(), new ItemStack(BONE, random.nextInt(1, 6)));
        world.dropItem(entity.getLocation(), new ItemStack(COAL, random.nextInt(1, 6)));
    }
}
