package com.iorgana.droidhelpers_project.ui.demo;

import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.iorgana.droidhelpers.runtime.RestartHelper;
import com.iorgana.droidhelpers.service.ServiceHelper;
import com.iorgana.droidhelpers.stream.StreamUtils;
import com.iorgana.droidhelpers.system.LanguageHelper;
import com.iorgana.droidhelpers.utils.JPatterns;
import com.iorgana.droidhelpers.utils.Utils;
import com.iorgana.droidhelpers_project.service.DemoService;
import com.iorgana.droidhelpers_project.ui.MainActivity;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

/**
 * SystemUtilsActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for the smaller droidhelpers packages, grouped together
 * as one "System & Utils" box: RestartHelper (runtime), ServiceHelper (service),
 * StreamUtils (stream), LanguageHelper (system), Utils (utils), JPatterns (utils).
 */
public class SystemUtilsActivity extends BaseDemoActivity {

    @Override
    protected String getScreenTitle() {
        return "System & Utils";
    }

    @Override
    protected void buildContent() {
        buildRestartHelperSection();
        buildServiceHelperSection();
        buildStreamUtilsSection();
        buildLanguageHelperSection();
        buildUtilsSection();
        buildJPatternsSection();
    }

    /* ------------------------------------------------------------------ */
    private void buildRestartHelperSection() {
        LinearLayout s = addSection("RestartHelper",
                "\u26A0\uFE0F Both methods below actually restart the app process (Runtime.exit(0)) - that's the real, intended behavior.");

        runSafe(addRow(s, "restartToMain(context, sendAction)  (will restart the app now)"), () -> {
            RestartHelper.restartToMain(getApplicationContext(), "DEMO_RESTART");
            return "Restarting...";
        });

        runSafe(addRow(s, "restartToTarget(context, intent)  (will restart straight into MainActivity)"), () -> {
            RestartHelper.restartToTarget(this, new Intent(this, MainActivity.class));
            return "Restarting...";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildServiceHelperSection() {
        LinearLayout s = addSection("ServiceHelper", "Checks whether a given started Service is currently running.");

        runSafe(addRow(s, "startService(DemoService)  (setup for the check below)"), () -> {
            startService(new Intent(this, DemoService.class));
            return "DemoService start requested";
        });

        runSafe(addRow(s, "isServiceRunning(context, DemoService.class)"), () ->
                String.valueOf(ServiceHelper.isServiceRunning(this, DemoService.class)));

        runSafe(addRow(s, "stopService(DemoService)  (then re-check above should be false)"), () -> {
            stopService(new Intent(this, DemoService.class));
            return "DemoService stop requested";
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildStreamUtilsSection() {
        LinearLayout s = addSection("StreamUtils", "Reads an InputStream fully into a String.");

        EditText textInput = addInput(s, "Text to wrap as an InputStream", "line one\nline two\nline three");

        runSafe(addRow(s, "streamToString(inputStream)"), () -> {
            ByteArrayInputStream stream = new ByteArrayInputStream(textInput.getText().toString().getBytes(StandardCharsets.UTF_8));
            return StreamUtils.streamToString(stream);
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildLanguageHelperSection() {
        LinearLayout s = addSection("LanguageHelper", "Reads/updates the device and in-app locale.");

        EditText langInput = addInput(s, "Language code to switch to (ar, fr, en...)", "fr");

        runSafe(addRow(s, "getSystemLanguage()"), LanguageHelper::getSystemLanguage);
        runSafe(addRow(s, "getAppLanguage()"), LanguageHelper::getAppLanguage);

        runSafe(addRow(s, "updateAppLanguage(context, lang_code)"), () -> {
            LanguageHelper.updateAppLanguage(this, langInput.getText().toString());
            return "Switched -> getAppLanguage() now = " + LanguageHelper.getAppLanguage();
        });
    }

    /* ------------------------------------------------------------------ */
    private void buildUtilsSection() {
        LinearLayout s = addSection("Utils", "General-purpose helpers.");

        runSafe(addRow(s, "isDebuggingMode(context)"), () -> String.valueOf(Utils.isDebuggingMode(this)));
    }

    /* ------------------------------------------------------------------ */
    private void buildJPatternsSection() {
        LinearLayout s = addSection("JPatterns", "Reusable compiled regex Patterns (IP, domain, URL, phone) plus small Matcher helpers.");

        EditText ipInput = addInput(s, "Text to test against IP_ADDRESS", "10.0.0.5");
        EditText urlInput = addInput(s, "Text to test against WEB_URL", "https://sub.example.com/page?x=1");
        EditText phoneInput = addInput(s, "Text to test against PHONE", "+1 (555) 123-4567");
        EditText ipPortInput = addInput(s, "Text to test against _IP_WITH_PORT", "192.168.1.10:8080");

        runSafe(addRow(s, "IP_ADDRESS.matcher(text).matches()"), () -> String.valueOf(JPatterns.IP_ADDRESS.matcher(ipInput.getText().toString()).matches()));
        runSafe(addRow(s, "DOMAIN_NAME.matcher(text).matches()"), () -> String.valueOf(JPatterns.DOMAIN_NAME.matcher("example.com").matches()));
        runSafe(addRow(s, "WEB_URL.matcher(text).matches()"), () -> String.valueOf(JPatterns.WEB_URL.matcher(urlInput.getText().toString()).matches()));
        runSafe(addRow(s, "_IP_WITH_PORT.matcher(text).matches()  (custom add-on pattern)"), () -> String.valueOf(JPatterns._IP_WITH_PORT.matcher(ipPortInput.getText().toString()).matches()));
        runSafe(addRow(s, "_DOMAIN_WITH_PORT.matcher(text).find() -> group()  (custom add-on pattern)"), () -> {
            Matcher m = JPatterns._DOMAIN_WITH_PORT.matcher(urlInput.getText().toString());
            return m.find() ? m.group() : "(no match)";
        });

        runSafe(addRow(s, "PHONE.matcher(text).matches() then concatGroups(matcher) / digitsAndPlusOnly(matcher)"), () -> {
            Matcher m = JPatterns.PHONE.matcher(phoneInput.getText().toString());
            if (!m.matches()) return "PHONE pattern did not match this input";
            return "concatGroups=" + JPatterns.concatGroups(m) + ", digitsAndPlusOnly=" + JPatterns.digitsAndPlusOnly(m);
        });
    }
}
