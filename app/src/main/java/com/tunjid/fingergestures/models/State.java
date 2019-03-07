package com.tunjid.fingergestures.models;

import android.content.Context;

import com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.GlyphState;
import com.tunjid.fingergestures.R;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import static androidx.core.content.ContextCompat.getDrawable;
import static com.tunjid.androidbootstrap.material.animator.FabExtensionAnimator.newState;
import static com.tunjid.fingergestures.App.transformApp;

public class State {

    public final boolean fabVisible;
    public final GlyphState glyphState;

    public State(Context context) {
        this.fabVisible = false;
        this.glyphState = newState(context.getText(R.string.enable_accessibility), getDrawable(context, R.drawable.ic_human_24dp));
    }

    private State(boolean fabVisible, GlyphState glyphState) {
        this.fabVisible = fabVisible;
        this.glyphState = glyphState;
    }

    public State visibility(boolean fabVisible) {
        return new State(fabVisible, this.glyphState);
    }

    public State glyph(@StringRes int text, @DrawableRes int icon) {
        GlyphState glyphState = transformApp(app -> newState(app.getText(text), getDrawable(app, icon)), this.glyphState);
        return new State(fabVisible, glyphState);
    }
}
