package com.iorgana.droidhelpers_project.ui.model;

/**
 * HelperBox
 * -----------------------------------------------------------------------------
 * Data model for a single "box" shown on the MainActivity index (RecyclerView).
 * header  -> package/category title, e.g. "Storage"
 * body    -> the classes that category wraps, e.g. "SqlPreferences, SimpleDB, SecurePreferences"
 * target  -> the demo Activity opened by the footer button
 */
public class HelperBox {
    private final String header;
    private final String body;
    private final Class<?> target;

    public HelperBox(String header, String body, Class<?> target) {
        this.header = header;
        this.body = body;
        this.target = target;
    }

    public String getHeader() {
        return header;
    }

    public String getBody() {
        return body;
    }

    public Class<?> getTarget() {
        return target;
    }
}