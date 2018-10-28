package com.tunjid.fingergestures;

import android.content.Context;
import androidx.core.view.ActionProvider;
import android.view.MenuItem;
import android.view.View;

public class TrialViewActionProvider extends ActionProvider {

    public TrialViewActionProvider(Context context) {
        super(context);
    }

    @Override
    public View onCreateActionView() {
        // Won't be called because of our min SDK version
        return null;
    }

    @Override
    public View onCreateActionView(MenuItem menuItem) {
        return new TrialView(getContext(), menuItem);
    }

    @Override
    public boolean onPerformDefaultAction() {
        return super.onPerformDefaultAction();
    }

}
