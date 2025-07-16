package com.iorgana.droidhelpers.timers;

import java.util.Locale;

public class ChronometerTimer {
    private long startTime;
    private long elapsedTime;
    private boolean isRunning;
    private boolean isPaused;
    private OnTimeChange onTimeChange;
    private OnTimeListener onTimeListener;

    private String lastValueStr="";
    private Long lastValueLong=0L;

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
    public void setListener(OnTimeChange onTimeChange){
        this.onTimeChange = onTimeChange;
    }

    /**
     * Add Listener (Option 2)
     * @param onTimeListener {@link OnTimeListener}
     */
    public void setListener(OnTimeListener onTimeListener){
        this.onTimeListener = onTimeListener;
    }

    /**
     * Start Chronometer from 0
     * --------------------------------------------------------------
     */
    public void start() {
        if (!isRunning) {
            startTime = System.currentTimeMillis();
            isRunning = true;
            isPaused = false;

            // Start a new thread to update the time
            new Thread(() -> {
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
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    /**
     * Pause Chronometer
     * --------------------------------------------------------------
     */
    public void pause() {
        isRunning = false;
        isPaused = true;
    }

    /**
     * Resume Chronometer
     * --------------------------------------------------------------
     */
    public void resume() {
        if (!isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime;
            isRunning = true;
            isPaused = false;
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

        // Notify Listener
        if(onTimeChange!=null){
            onTimeChange.onChange("00:00:00");
            onTimeChange.onChange(0L);
        }
        // Notify Abstract
        if(onTimeListener!=null){
            onTimeListener.onChange("00:00:00");
            onTimeListener.onChange(0L);
        }
    }

    /**
     * Is Counting
     */
    public boolean isCounting(){
        return this.isRunning;
    }

    /**
     * Is Paused
     */
    public boolean isPaused(){
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

        return String.format(Locale.ENGLISH ,"%02d:%02d:%02d", hours, minutes, seconds);
    }


    /*-------------------------------[Listeners]---------------------------*/

    /**
     * We can ether use Interface listener {@link OnTimeChange}
     * Or we can use Abstract Listener {@link OnTimeListener}
     */
    public interface OnTimeChange{
        void onChange(String currentTime);
        void onChange(Long currentTime);
    }
    public abstract static class OnTimeListener{
        public abstract void onChange(String currentTime);
        public abstract void onChange(Long currentTime);
    }
}