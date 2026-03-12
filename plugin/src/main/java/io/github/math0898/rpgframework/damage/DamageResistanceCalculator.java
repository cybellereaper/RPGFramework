package io.github.math0898.rpgframework.damage;

final class DamageResistanceCalculator {

    private DamageResistanceCalculator() {
    }

    static double applyResistance(double damage, DamageResistance resistance) {
        if (resistance == null) {
            throw new IllegalArgumentException("Unexpected value: null");
        }

        return switch (resistance) {
            case IMMUNITY -> 0.00;
            case RESISTANCE -> damage * 0.50;
            case NORMAL -> damage;
            case SUSCEPTIBILITY -> damage * 1.50;
            case VULNERABILITY -> damage * 2.00;
        };
    }
}
