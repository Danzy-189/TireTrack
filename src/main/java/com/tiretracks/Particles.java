package com.tiretracks;

/**
 * One place that decides how many particles a puff is made of.
 *
 * <p>Every emitter in the mod runs its counts through {@link #count(int)}, so a
 * single config value scales the whole mod at once instead of twenty magic
 * numbers drifting apart.</p>
 */
public final class Particles {

    private Particles() {
    }

    /**
     * Scales a base particle count by the configured volume multiplier.
     *
     * <p>A puff that is meant to exist never scales down to nothing: anything
     * above zero stays at least one particle, otherwise low multipliers would
     * silently delete whole effects.</p>
     */
    public static int count(int base) {
        if (base <= 0) {
            return 0;
        }

        double scaled = base * TireTracksConfig.particleVolumeMultiplier();

        return Math.max(1, (int) Math.round(scaled));
    }

    /**
     * Same as {@link #count(int)} for emitters that want a fraction of the base
     * amount, without rounding themselves down to zero first.
     */
    public static int fraction(int base, double factor) {
        return count(Math.max(1, (int) Math.round(base * factor)));
    }
}
