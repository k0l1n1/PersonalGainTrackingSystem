package com.personaltracker.util;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global Real-Time Data Synchronization and Event Notification Manager.
 * Ensures that when health and fitness parameters (such as weight, goals,
 * workouts, nutrition, water, sleep, or profile settings) are modified
 * from any interface or dialog, all affected views across the application
 * are automatically and immediately refreshed in real time.
 */
public final class DataSyncManager {
    private static final Logger LOGGER = Logger.getLogger(DataSyncManager.class.getName());

    public enum DataType {
        WEIGHT_MEASUREMENT,
        GOALS,
        WORKOUTS,
        NUTRITION,
        WATER,
        SLEEP,
        PROFILE,
        REMINDERS
    }

    @FunctionalInterface
    public interface DataChangeListener {
        void onDataChanged(DataType type);
    }

    private static final List<DataChangeListener> LISTENERS = new CopyOnWriteArrayList<>();

    private DataSyncManager() {
        // Utility class
    }

    /**
     * Registers a listener to be notified whenever application data changes.
     */
    public static void registerListener(DataChangeListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     * Unregisters a previously registered data change listener.
     */
    public static void unregisterListener(DataChangeListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    /**
     * Dispatches a data change event to all registered listeners.
     * Guaranteed to execute on the Swing Event Dispatch Thread (EDT) for thread-safe UI updates.
     */
    public static void fireDataChanged(DataType type) {
        if (type == null) return;
        for (DataChangeListener listener : LISTENERS) {
            SwingUtilities.invokeLater(() -> {
                try {
                    listener.onDataChanged(type);
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "DataChangeListener dispatch notice: " + e.getMessage());
                }
            });
        }
    }

    /**
     * Convenience method to dispatch multiple data change events simultaneously.
     */
    public static void fireDataChanged(DataType... types) {
        if (types == null) return;
        for (DataType type : types) {
            fireDataChanged(type);
        }
    }
}
