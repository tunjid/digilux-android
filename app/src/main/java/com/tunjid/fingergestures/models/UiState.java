package com.tunjid.fingergestures.models;

import android.content.Context;

import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.GlyphState;
import com.tunjid.fingergestures.R;

import java.util.Objects;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import static androidx.core.content.ContextCompat.getDrawable;
import static com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.newState;
import static com.tunjid.fingergestures.App.transformApp;

public class UiState {

    public final boolean fabVisible;
    public final GlyphState glyphState;

    public UiState(Context context) {
        this.fabVisible = false;
        this.glyphState = newState(context.getText(R.string.enable_accessibility), getDrawable(context, R.drawable.ic_human_24dp));
    }

    private UiState(boolean fabVisible, GlyphState glyphState) {
        this.fabVisible = fabVisible;
        this.glyphState = glyphState;
    }

    public UiState visibility(boolean fabVisible) {
        return new UiState(fabVisible, this.glyphState);
    }

    public UiState glyph(@StringRes int text, @DrawableRes int icon) {
        GlyphState glyphState = transformApp(app -> newState(app.getText(text), getDrawable(app, icon)), this.glyphState);
        return new UiState(fabVisible, glyphState);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UiState uiState = (UiState) o;
        return fabVisible == uiState.fabVisible &&
                Objects.equals(glyphState, uiState.glyphState);
    }

    @Override public int hashCode() {
        return Objects.hash(fabVisible, glyphState);
    }
}
