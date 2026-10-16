package com.atlasx.gis.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small executor wrapper used for file work (GeoJSON import/export) and other
 * operations that must not run on the main thread.
 *
 * <p>The course material covers {@code AsyncTask}; it has been deprecated since
 * API 30, so Atlas X uses a shared executor plus a main-thread handler, which
 * is the supported replacement pattern.</p>
 */
public final class AppExecutors {

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    /** Runs {@code task} on the shared background executor. */
    public static void runOnBackground(Runnable task) {
        IO.execute(task);
    }

    /** Runs {@code task} on the main thread. */
    public static void runOnMain(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            MAIN.post(task);
        }
    }

    /** Convenience: background work then a main-thread callback. */
    public static void backgroundThenMain(Runnable background, Runnable onMain) {
        IO.execute(() -> {
            try {
                background.run();
            } catch (RuntimeException e) {
                // Never let a background failure kill the process; callers are
                // responsible for surfacing their own error state.
                android.util.Log.e("AtlasX", "Background task failed", e);
            } finally {
                MAIN.post(onMain);
            }
        });
    }
}
