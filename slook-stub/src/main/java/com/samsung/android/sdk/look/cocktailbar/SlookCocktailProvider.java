package com.samsung.android.sdk.look.cocktailbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Compile-time stub only. The real class is provided by Samsung's firmware
 * (declared with &lt;uses-library com.samsung.android.sdk.look&gt;) and is never packaged in the APK.
 */
public class SlookCocktailProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
    }

    public void onUpdate(Context context, SlookCocktailManager manager, int[] cocktailIds) {
    }

    public void onEnabled(Context context) {
    }

    public void onDisabled(Context context) {
    }

    public void onVisibilityChanged(Context context, int cocktailId, int visibility) {
    }
}
