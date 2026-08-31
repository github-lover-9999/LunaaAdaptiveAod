package dev.lunaa.aod;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/** SystemUI-owned full-screen black layer used to tame the physically full AOD-HBM level. */
public final class ExtraBrightnessDimLayer {
    private static final String TAG = "LunaaAOD";
    static final int TYPE_NAVIGATION_BAR_PANEL = 2024;

    private final Context context;
    private WindowManager windowManager;
    private View view;
    private WindowManager.LayoutParams layoutParams;
    private boolean attached;

    public ExtraBrightnessDimLayer(Context context) {
        this.context = context;
    }

    public boolean show(int percent) {
        int safePercent = ExtraBrightnessLevel.normalize(percent);
        float alpha = ExtraBrightnessLevel.overlayAlphaForPercent(safePercent);
        boolean wasAttached = attached;
        float previousAlpha = layoutParams == null
                ? ExtraBrightnessLevel.overlayAlphaForPercent(ExtraBrightnessLevel.DEFAULT_PERCENT)
                : layoutParams.alpha;
        try {
            ensureCreated();
            if (windowManager == null || view == null || layoutParams == null) return false;
            layoutParams.alpha = alpha;
            if (!attached) {
                windowManager.addView(view, layoutParams);
                attached = true;
            } else {
                windowManager.updateViewLayout(view, layoutParams);
            }
            Log.i(TAG, "extraBright dimLayer=" + safePercent + "% alpha=" + alpha);
            return true;
        } catch (Throwable t) {
            if (wasAttached) {
                if (layoutParams != null) layoutParams.alpha = previousAlpha;
                Log.w(TAG, "Extra Bright dim-layer update failed; keeping existing protective layer", t);
                return true;
            }
            removeIfPresent("after initial attach failure");
            Log.w(TAG, "Extra Bright dim layer unavailable; HBM will not be enabled", t);
            return false;
        }
    }

    public void hide() {
        removeIfPresent("hide");
    }

    private void removeIfPresent(String reason) {
        if (windowManager == null || view == null) {
            attached = false;
            return;
        }
        try {
            if (attached || view.getParent() != null || view.isAttachedToWindow()) {
                windowManager.removeViewImmediate(view);
            }
        } catch (Throwable t) {
            if (attached) Log.w(TAG, "Failed to remove Extra Bright dim layer: " + reason, t);
        } finally {
            attached = false;
        }
    }

    public boolean isAttached() {
        return attached;
    }

    private void ensureCreated() {
        if (windowManager != null && view != null && layoutParams != null) return;
        if (context == null) throw new IllegalStateException("SystemUI context unavailable");
        Object service = context.getSystemService(Context.WINDOW_SERVICE);
        if (!(service instanceof WindowManager)) {
            throw new IllegalStateException("WindowManager unavailable");
        }
        windowManager = (WindowManager) service;
        view = new View(context);
        view.setBackgroundColor(Color.rgb(0, 0, 0));

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        layoutParams = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                TYPE_NAVIGATION_BAR_PANEL,
                flags,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        layoutParams.alpha = ExtraBrightnessLevel.overlayAlphaForPercent(ExtraBrightnessLevel.DEFAULT_PERCENT);
        layoutParams.setTitle("Lunaa Extra Bright Dim Layer");
    }
}
