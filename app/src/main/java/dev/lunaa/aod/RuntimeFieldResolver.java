package dev.lunaa.aod;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Conservative reflection helper for ROMs that rename private SystemUI fields. */
public final class RuntimeFieldResolver {
    private RuntimeFieldResolver() {}

    public static Object readExactOrUniqueAssignable(
            Object instance,
            String preferredName,
            Class<?> requiredType
    ) {
        if (instance == null || preferredName == null || requiredType == null) return null;

        Object exact = readNamed(instance, preferredName);
        if (requiredType.isInstance(exact)) return exact;

        Object unique = null;
        int matches = 0;
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            Field[] fields;
            try {
                fields = type.getDeclaredFields();
            } catch (Throwable ignored) {
                return null;
            }
            for (Field field : fields) {
                try {
                    if (Modifier.isStatic(field.getModifiers())) continue;
                    if (!requiredType.isAssignableFrom(field.getType())) continue;
                    field.setAccessible(true);
                    Object value = field.get(instance);
                    if (!requiredType.isInstance(value)) continue;
                    unique = value;
                    matches++;
                    if (matches > 1) return null;
                } catch (Throwable ignored) {
                    // A hidden/inaccessible candidate cannot be used safely.
                }
            }
        }
        return matches == 1 ? unique : null;
    }

    private static Object readNamed(Object instance, String fieldName) {
        for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(fieldName);
                if (Modifier.isStatic(field.getModifiers())) return null;
                field.setAccessible(true);
                return field.get(instance);
            } catch (NoSuchFieldException ignored) {
                // Continue up the hierarchy.
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }
}
