package com.iorgana.droidhelpers_project.util;

import android.content.Context;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import com.iorgana.droidhelpers_project.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CodeHighlighter
 * -----------------------------------------------------------------------------
 * Minimal, dependency-free syntax highlighter for the two content types the
 * demo screens actually display: Java method signatures (labels) and JSON
 * response bodies (live output). Deliberately NOT a general-purpose code
 * editor/highlighter library — every off-the-shelf native option is
 * unmaintained (last updated 2018-2022), and this project's own
 * PROJECT_MAP.md already commits to zero new third-party dependencies for
 * the demo app. A ~100-line regex-based Spannable builder covers the actual
 * need with no external dependency and no WebView.
 * -----------------------------------------------------------------------------
 * Usage:
 *   textView.setText(CodeHighlighter.highlightJavaSignature(context, "getIPAddress(boolean useIPv4)"));
 *   textView.setText(CodeHighlighter.highlightAuto(context, jsonOrPlainOutput));
 */
public final class CodeHighlighter {

    private CodeHighlighter() {}

    /* ------------------------------------------------------------------ */
    /*  Java method-signature highlighting (used for the row labels)       */
    /* ------------------------------------------------------------------ */

    private static final Pattern JAVA_TOKEN = Pattern.compile(
            "(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\")"
                    + "|(?<NUMBER>\\b\\d+(?:\\.\\d+)?\\b)"
                    + "|(?<KEYWORD>\\b(?:public|private|protected|static|final|void|new|class|interface|"
                    + "extends|implements|throws|return|synchronized|abstract|native|this|super|"
                    + "null|true|false|int|long|short|byte|char|boolean|float|double)\\b)"
                    + "|(?<METHOD>\\b[a-zA-Z_][a-zA-Z0-9_]*(?=\\s*\\())"
                    + "|(?<TYPE>\\b[A-Z][a-zA-Z0-9_]*\\b)"
                    + "|(?<PUNCT>[(){}\\[\\],.;<>])"
    );

    public static SpannableString highlightJavaSignature(Context context, String signature) {
        SpannableString spannable = new SpannableString(signature);
        Matcher m = JAVA_TOKEN.matcher(signature);
        while (m.find()) {
            int color;
            if (m.group("STRING") != null) color = R.color.hl_string;
            else if (m.group("NUMBER") != null) color = R.color.hl_number;
            else if (m.group("KEYWORD") != null) color = R.color.hl_keyword;
            else if (m.group("METHOD") != null) color = R.color.hl_method;
            else if (m.group("TYPE") != null) color = R.color.hl_type;
            else color = R.color.hl_punctuation; // PUNCT
            applySpan(context, spannable, m.start(), m.end(), color);
        }
        return spannable;
    }

    /* ------------------------------------------------------------------ */
    /*  JSON highlighting (used for the live output line)                  */
    /* ------------------------------------------------------------------ */

    private static final Pattern JSON_TOKEN = Pattern.compile(
            "(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\")"
                    + "|(?<KEYWORD>\\btrue\\b|\\bfalse\\b|\\bnull\\b)"
                    + "|(?<NUMBER>-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)"
                    + "|(?<PUNCT>[{}\\[\\],:])"
    );

    public static SpannableString highlightJson(Context context, String json) {
        SpannableString spannable = new SpannableString(json);
        Matcher m = JSON_TOKEN.matcher(json);
        while (m.find()) {
            int color;
            if (m.group("STRING") != null) {
                // A quoted string followed by ':' is a JSON key; otherwise it's a value.
                color = isFollowedByColon(json, m.end()) ? R.color.hl_type : R.color.hl_string;
            } else if (m.group("KEYWORD") != null) color = R.color.hl_keyword;
            else if (m.group("NUMBER") != null) color = R.color.hl_number;
            else color = R.color.hl_punctuation; // PUNCT
            applySpan(context, spannable, m.start(), m.end(), color);
        }
        return spannable;
    }

    /**
     * Picks JSON highlighting when the text looks like a JSON object/array,
     * otherwise returns the text unhighlighted (still shown in the
     * monospace code block by the caller).
     */
    public static CharSequence highlightAuto(Context context, String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return highlightJson(context, text);
        }
        return text;
    }

    /* ------------------------------------------------------------------ */

    private static boolean isFollowedByColon(String text, int fromIndex) {
        int i = fromIndex;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        return i < text.length() && text.charAt(i) == ':';
    }

    private static void applySpan(Context context, SpannableString spannable, int start, int end, int colorRes) {
        spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, colorRes)),
                start, end, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
}