package sugaku.rpg.mobs.teir1.feyrith;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.random.RandomGenerator;

final class FeyrithBrain {

    static final double PHASE_TWO_HEALTH_RATIO = 0.70;
    static final double PHASE_THREE_HEALTH_RATIO = 0.40;
    private static final int MIN_TELEPORT_OFFSET = 3;
    private static final int MAX_TELEPORT_OFFSET = 8;

    enum Attack {
        LIGHTNING,
        WAVE,
        FIREBALL
    }

    record Point(double x, double y, double z) {
        double distanceSquared(Point other) {
            double deltaX = x - other.x;
            double deltaY = y - other.y;
            double deltaZ = z - other.z;
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        }
    }

    record Snapshot(double health, double maxHealth, Point bossPosition, List<Point> players) {
        Snapshot {
            bossPosition = Objects.requireNonNull(bossPosition, "bossPosition");
            players = List.copyOf(players);
        }
    }

    record Plan(int phase, Attack attack, Point anchor, int offsetX, int offsetZ) {
        Plan {
            anchor = Objects.requireNonNull(anchor, "anchor");
        }
    }

    private FeyrithBrain() {
    }

    static Plan planTurn(Snapshot snapshot, RandomGenerator random) {
        return planTurn(
                snapshot,
                random.nextDouble(),
                random.nextDouble(),
                random.nextBoolean(),
                random.nextDouble(),
                random.nextBoolean()
        );
    }

    static Plan planTurn(Snapshot snapshot, double attackRoll, double xRoll, boolean positiveX, double zRoll, boolean positiveZ) {
        int phase = determinePhase(snapshot.health(), snapshot.maxHealth());
        Point anchor = chooseAnchor(snapshot.bossPosition(), snapshot.players()).orElse(snapshot.bossPosition());
        return new Plan(
                phase,
                chooseAttack(phase, attackRoll),
                anchor,
                computeOffset(xRoll, positiveX),
                computeOffset(zRoll, positiveZ)
        );
    }

    static int determinePhase(double health, double maxHealth) {
        if (maxHealth <= 0.0) {
            return 1;
        }

        double ratio = health / maxHealth;
        if (ratio <= PHASE_THREE_HEALTH_RATIO) {
            return 3;
        }
        if (ratio <= PHASE_TWO_HEALTH_RATIO) {
            return 2;
        }
        return 1;
    }

    static Optional<Point> chooseAnchor(Point bossPosition, List<Point> players) {
        return players.stream().min(Comparator.comparingDouble(bossPosition::distanceSquared));
    }

    static Attack chooseAttack(int phase, double roll) {
        double normalizedRoll = clampRoll(roll);
        double lightningThreshold = switch (phase) {
            case 2 -> 0.30;
            case 3 -> 0.25;
            default -> 0.40;
        };
        double waveThreshold = switch (phase) {
            case 2 -> 0.65;
            case 3 -> 0.45;
            default -> 0.75;
        };

        if (normalizedRoll < lightningThreshold) {
            return Attack.LIGHTNING;
        }
        if (normalizedRoll < waveThreshold) {
            return Attack.WAVE;
        }
        return Attack.FIREBALL;
    }

    static int computeOffset(double roll, boolean positive) {
        double normalizedRoll = clampRoll(roll);
        int range = MAX_TELEPORT_OFFSET - MIN_TELEPORT_OFFSET + 1;
        int magnitude = MIN_TELEPORT_OFFSET + Math.min((int) Math.floor(normalizedRoll * range), range - 1);
        return positive ? magnitude : -magnitude;
    }

    private static double clampRoll(double roll) {
        if (Double.isNaN(roll)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(Math.nextDown(1.0), roll));
    }
}
