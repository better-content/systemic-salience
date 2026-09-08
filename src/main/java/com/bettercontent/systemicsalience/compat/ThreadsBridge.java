package com.bettercontent.systemicsalience.compat;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/** Optional reflection-only access to Better Content Threads' public producer API. */
public final class ThreadsBridge {
    static final String API_CLASS = "com.bettercontent.threads.api.ThreadSignals";
    private static Method emit;
    private static Method activeCorrelation;
    private static boolean resolved;
    private static boolean available;

    private ThreadsBridge() {}

    public static boolean available() {
        resolve();
        return available;
    }

    public static String activeCorrelation(ServerPlayer player, String threadId) {
        resolve();
        if (!available) return null;
        try {
            Object value = activeCorrelation.invoke(null, player, threadId);
            return value instanceof String token && !token.isBlank() ? token : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    public static boolean emit(ServerPlayer player, String type, String value, String token) {
        resolve();
        if (!available) return false;
        try {
            emit.invoke(null, player, type, value, token);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> api = Class.forName(API_CLASS);
            emit = api.getMethod("emit", ServerPlayer.class, String.class, String.class, String.class);
            activeCorrelation = api.getMethod("activeCorrelation", ServerPlayer.class, String.class);
            available = true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            available = false;
        }
    }
}
