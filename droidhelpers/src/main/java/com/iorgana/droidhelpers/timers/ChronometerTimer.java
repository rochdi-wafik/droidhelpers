package com.iorgana.droidhelpers.timers;

import java.util.Locale;

import java.util.Locale;

/**
 * Chronometer Timer
 * -------------------------------------------------------------------------------
 * - This timer counts up from zero and can be paused, resumed, and stopped.
 * - It notifies listeners of changes in the current time in both string and long formats.
 * -------------------------------------------------------------------------------
 * @apiNote Warning: Listener all invoked in background thread, Make sure to switch
 *         to main thread if you want to update UI.
 */
public class ChronometerTimer {
    private long startTime;
    private long elapsedTime;
    private volatile boolean isRunning;
    private volatile boolean isPaused;
    private OnTimeChange onTimeChange;
    private OnTimeListener onTimeListener;

    private volatile String lastValueStr = "";
    private volatile Long lastValueLong = 0L;

    private Thread timerThread;

    /**
     * Get Value
     */
    public String getLastString() {
        return this.lastValueStr;
    }

    public Long getLastLong() {
        return this.lastValueLong;
    }

    /**
     * Add Listener (Option 1)
     * @param onTimeChange {@link OnTimeChange}
     */
    public void setListener(OnTimeChange onTimeChange) {
        this.onTimeChange = onTimeChange;
    }

    /**
     * Add Listener (Option 2)
     * @param onTimeListener {@link OnTimeListener}
     */
    public void setListener(OnTimeListener onTimeListener) {
        this.onTimeListener = onTimeListener;
    }

    /**
     * Start Chronometer from 0
     * --------------------------------------------------------------
     *  @apiNote Warning: Listener all invoked in background thread, Make sure to switch
     *           to main thread if you want to update UI.
     */
    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            elapsedTime = 0; // Explicitly reset for a clean start
            isRunning = true;
            isPaused = false;
            startTimerThread();
        }
    }

    /**
     * Pause Chronometer
     * --------------------------------------------------------------
     */
    public void pause() {
        isRunning = false;
        isPaused = true;
        if (timerThread != null) {
            timerThread.interrupt(); // Stop the thread immediately
        }
    }

    /**
     * Resume Chronometer
     * --------------------------------------------------------------
     */
    public void resume() {
        if (isPaused && !isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime;
            isRunning = true;
            isPaused = false;
            startTimerThread(); // Actually start the new thread
        }
    }

    /**
     * Stop & Reset
     * --------------------------------------------------------------
     */
    public void stop() {
        isRunning = false;
        isPaused = false;
        elapsedTime = 0;

        if (timerThread != null) {
            timerThread.interrupt(); // Stop the thread immediately
            timerThread = null;
        }

        // Notify Listener
        if (onTimeChange != null) {
            onTimeChange.onChange("00:00:00");
            onTimeChange.onChange(0L);
        }
        // Notify Abstract
        if (onTimeListener != null) {
            onTimeListener.onChange("00:00:00");
            onTimeListener.onChange(0L);
        }
    }

    /**
     * Is Counting
     */
    public boolean isCounting() {
        return this.isRunning;
    }

    /**
     * Is Paused
     */
    public boolean isPaused() {
        return this.isPaused;
    }

    /**
     * Long Time To String
     * --------------------------------------------------------------
     * Format long to human readable: "hours:minutes:seconds"
     */
    public static String longToString(long time) {
        long hours = (time / (1000 * 60 * 60)) % 24;
        long minutes = (time / (1000 * 60)) % 60;
        long seconds = (time / 1000) % 60;

        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Starts the background thread for time updates
     */
    private void startTimerThread() {
        timerThread = new Thread(() -> {
            while (isRunning) {
                long currentTime = System.currentTimeMillis();
                elapsedTime = currentTime - startTime;

                String timeStr = longToString(elapsedTime);
                this.lastValueStr = timeStr;
                this.lastValueLong = elapsedTime;

                // Notify Listener
                if (onTimeChange != null) {
                    onTimeChange.onChange(timeStr);
                    onTimeChange.onChange(elapsedTime);
                }
                // Notify Abstract
                if (onTimeListener != null) {
                    onTimeListener.onChange(timeStr);
                    onTimeListener.onChange(elapsedTime);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Thread was interrupted (e.g., by pause() or stop())
                    Thread.currentThread().interrupt(); // Preserve interrupt status
                    break; // Exit the loop cleanly
                }
            }
        });
        timerThread.start();
    }

    /*-------------------------------[Listeners]---------------------------*/

    /**
     * We can either use Interface listener {@link OnTimeChange}
     * Or we can use Abstract Listener {@link OnTimeListener}
     */
    public interface OnTimeChange {
        void onChange(String currentTime);
        void onChange(Long currentTime);
    }

    public abstract static class OnTimeListener {
        public abstract void onChange(String currentTime);
        public abstract void onChange(Long currentTime);
    }
}