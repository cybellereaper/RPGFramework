package sugaku.rpg.mobs.teir1.feyrith;

import io.github.math0898.rpgframework.enemies.CustomMob;
import io.github.math0898.rpgframework.items.ItemManager;
import io.github.math0898.utils.Utils;
import io.github.math0898.utils.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import sugaku.rpg.framework.items.BossDrop;
import sugaku.rpg.framework.items.Rarity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.bukkit.Material.BONE;
import static org.bukkit.Material.COAL;

public class FeyrithBoss extends CustomMob implements Listener {

    private static final String NAME = "Feyrith, Apprentice Mage";
    private static final int MAX_HEALTH = 350;
    private static final double BASE_DAMAGE = 50.0;
    private static final double LIGHTNING_DAMAGE = 1.25 * BASE_DAMAGE / 5.0;
    private static final double WAVE_DAMAGE = BASE_DAMAGE / 5.0;
    private static final double CONE_DAMAGE = 1.35 * BASE_DAMAGE / 5.0;
    private static final double PLAYER_SEARCH_RADIUS_XZ = 12.0;
    private static final double PLAYER_SEARCH_RADIUS_Y = 6.0;
    private static final int AI_INTERVAL_TICKS = 4 * 20;
    private static final int LIGHTNING_DELAY_TICKS = 2 * 20;
    private static final int WAVE_STEP_TICKS = 5;
    private static final int WAVE_DURATION_TICKS = 4 * 20;
    private static final int WAVE_DAMAGE_INTERVAL_TICKS = 20;
    private static final int WAVE_SOUND_INTERVAL_TICKS = 10;
    private static final double LIGHTNING_RADIUS = 1.75;
    private static final int LIGHTNING_MARKER_POINTS = 18;
    private static final int LIGHTNING_BEAM_HEIGHT = 6;
    private static final double WAVE_RADIUS = 4.5;
    private static final int WAVE_RING_POINTS = 28;
    private static final int WAVE_FILL_POINTS = 16;
    private static final double CONE_RANGE = 9.0;
    private static final double CONE_HALF_ANGLE_DEGREES = 28.0;
    private static final int CONE_PULSES = 3;
    private static final int CONE_PULSE_DELAY_TICKS = 6;
    private static final double CONE_STEP_DISTANCE = 1.2;
    private static final int CONE_STEPS = 8;
    private static final int CONE_ARC_POINTS = 7;

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
            case FIREBALL -> coneAttack(entity, players, plan.anchor());
        }
    }

    private void lightningAttack(LivingEntity entity, Collection<Player> players) {
        dyeArmor(25, 64, 255, 0.75, 1.0, 0.5);

        for (Player player : players) {
            Location target = player.getLocation().clone();
            telegraphLightning(target);
            Bukkit.getScheduler().runTaskLater(plugin(), () -> resolveLightning(entity, target), LIGHTNING_DELAY_TICKS);
        }
    }

    private void telegraphLightning(Location target) {
        spawnVerticalBeam(target, Particle.FALLING_WATER, LIGHTNING_BEAM_HEIGHT, 32);
        spawnCircle(
                target,
                LIGHTNING_RADIUS,
                LIGHTNING_MARKER_POINTS,
                Particle.DUST,
                new Particle.DustOptions(Color.fromRGB(80, 170, 255), 1.25f)
        );
    }

    private void resolveLightning(LivingEntity caster, Location strikeLocation) {
        if (!isActive(caster)) {
            return;
        }
        World world = strikeLocation.getWorld();
        if (world == null) {
            return;
        }

        spawnVerticalBeam(strikeLocation, Particle.ELECTRIC_SPARK, LIGHTNING_BEAM_HEIGHT, 24);
        spawnCircle(
                strikeLocation,
                LIGHTNING_RADIUS,
                LIGHTNING_MARKER_POINTS,
                Particle.DUST,
                new Particle.DustOptions(Color.fromRGB(160, 220, 255), 1.4f)
        );
        world.strikeLightningEffect(strikeLocation);
        world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.5f, 1.1f);
        for (Player player : findNearbyPlayers(caster)) {
            if (Objects.equals(player.getWorld(), world)
                    && FeyrithBrain.isWithinCircle(toPoint(strikeLocation), toPoint(player.getLocation()), LIGHTNING_RADIUS)) {
                player.damage(LIGHTNING_DAMAGE, caster);
            }
        }
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
                    entity.getNearbyEntities(WAVE_RADIUS, 4.0, WAVE_RADIUS).stream()
                            .filter(Player.class::isInstance)
                            .map(Player.class::cast)
                            .filter(player -> FeyrithBrain.isWithinCircle(toPoint(location), toPoint(player.getLocation()), WAVE_RADIUS))
                            .forEach(player -> player.damage(WAVE_DAMAGE, entity));
                }

                elapsedTicks += WAVE_STEP_TICKS;
                if (elapsedTicks >= WAVE_DURATION_TICKS) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin(), 0L, WAVE_STEP_TICKS);
    }

    private void coneAttack(LivingEntity entity, Collection<Player> players, FeyrithBrain.Point anchor) {
        dyeArmor(255, 64, 25, 1.0, 0.75, 0.5);

        if (players.isEmpty()) {
            return;
        }
        int pulses = phase >= 3 ? CONE_PULSES + 1 : CONE_PULSES;
        for (int pulse = 0; pulse < pulses; pulse++) {
            Bukkit.getScheduler().runTaskLater(plugin(), () -> resolveConePulse(entity, players, anchor), (long) pulse * CONE_PULSE_DELAY_TICKS);
        }
    }

    private void resolveConePulse(LivingEntity caster, Collection<Player> players, FeyrithBrain.Point anchor) {
        if (!isActive(caster)) {
            return;
        }

        Location origin = caster.getEyeLocation();
        spawnConeParticles(origin, anchor);
        World world = origin.getWorld();
        if (world != null) {
            world.playSound(origin, Sound.ITEM_FIRECHARGE_USE, 1.2f, 0.85f);
        }

        FeyrithBrain.Point originPoint = toPoint(origin);
        for (Player player : players) {
            if (player.isDead() || !player.isValid() || !Objects.equals(player.getWorld(), caster.getWorld())) {
                continue;
            }
            FeyrithBrain.Point target = toPoint(player.getLocation());
            if (FeyrithBrain.isWithinCone(originPoint, anchor, target, CONE_RANGE, CONE_HALF_ANGLE_DEGREES)) {
                player.setFireTicks(Math.max(player.getFireTicks(), 60));
                player.damage(CONE_DAMAGE, caster);
            }
        }
    }

    private void teleport(LivingEntity entity, FeyrithBrain.Plan plan) {
        Location originalLocation = entity.getLocation().clone();
        World world = originalLocation.getWorld();
        if (world == null) {
            return;
        }

        Location targetLocation = resolveTeleportLocation(world, plan, originalLocation.getY());
        entity.teleport(targetLocation);
        spawnVerticalBeam(originalLocation, Particle.PORTAL, 4, 20);
        spawnVerticalBeam(targetLocation, Particle.PORTAL, 4, 20);
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

    private void spawnVerticalBeam(Location location, Particle particle, int height, int count) {
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
        spawnCircle(location, WAVE_RADIUS, WAVE_RING_POINTS, Particle.HAPPY_VILLAGER, null);
        for (int i = 0; i < WAVE_FILL_POINTS; i++) {
            double angle = random.nextDouble(0.0, Math.PI * 2.0);
            double radius = Math.sqrt(random.nextDouble()) * WAVE_RADIUS;
            world.spawnParticle(
                    Particle.HAPPY_VILLAGER,
                    location.getX() + Math.cos(angle) * radius,
                    location.getY() + random.nextDouble(-0.25, 0.75),
                    location.getZ() + Math.sin(angle) * radius,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    private void spawnCircle(Location center, double radius, int samples, Particle particle, Particle.DustOptions dustOptions) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        for (FeyrithBrain.Point point : FeyrithBrain.circlePoints(toPoint(center), radius, samples)) {
            if (dustOptions == null) {
                world.spawnParticle(particle, point.x(), center.getY() + 0.1, point.z(), 1, 0.0, 0.0, 0.0, 0.0);
            } else {
                world.spawnParticle(particle, point.x(), center.getY() + 0.1, point.z(), 1, 0.0, 0.0, 0.0, 0.0, dustOptions);
            }
        }
    }

    private void spawnConeParticles(Location origin, FeyrithBrain.Point anchor) {
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        Vector direction = new Vector(anchor.x() - origin.getX(), 0.0, anchor.z() - origin.getZ());
        if (direction.lengthSquared() == 0.0) {
            return;
        }
        direction.normalize();
        Vector perpendicular = new Vector(-direction.getZ(), 0.0, direction.getX());

        for (int step = 1; step <= CONE_STEPS; step++) {
            double distance = step * CONE_STEP_DISTANCE;
            double halfWidth = Math.tan(Math.toRadians(CONE_HALF_ANGLE_DEGREES)) * distance;
            Location stepCenter = origin.clone().add(direction.clone().multiply(distance));
            for (int point = -CONE_ARC_POINTS; point <= CONE_ARC_POINTS; point++) {
                double interpolation = point / (double) CONE_ARC_POINTS;
                Location sample = stepCenter.clone().add(perpendicular.clone().multiply(halfWidth * interpolation));
                world.spawnParticle(Particle.FLAME, sample, 1, 0.05, 0.05, 0.05, 0.0);
                world.spawnParticle(Particle.SMALL_FLAME, sample, 1, 0.05, 0.05, 0.05, 0.0);
            }
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
