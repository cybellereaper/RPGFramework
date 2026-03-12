package sugaku.rpg.mobs.teir1.feyrith;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeyrithBrainTest {

    @Test
    void determinePhaseUsesExpectedHealthBands() {
        assertEquals(1, FeyrithBrain.determinePhase(350.0, 350.0));
        assertEquals(2, FeyrithBrain.determinePhase(245.0, 350.0));
        assertEquals(3, FeyrithBrain.determinePhase(140.0, 350.0));
    }

    @Test
    void chooseAttackUsesPhaseSpecificThresholds() {
        assertEquals(FeyrithBrain.Attack.LIGHTNING, FeyrithBrain.chooseAttack(1, 0.10));
        assertEquals(FeyrithBrain.Attack.WAVE, FeyrithBrain.chooseAttack(1, 0.50));
        assertEquals(FeyrithBrain.Attack.FIREBALL, FeyrithBrain.chooseAttack(1, 0.90));

        assertEquals(FeyrithBrain.Attack.WAVE, FeyrithBrain.chooseAttack(2, 0.40));
        assertEquals(FeyrithBrain.Attack.FIREBALL, FeyrithBrain.chooseAttack(3, 0.90));
    }

    @Test
    void chooseAnchorPrefersNearestPlayer() {
        FeyrithBrain.Point boss = new FeyrithBrain.Point(0.0, 64.0, 0.0);
        List<FeyrithBrain.Point> players = List.of(
                new FeyrithBrain.Point(8.0, 64.0, 8.0),
                new FeyrithBrain.Point(2.0, 64.0, 2.0),
                new FeyrithBrain.Point(5.0, 64.0, 0.0)
        );

        FeyrithBrain.Point chosen = FeyrithBrain.chooseAnchor(boss, players).orElseThrow();

        assertEquals(new FeyrithBrain.Point(2.0, 64.0, 2.0), chosen);
    }

    @Test
    void planTurnFallsBackToBossPositionWhenNoTargetsExist() {
        FeyrithBrain.Point boss = new FeyrithBrain.Point(4.0, 70.0, -3.0);
        FeyrithBrain.Snapshot snapshot = new FeyrithBrain.Snapshot(350.0, 350.0, boss, List.of());

        FeyrithBrain.Plan plan = FeyrithBrain.planTurn(snapshot, 0.95, 0.0, true, 0.99, false);

        assertEquals(1, plan.phase());
        assertEquals(FeyrithBrain.Attack.FIREBALL, plan.attack());
        assertEquals(boss, plan.anchor());
        assertEquals(3, plan.offsetX());
        assertEquals(-8, plan.offsetZ());
    }

    @Test
    void computeOffsetRespectsConfiguredRange() {
        int minimum = FeyrithBrain.computeOffset(0.0, true);
        int maximum = FeyrithBrain.computeOffset(0.999999, false);

        assertEquals(3, minimum);
        assertEquals(-8, maximum);
        assertTrue(Math.abs(FeyrithBrain.computeOffset(0.42, true)) >= 3);
        assertTrue(Math.abs(FeyrithBrain.computeOffset(0.42, true)) <= 8);
    }

    @Test
    void circlePointsStayOnRequestedRadius() {
        FeyrithBrain.Point center = new FeyrithBrain.Point(12.0, 70.0, -4.0);
        List<FeyrithBrain.Point> points = FeyrithBrain.circlePoints(center, 2.5, 12);

        assertEquals(12, points.size());
        for (FeyrithBrain.Point point : points) {
            double distanceSquared = center.distanceSquared(new FeyrithBrain.Point(point.x(), center.y(), point.z()));
            assertEquals(2.5 * 2.5, distanceSquared, 0.000001);
            assertEquals(center.y(), point.y());
        }
    }

    @Test
    void withinCircleUsesHorizontalDistance() {
        FeyrithBrain.Point center = new FeyrithBrain.Point(0.0, 64.0, 0.0);

        assertTrue(FeyrithBrain.isWithinCircle(center, new FeyrithBrain.Point(1.0, 80.0, 1.0), 1.5));
        assertFalse(FeyrithBrain.isWithinCircle(center, new FeyrithBrain.Point(2.0, 64.0, 2.0), 2.0));
    }

    @Test
    void withinConeAcceptsTargetsInFrontAndRejectsSideTargets() {
        FeyrithBrain.Point origin = new FeyrithBrain.Point(0.0, 64.0, 0.0);
        FeyrithBrain.Point aim = new FeyrithBrain.Point(0.0, 64.0, 8.0);

        assertTrue(FeyrithBrain.isWithinCone(origin, aim, new FeyrithBrain.Point(0.5, 64.0, 5.0), 8.0, 30.0));
        assertFalse(FeyrithBrain.isWithinCone(origin, aim, new FeyrithBrain.Point(4.5, 64.0, 4.5), 8.0, 30.0));
        assertFalse(FeyrithBrain.isWithinCone(origin, aim, new FeyrithBrain.Point(0.0, 64.0, 9.0), 8.0, 30.0));
    }
}
