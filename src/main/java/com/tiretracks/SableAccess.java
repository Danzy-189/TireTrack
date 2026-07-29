package com.tiretracks;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads the mass of the vehicle a wheel belongs to from Sable.
 *
 * <p>Sable groups a vehicle into a sub level, and that sub level carries the
 * mass directly, exactly as the in game debug dump shows it:</p>
 *
 * <pre>
 * Found 1 sub-levels:
 *   0c15b9ef-...: Position ... Orientation ... Mass: 79.0
 * </pre>
 *
 * <p>So the sub level is asked for its mass first, and only if that fails does
 * it fall back to a mass tracker object. The exact API differs between builds,
 * so it is discovered by reflection, but discovery happens at most a handful of
 * times for the whole game: the resolved field and methods are cached and a
 * permanent failure flag stops the lookup from ever running again. This is
 * called for every wheel several times a second, so it must be close to
 * free.</p>
 */
public final class SableAccess {

    private static final String SABLE_CLASS = "dev.ryanhcode.sable.Sable";
    private static final String HELPER_FIELD = "HELPER";
    private static final String GET_CONTAINING = "getContaining";
    private static final String GET_MASS_TRACKER = "getMassTracker";

    private static final String[] MASS_METHOD_NAMES = {
            "getMass",
            "mass",
            "getTotalMass",
            "totalMass",
            "getBodyMass",
            "getNormalMass"
    };

    private static final String[] MASS_FIELD_NAMES = {
            "mass",
            "totalMass",
            "bodyMass"
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

    private static volatile boolean loggedSource;

    /*
     * Cached per concrete class, because different Sable builds hand out
     * different sub level and mass tracker implementations.
     */
    private static final Map<Class<?>, Optional<Method>> MASS_METHODS =
            new ConcurrentHashMap<>();

    private static final Map<Class<?>, Optional<Field>> MASS_FIELDS =
            new ConcurrentHashMap<>();

    private static final Map<Class<?>, Optional<Method>> MASS_TRACKER_GETTERS =
            new ConcurrentHashMap<>();

    private SableAccess() {
    }

    /**
     * Vehicle mass in kilograms, or the fallback profile mass when Sable does
     * not expose a readable value.
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

            /*
             * The sub level itself reports the mass in the debug dump, so try it
             * before anything else.
             */
            Double direct = massOf(subLevel);

            if (direct != null) {
                logSource("sub level");

                return direct;
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

            Double tracked = massOf(massTracker);

            if (tracked != null) {
                logSource("mass tracker");
            }

            return tracked;
        } catch (Throwable ignored) {
            /*
             * Never let a mass lookup break vehicle physics.
             */
            return null;
        }
    }

    /**
     * Pulls a numeric mass off any object, by getter first and then by field.
     */
    private static Double massOf(Object owner) {
        if (owner == null) {
            return null;
        }

        Class<?> type = owner.getClass();

        try {
            Method getter = MASS_METHODS
                    .computeIfAbsent(type, SableAccess::findMassMethod)
                    .orElse(null);

            if (getter != null) {
                Double value = toMass(getter.invoke(owner));

                if (value != null) {
                    return value;
                }
            }

            Field field = MASS_FIELDS
                    .computeIfAbsent(type, SableAccess::findMassField)
                    .orElse(null);

            if (field != null) {
                return toMass(field.get(owner));
            }
        } catch (Throwable ignored) {
            /*
             * Treated as unreadable.
             */
        }

        return null;
    }

    private static Double toMass(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }

        double mass = number.doubleValue();

        return Double.isFinite(mass) && mass > 0.0D
                ? mass
                : null;
    }

    private static void logSource(String source) {
        if (loggedSource) {
            return;
        }

        loggedSource = true;

        TireTracks.LOGGER.info(
                "[TireTracks] Reading vehicle mass from the Sable {}.",
                source
        );
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
                    "[TireTracks] Sable sub level API resolved; vehicle mass classes are live."
            );
        } else if (resolveAttempts >= MAX_RESOLVE_ATTEMPTS) {
            resolved = true;
            TireTracks.LOGGER.info(
                    "[TireTracks] Sable sub level API not found; every vehicle uses the medium profile."
            );
        }

        return available;
    }

    private static Optional<Method> findMassMethod(Class<?> type) {
        for (String name : MASS_METHOD_NAMES) {
            Method method = findMethod(type, name, 0);

            if (method != null
                    && Number.class.isAssignableFrom(box(method.getReturnType()))) {
                return Optional.of(method);
            }
        }

        return Optional.empty();
    }

    private static Optional<Field> findMassField(Class<?> type) {
        for (String name : MASS_FIELD_NAMES) {
            Class<?> current = type;

            while (current != null) {
                try {
                    Field field = current.getDeclaredField(name);

                    if (Number.class.isAssignableFrom(box(field.getType()))) {
                        field.setAccessible(true);

                        return Optional.of(field);
                    }
                } catch (Throwable ignored) {
                    /*
                     * Not on this class, keep walking up.
                     */
                }

                current = current.getSuperclass();
            }
        }

        return Optional.empty();
    }

    private static Class<?> box(Class<?> type) {
        if (type == double.class) {
            return Double.class;
        }

        if (type == float.class) {
            return Float.class;
        }

        if (type == int.class) {
            return Integer.class;
        }

        if (type == long.class) {
            return Long.class;
        }

        return type;
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
