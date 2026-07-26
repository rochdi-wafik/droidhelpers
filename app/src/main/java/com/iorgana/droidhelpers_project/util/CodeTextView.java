package com.iorgana.droidhelpers_project.util;

import android.content.Context;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * A TextView that transparently runs any text passed to setText() through
 * {@link CodeHighlighter#highlightAuto}. Used for the demo screens' live
 * output line so the ~30 existing "row.output.setText(...)" call sites
 * across the demo Activities get JSON highlighting for free, with no
 * per-call-site changes needed.
 */
public class CodeTextView extends AppCompatTextView {

    public CodeTextView(Context context) {
        super(context);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        CharSequence highlighted = CodeHighlighter.highlightAuto(getContext(), text == null ? "" : text.toString());
        super.setText(highlighted, type);
    }
}