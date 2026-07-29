package com.tiretracks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the total mass of the vehicle a wheel belongs to from Sable.
 *
 * <p>The exact Sable API differs between builds, so it is discovered by
 * reflection. Discovery happens at most a handful of times for the whole game:
 * the resolved field and methods are cached, and a permanent failure flag stops
 * the lookup from ever running again. This is called for every wheel several
 * times a second, so it must be close to free.</p>
 */
public final class SableAccess {

    private static final String SABLE_CLASS = "dev.ryanhcode.sable.Sable";
    private static final String HELPER_FIELD = "HELPER";
    private static final String GET_CONTAINING = "getContaining";
    private static final String GET_MASS_TRACKER = "getMassTracker";

    private static final String[] MASS_VALUE_NAMES = {
            "getMass",
            "mass",
            "getTotalMass",
            "getBodyMass",
            "getNormalMass"
    };

    /**
     * Sable may not have populated its helper yet during the first ticks, so a
     * few retries are allowed before giving up for good.
     */
    private static final int MAX_RESOLVE_ATTEMPTS = 20;

    private static volatile boolean resolved;
    private static volatile boolean available;
    private static volatile Object helper;
    private static volatile Method getContaining;
    private static int resolveAttempts;

    /**
     * Cached per concrete class, because different Sable builds hand out
     * different sub level and mass tracker implementations.
     */
    private static final Map<Class<?>, Optional<Method>> MASS_TRACKER_GETTERS =
            new ConcurrentHashMap<>();

    private static final Map<Class<?>, Optional<Method>> MASS_VALUE_GETTERS =
            new ConcurrentHashMap<>();

    private SableAccess() {
    }

    /**
     * Total vehicle mass in kilograms, or the fallback profile mass when Sable
     * does not expose a readable value.
     */
    public static double vehicleMass(Object wheelBlockEntity) {
        Double mass = readMass(wheelBlockEntity);

        if (mass != null
                && Double.isFinite(mass)
                && mass > 0.0D) {
            return mass;
        }

        return fallbackMass();
    }

    /**
     * Middle of the medium band: strictly above the light bound and at or below
     * the medium bound, so an unreadable mass always resolves to the medium
     * profile instead of breaking the game.
     */
    public static double fallbackMass() {
        double lightBound = TireTracksConfig.lightVehicleMaxMass();

        double mediumBound = Math.max(
                TireTracksConfig.mediumVehicleMaxMass(),
                lightBound
        );

        return Math.max(
                Math.nextUp(lightBound),
                (lightBound + mediumBound) * 0.5D
        );
    }

    private static Double readMass(Object wheelBlockEntity) {
        if (wheelBlockEntity == null || !ensureResolved()) {
            return null;
        }

        try {
            Object subLevel = getContaining.invoke(helper, wheelBlockEntity);

            if (subLevel == null) {
                return null;
            }

            Method massTrackerGetter = MASS_TRACKER_GETTERS
                    .computeIfAbsent(
                            subLevel.getClass(),
                            type -> Optional.ofNullable(
                                    findMethod(type, GET_MASS_TRACKER, 0)
                            )
                    )
                    .orElse(null);

            if (massTrackerGetter == null) {
                return null;
            }

            Object massTracker = massTrackerGetter.invoke(subLevel);

            if (massTracker == null) {
                return null;
            }

            Method massValueGetter = MASS_VALUE_GETTERS
                    .computeIfAbsent(
                            massTracker.getClass(),
                            SableAccess::findMassValueGetter
                    )
                    .orElse(null);

            if (massValueGetter == null) {
                return null;
            }

            Object value = massValueGetter.invoke(massTracker);

            return value instanceof Number number
                    ? number.doubleValue()
                    : null;
        } catch (Throwable ignored) {
            /*
             * Never let a mass lookup break vehicle physics.
             */
            return null;
        }
    }

    private static boolean ensureResolved() {
        if (resolved) {
            return available;
        }

        return resolve();
    }

    private static synchronized boolean resolve() {
        if (resolved) {
            return available;
        }

        resolveAttempts++;

        try {
            Class<?> sableClass = Class.forName(SABLE_CLASS);

            Field helperField = sableClass.getField(HELPER_FIELD);
            helperField.setAccessible(true);

            Object resolvedHelper = helperField.get(null);

            if (resolvedHelper != null) {
                Method resolvedGetContaining = findMethod(
                        resolvedHelper.getClass(),
                        GET_CONTAINING,
                        1
                );

                if (resolvedGetContaining != null) {
                    helper = resolvedHelper;
                    getContaining = resolvedGetContaining;
                    available = true;
                }
            }
        } catch (Throwable ignored) {
            /*
             * Sable is optional: fall through to the fallback profile.
             */
        }

        if (available) {
            resolved = true;
            TireTracks.LOGGER.info(
                    "[TireTracks] Sable mass API resolved; vehicle mass classes are live."
            );
        } else if (resolveAttempts >= MAX_RESOLVE_ATTEMPTS) {
            resolved = true;
            TireTracks.LOGGER.info(
                    "[TireTracks] Sable mass API not found; every vehicle uses the medium profile."
            );
        }

        return available;
    }

    private static Optional<Method> findMassValueGetter(Class<?> type) {
        for (String name : MASS_VALUE_NAMES) {
            Method method = findMethod(type, name, 0);

            if (method != null) {
                return Optional.of(method);
            }
        }

        return Optional.empty();
    }

    private static Method findMethod(
            Class<?> type,
            String name,
            int parameterCount
    ) {
        Class<?> current = type;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name)
                        && method.getParameterCount() == parameterCount) {
                    try {
                        method.setAccessible(true);
                    } catch (Throwable ignored) {
                        /*
                         * Still usable if it happens to be public.
                         */
                    }

                    return method;
                }
            }

            current = current.getSuperclass();
        }

        return null;
    }
}
