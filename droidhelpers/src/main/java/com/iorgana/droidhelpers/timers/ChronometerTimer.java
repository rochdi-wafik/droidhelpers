package com.iorgana.droidhelpers.timers;

import java.util.Locale;

import java.util.Locale;

/**
 * ************************************************************************
 * ChronometerTimer
 * ************************************************************************
 * - A timer that counts up from zero, supporting pause, resume, and stop.
 * - Notifies listeners of time changes in both string and long formats.
 * ------------------------------------------------------------------------
 * @apiNote Listeners are invoked in a background thread. Switch to the main
 *          thread if you need to update the UI.
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
     * ************************************************************************
     * getLastString()
     * ************************************************************************
     * - Get the last recorded time value as a string.
     * ------------------------------------------------------------------------
     * @return The last time string (e.g., "00:00:00").
     */
    public String getLastString() {
        return this.lastValueStr;
    }

    /**
     * ************************************************************************
     * getLastLong()
     * ************************************************************************
     * - Get the last recorded elapsed time in milliseconds.
     * ------------------------------------------------------------------------
     * @return The last elapsed time as a long value.
     */
    public Long getLastLong() {
        return this.lastValueLong;
    }

    /**
     * ************************************************************************
     * setListener() (Option 1)
     * ************************************************************************
     * - Register an OnTimeChange listener.
     * ------------------------------------------------------------------------
     * @param onTimeChange The OnTimeChange callback.
     */
    public void setListener(OnTimeChange onTimeChange) {
        this.onTimeChange = onTimeChange;
    }

    /**
     * ************************************************************************
     * setListener() (Option 2)
     * ************************************************************************
     * - Register an OnTimeListener (abstract class).
     * ------------------------------------------------------------------------
     * @param onTimeListener The OnTimeListener callback.
     */
    public void setListener(OnTimeListener onTimeListener) {
        this.onTimeListener = onTimeListener;
    }

    /**
     * ************************************************************************
     * start()
     * ************************************************************************
     * - Start the chronometer counting from zero.
     * ------------------------------------------------------------------------
     * @apiNote Listeners are invoked in a background thread. Switch to the main
     *          thread if you need to update the UI.
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
     * ************************************************************************
     * pause()
     * ************************************************************************
     * - Pause the chronometer.
     */
    public void pause() {
        isRunning = false;
        isPaused = true;
        if (timerThread != null) {
            timerThread.interrupt(); // Stop the thread immediately
        }
    }

    /**
     * ************************************************************************
     * resume()
     * ************************************************************************
     * - Resume the chronometer from where it was paused.
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
     * ************************************************************************
     * stop()
     * ************************************************************************
     * - Stop the chronometer and reset the elapsed time to zero.
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
     * ************************************************************************
     * isCounting()
     * ************************************************************************
     * - Check if the chronometer is currently running.
     * ------------------------------------------------------------------------
     * @return true if counting, false otherwise.
     */
    public boolean isCounting() {
        return this.isRunning;
    }

    /**
     * ************************************************************************
     * isPaused()
     * ************************************************************************
     * - Check if the chronometer is paused.
     * ------------------------------------------------------------------------
     * @return true if paused, false otherwise.
     */
    public boolean isPaused() {
        return this.isPaused;
    }

    /**
     * ************************************************************************
     * longToString()
     * ************************************************************************
     * - Format a time in milliseconds to a human-readable string (HH:mm:ss).
     * ------------------------------------------------------------------------
     * @param time The time in milliseconds.
     * @return The formatted time string.
     */
    public static String longToString(long time) {
        long hours = (time / (1000 * 60 * 60)) % 24;
        long minutes = (time / (1000 * 60)) % 60;
        long seconds = (time / 1000) % 60;

        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * ************************************************************************
     * startTimerThread()
     * ************************************************************************
     * - Start the background thread that updates the elapsed time.
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
     * ************************************************************************
     * OnTimeChange
     * ************************************************************************
     * - Interface listener for time change events.
     * - Alternatively, use {@link OnTimeListener} for an abstract class.
     */
    public interface OnTimeChange {
        void onChange(String currentTime);
        void onChange(Long currentTime);
    }

    /**
     * ************************************************************************
     * OnTimeListener
     * ************************************************************************
     * - Abstract class listener for time change events.
     * - Alternatively, use {@link OnTimeChange} for an interface.
     */
    public abstract static class OnTimeListener {
        public abstract void onChange(String currentTime);
        public abstract void onChange(Long currentTime);
    }
}