package com.iorgana.droidhelpers.timers;


public class CountdownTimer {

    public interface TimerListener {
        void onChange(int currentSecond);
        void onStateChanged(State state);
        void onComplete();
    }

    public enum State {
        RUNNING, PAUSED, STOPPED, COMPLETED
    }

    private int totalSeconds;
    private int currentSeconds;
    private State state = State.STOPPED;

    private TimerListener listener;
    private final Object lock = new Object();
    private boolean stopFlag = false;

    public CountdownTimer(int totalSeconds) {
        this.totalSeconds = totalSeconds;
        this.currentSeconds = totalSeconds;
    }

    public void setListener(TimerListener listener) {
        this.listener = listener;
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public int getCurrentValue() {
        return currentSeconds;
    }

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

    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            notifyStateChanged();
        }
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.RUNNING;
            notifyStateChanged();
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    }

    public void delay(int seconds) {
        currentSeconds += seconds;
        notifyChange();
    }

    public void end() {
        stopFlag = true;
        state = State.STOPPED;
        notifyStateChanged();
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    // Private helpers
    private void notifyChange() {
        if (listener != null) {
            listener.onChange(currentSeconds);
        }
    }

    private void notifyStateChanged() {
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    private void notifyComplete() {
        if (listener != null) {
            listener.onComplete();
        }
    }
}