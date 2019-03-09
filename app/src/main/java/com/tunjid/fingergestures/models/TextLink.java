package com.tunjid.fingergestures.models;

import androidx.annotation.NonNull;

public class TextLink implements CharSequence {

    private final CharSequence text;
    public final String link;

    public TextLink(CharSequence text, String link) {
        this.text = text;
        this.link = link;
    }

    @Override
    @NonNull
    public String toString() {
        return text.toString();
    }

    @Override
    public int length() {
        return text.length();
    }

    @Override
    public char charAt(int index) {
        return text.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return text.subSequence(start, end);
    }
}
