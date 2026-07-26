package com.iorgana.droidhelpers_project.ui.demo;

import android.widget.LinearLayout;
import android.widget.TextView;

import com.iorgana.droidhelpers.alerts.AlertMaker;
import com.iorgana.droidhelpers_project.ui.base.BaseDemoActivity;

/**
 * AlertsActivity
 * -----------------------------------------------------------------------------
 * Live usage examples for com.iorgana.droidhelpers.alerts package: AlertMaker.
 * AlertMaker.build() returns a LinearLayout meant to be inserted into your own
 * layout - this screen shows exactly that, using a dedicated preview area.
 */
public class AlertsActivity extends BaseDemoActivity {

    private LinearLayout previewContainer;

    @Override
    protected String getScreenTitle() {
        return "UI Alerts";
    }

    @Override
    protected void buildContent() {
        LinearLayout s = addSection("AlertMaker",
                "Builder for styled, dismissible alert banners (Bootstrap-like types). build() returns a LinearLayout you insert anywhere.");

        TextView previewLabel = new TextView(this);
        previewLabel.setText("Preview area (tap a type below):");
        previewLabel.setAlpha(0.7f);
        s.addView(previewLabel);

        previewContainer = new LinearLayout(this);
        previewContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        params.bottomMargin = dp(6);
        previewContainer.setLayoutParams(params);
        s.addView(previewContainer);

        AlertMaker.AlertType[] types = new AlertMaker.AlertType[]{
                AlertMaker.AlertType.Primary, AlertMaker.AlertType.Secondary, AlertMaker.AlertType.Info,
                AlertMaker.AlertType.Success, AlertMaker.AlertType.Warning, AlertMaker.AlertType.Danger,
                AlertMaker.AlertType.DEFAULT
        };
        for (AlertMaker.AlertType type : types) {
            Row row = addRow(s, "setType(" + type + ").setTitle().setContent().setCancelable(true).build()");
            row.button.setOnClickListener(v -> showAlert(type, row));
        }

        Row marginsRow = addRow(s, "setMargins(top,bottom,start,end) / setMarginTop() / setMarginBottom()");
        marginsRow.button.setOnClickListener(v -> {
            previewContainer.removeAllViews();
            LinearLayout alert = new AlertMaker(this)
                    .setType(AlertMaker.AlertType.Info)
                    .setTitle("Custom margins")
                    .setContent("This alert was built with setMarginTop(24) and setMarginBottom(24).")
                    .setMarginTop(dp(24))
                    .setMarginBottom(dp(24))
                    .setCancelable(true)
                    .build();
            previewContainer.addView(alert);
            marginsRow.output.setText("Alert added with custom top/bottom margins");
        });

        runSafe(addRow(s, "getLastError()  (null unless build() failed internally)"), () ->
                String.valueOf(new AlertMaker(this).getLastError()));

        Row htmlRow = addRow(s, "AlertType.HTML  (currently disabled - deprecated in this release, see class docs)");
        htmlRow.button.setOnClickListener(v -> htmlRow.output.setText(
                "HTML alert type is present in the enum but build() falls back to the normal layout - HtmlFormatter was removed due to bugs."));
    }

    private void showAlert(AlertMaker.AlertType type, Row row) {
        previewContainer.removeAllViews();
        LinearLayout alert = new AlertMaker(this)
                .setType(type)
                .setTitle(type + " alert")
                .setContent("This is a live " + type + " alert built with AlertMaker.")
                .setCancelable(true)
                .setListener(() -> row.output.setText("Closed via the (x) button - onClose() fired"))
                .build();
        previewContainer.addView(alert);
        row.output.setText("Alert added to preview area above");
    }
}