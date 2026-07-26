package com.iorgana.droidhelpers.timers;


/**
 * ************************************************************************
 * CountdownTimer
 * ************************************************************************
 * - A timer that counts down from a specified number of seconds to zero.
 * - Supports pausing, resuming, delaying, and ending the countdown.
 * - Notifies listeners of changes, state changes, and completion.
 * ------------------------------------------------------------------------
 * @apiNote Listeners are invoked in a background thread. Switch to the main
 *          thread if you need to update the UI.
 */
public class CountdownTimer {

    /**
     * ************************************************************************
     * TimerListener
     * ************************************************************************
     * - Listener interface for countdown timer events.
     */
    public interface TimerListener {
        void onChange(int currentSecond);
        void onStateChanged(State state);
        void onComplete();
    }

    /**
     * ************************************************************************
     * State
     * ************************************************************************
     * - Enum representing the possible states of the countdown timer.
     */
    public enum State {
        RUNNING, PAUSED, STOPPED, COMPLETED
    }

    private int totalSeconds;
    private int currentSeconds;
    private State state = State.STOPPED;

    private TimerListener listener;
    private final Object lock = new Object();
    private boolean stopFlag = false;

    /**
     * ************************************************************************
     * CountdownTimer (Constructor)
     * ************************************************************************
     * - Create a countdown timer with the specified total seconds.
     * ------------------------------------------------------------------------
     * @param totalSeconds The total countdown duration in seconds.
     */
    public CountdownTimer(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.currentSeconds = totalSeconds;
    }

    /**
     * ************************************************************************
     * setListener()
     * ************************************************************************
     * - Register a TimerListener for countdown events.
     * ------------------------------------------------------------------------
     * @param listener The TimerListener callback.
     */
    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    /**
     * ************************************************************************
     * isRunning()
     * ************************************************************************
     * - Check if the countdown timer is currently running.
     * ------------------------------------------------------------------------
     * @return true if running, false otherwise.
     */
    public boolean isRunning() {
        return state == State.RUNNING;
    }

    /**
     * ************************************************************************
     * getCurrentValue()
     * ************************************************************************
     * - Get the current remaining seconds.
     * ------------------------------------------------------------------------
     * @return The current second value.
     */
    public int getCurrentValue() {
        return currentSeconds;
    }

    /**
     * ************************************************************************
     * start()
     * ************************************************************************
     * - Start the countdown timer.
     * ------------------------------------------------------------------------
     * @apiNote Listeners are invoked in a background thread. Switch to the main
     *          thread if you need to update the UI.
     */
    public void start() {
        if (state == State.RUNNING) return;
        stopFlag = false;
        state = State.RUNNING;
        notifyStateChanged();
        Thread timerThread = new Thread(() -> {
            while (currentSeconds > 0 && !stopFlag) {
                synchronized (lock) {
                    try {
                        while (state == State.PAUSED) {
                            lock.wait();
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

                if (state == State.RUNNING) {
                    currentSeconds--;
                    notifyChange();

                    if (currentSeconds <= 0) {
                        state = State.COMPLETED;
                        notifyStateChanged();
                        notifyComplete();
                    }
                }
            }
        });
        timerThread.start();
    }

    /**
     * ************************************************************************
     * pause()
     * ************************************************************************
     * - Pause the countdown timer.
     */
    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            notifyStateChanged();
        }
    }

    /**
     * ************************************************************************
     * resume()
     * ************************************************************************
     * - Resume the countdown timer from a paused state.
     */
    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            notifyStateChanged();
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    /**
     * ************************************************************************
     * delay()
     * ************************************************************************
     * - Add extra seconds to the remaining countdown time.
     * ------------------------------------------------------------------------
     * @param seconds The number of seconds to add.
     */
    public void delay(int seconds) {
        currentSeconds += seconds;
        notifyChange();
    }

    /**
     * ************************************************************************
     * end()
     * ************************************************************************
     * - End the countdown timer immediately and set state to STOPPED.
     */
    public void end() {
        stopFlag = true;
        state = State.STOPPED;
        notifyStateChanged();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * ************************************************************************
     * notifyChange() (Private)
     * ************************************************************************
     * - Notify the listener of the current second change.
     */
    private void notifyChange() {
        if (listener != null) {
            listener.onChange(currentSeconds);
        }
    }

    /**
     * ************************************************************************
     * notifyStateChanged() (Private)
     * ************************************************************************
     * - Notify the listener of a state change.
     */
    private void notifyStateChanged() {
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    /**
     * ************************************************************************
     * notifyComplete() (Private)
     * ************************************************************************
     * - Notify the listener that the countdown has completed.
     */
    private void notifyComplete() {
        if (listener != null) {
            listener.onComplete();
        }
    }
}