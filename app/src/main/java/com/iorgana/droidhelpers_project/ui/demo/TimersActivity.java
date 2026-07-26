package com.iorgana.droidhelpers_project.ui.demo;

import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.timers.ChronometerTimer;
import com.iorgana.droidhelpers.timers.CountdownTimer;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

/**
 * TimersActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.timers package:
 * CountdownTimer (counts down to zero) and ChronometerTimer (counts up).
 * Both fire their listeners on a background thread, so UI updates are posted
 * back to the main thread via runOnUiThread().
 */
public class TimersActivity extends BaseDemoActivity {

    private CountdownTimer countdownTimer;
    private ChronometerTimer chronometerTimer;

    @Override
    protected String getScreenTitle() {
        return "Timers";
    }

    @Override
    protected void buildContent() {
        buildCountdownSection();
        buildChronometerSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildCountdownSection() {
        LinearLayout s = addSection("CountdownTimer", "Counts down from N seconds to zero. Supports pause/resume/delay/end.");

        EditText secondsInput = addInput(s, "Total seconds", "20");

        Row liveRow = addRow(s, "setListener(TimerListener)  (live onChange / onStateChanged / onComplete)");
        liveRow.output.setText("(not started yet)");

        runSafe(addRow(s, "new CountdownTimer(totalSeconds)"), () -> {
            int seconds = Integer.parseInt(secondsInput.getText().toString());
            countdownTimer = new CountdownTimer(seconds);
            countdownTimer.setListener(new CountdownTimer.TimerListener() {
                @Override public void onChange(int currentSecond) {
                    runOnUiThread(() -> liveRow.output.setText("onChange: " + currentSecond + "s remaining"));
                }
                @Override public void onStateChanged(CountdownTimer.State state) {
                    runOnUiThread(() -> liveRow.output.setText("onStateChanged: " + state));
                }
                @Override public void onComplete() {
                    runOnUiThread(() -> liveRow.output.setText("onComplete: reached zero"));
                }
            });
            return "Created with " + seconds + "s (listener wired to the row below)";
        });

        runSafe(addRow(s, "start()"), () -> {
            countdownTimer.start();
            return "Started";
        });
        runSafe(addRow(s, "pause()"), () -> {
            countdownTimer.pause();
            return "Paused";
        });
        runSafe(addRow(s, "resume()"), () -> {
            countdownTimer.resume();
            return "Resumed";
        });
        runSafe(addRow(s, "delay(10)  (adds 10 seconds back)"), () -> {
            countdownTimer.delay(10);
            return "Added 10s, current=" + countdownTimer.getCurrentValue();
        });
        runSafe(addRow(s, "end()  (stops immediately)"), () -> {
            countdownTimer.end();
            return "Ended";
        });
        runSafe(addRow(s, "isRunning() / getCurrentValue()"), () ->
                "isRunning=" + countdownTimer.isRunning() + ", currentValue=" + countdownTimer.getCurrentValue());
    }

    /* ------------------------------------------------------------------ */
    private void buildChronometerSection() {
        LinearLayout s = addSection("ChronometerTimer", "Counts up from zero (a stopwatch). Supports pause/resume/stop.");

        Row liveRow = addRow(s, "setListener(OnTimeChange)  (live elapsed time, String and Long overloads)");
        liveRow.output.setText("(not started yet)");

        chronometerTimer = new ChronometerTimer();
        chronometerTimer.setListener(new ChronometerTimer.OnTimeChange() {
            @Override public void onChange(String currentTime) {
                runOnUiThread(() -> liveRow.output.setText("elapsed = " + currentTime));
            }
            @Override public void onChange(Long currentTime) {
                // Same tick as the String overload above; millis available here if needed.
            }
        });

        runSafe(addRow(s, "start()"), () -> {
            chronometerTimer.start();
            return "Started";
        });
        runSafe(addRow(s, "pause()"), () -> {
            chronometerTimer.pause();
            return "Paused";
        });
        runSafe(addRow(s, "resume()"), () -> {
            chronometerTimer.resume();
            return "Resumed";
        });
        runSafe(addRow(s, "stop()  (resets to zero)"), () -> {
            chronometerTimer.stop();
            return "Stopped & reset";
        });
        runSafe(addRow(s, "isCounting() / isPaused()"), () ->
                "isCounting=" + chronometerTimer.isCounting() + ", isPaused=" + chronometerTimer.isPaused());
        runSafe(addRow(s, "getLastString() / getLastLong()"), () ->
                "lastString=" + chronometerTimer.getLastString() + ", lastLong=" + chronometerTimer.getLastLong());
        runSafe(addRow(s, "static longToString(millis)"), () -> ChronometerTimer.longToString(3_723_000L));
    }
}
