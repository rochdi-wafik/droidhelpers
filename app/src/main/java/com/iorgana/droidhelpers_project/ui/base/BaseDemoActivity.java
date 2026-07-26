package com.iorgana.droidhelpers_project.ui.base;

import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.iorgana.droidhelpers_project.util.EdgeToEdgeUtils;

/**
 * BaseDemoActivity
 * -----------------------------------------------------------------------------
 * Shared scaffolding for every droidhelpers demo screen so each package's
 * Activity only has to describe "which classes, which methods" and not
 * rebuild the same Material layout code 9 times (Simplicity First).
 * -----------------------------------------------------------------------------
 * Layout produced: ScrollView > vertical LinearLayout (the "root"), into which
 * subclasses add one MaterialCardView "section" per wrapped class, and one
 * Row (label + Run button + live output) per public method demonstrated.
 */
public abstract class BaseDemoActivity extends AppCompatActivity {

    /** A demo action that performs a real droidhelpers call and returns text to display. */
    public interface DemoAction {
        String run() throws Exception;
    }

    /** One method-demo row: a Run button + the output line beneath it. */
    public static class Row {
        public final MaterialButton button;
        public final TextView output;

        Row(MaterialButton button, TextView output) {
            this.button = button;
            this.output = output;
        }
    }

    protected LinearLayout root;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, dp(32));
        root.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(root);
        setContentView(scrollView);
        // Fix for Android 16
        EdgeToEdgeUtils.applyBarInsets(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(getScreenTitle());

        buildContent();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Screen title shown in the ActionBar, e.g. "Storage". */
    protected abstract String getScreenTitle();

    /** Subclasses build their sections/rows here (called once from onCreate). */
    protected abstract void buildContent();

    /* ------------------------------------------------------------------ */
    /*  Builder helpers                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Adds a MaterialCardView section for one wrapped class (e.g. "SqlPreferences")
     * and returns the inner vertical LinearLayout to which rows/inputs should be added.
     */
    protected LinearLayout addSection(String className, String note) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.topMargin = dp(8);
        cardParams.bottomMargin = dp(8);
        card.setLayoutParams(cardParams);
        card.setRadius(dp(14));
        card.setCardElevation(dp(2));
        card.setStrokeWidth(0);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        int pad = dp(14);
        body.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(className);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        body.addView(title);

        if (note != null) {
            TextView subtitle = new TextView(this);
            subtitle.setText(note);
            subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            subtitle.setAlpha(0.65f);
            LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            subParams.topMargin = dp(2);
            subParams.bottomMargin = dp(6);
            subtitle.setLayoutParams(subParams);
            body.addView(subtitle);
        }

        card.addView(body);
        root.addView(card);
        return body;
    }

    /**
     * Adds an editable input field pre-filled with a sane default, so every
     * demo is tap-and-go without requiring the user to type anything first.
     */
    protected EditText addInput(LinearLayout section, String hint, String prefill) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setText(prefill);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setSingleLine(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(6);
        editText.setLayoutParams(params);
        section.addView(editText);
        return editText;
    }

    /**
     * Adds one method-demo row: a small label, a "Run" button, and an output
     * line. Returns the Row so callers can wire up sync or async logic.
     */
    protected Row addRow(LinearLayout section, String methodLabel) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.topMargin = dp(10);
        row.setLayoutParams(rowParams);

        TextView label = new TextView(this);
        label.setText(com.iorgana.droidhelpers_project.util.CodeHighlighter.highlightJavaSignature(this, methodLabel));
        styleAsCode(label);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.rightMargin = dp(8);
        label.setLayoutParams(labelParams);

        MaterialButton button = new MaterialButton(this);
        button.setText("Run");
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, com.iorgana.droidhelpers_project.R.color.google_blue)));
        button.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.iorgana.droidhelpers_project.R.color.white));

        row.addView(label);
        row.addView(button);
        section.addView(row);

        TextView output = new com.iorgana.droidhelpers_project.util.CodeTextView(this);
        styleAsCode(output);
        LinearLayout.LayoutParams outParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        outParams.topMargin = dp(6);
        outParams.bottomMargin = dp(4);
        output.setLayoutParams(outParams);
        section.addView(output);

        return new Row(button, output);
    }

    /**
     * Convenience for the common synchronous case: wires the row's button to
     * run `action` and display either its result or the caught exception.
     */
    protected void runSafe(Row row, DemoAction action) {
        row.button.setOnClickListener(v -> {
            try {
                row.output.setText(String.valueOf(action.run()));
            } catch (Throwable e) {
                row.output.setText("Error: " + e.getMessage());
            }
        });
    }

    /**
     * Applies the shared "code block" look (monospace font, code-tinted
     * text color, padded rounded background) to a method-signature label
     * or a live output line, so both read as code instead of plain body
     * text. Day/night colors come from {@code @color/code_block_*}.
     */
    protected void styleAsCode(TextView view) {
        view.setTypeface(android.graphics.Typeface.MONOSPACE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setTextColor(androidx.core.content.ContextCompat.getColor(this, com.iorgana.droidhelpers_project.R.color.code_block_text));
        view.setBackgroundResource(com.iorgana.droidhelpers_project.R.drawable.bg_code_block);
        int hPad = dp(10), vPad = dp(8);
        view.setPadding(hPad, vPad, hPad, vPad);
    }

    protected void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    protected int dp(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}