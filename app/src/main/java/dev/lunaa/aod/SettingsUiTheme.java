package dev.lunaa.aod;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

final class SettingsUiTheme {
    static final int COLOR_BACKGROUND = Color.rgb(7, 8, 9);
    static final int COLOR_SURFACE = Color.rgb(19, 21, 23);
    static final int COLOR_SURFACE_STRONG = Color.rgb(28, 31, 34);
    static final int COLOR_BORDER = Color.rgb(52, 56, 60);
    static final int COLOR_TEXT = Color.rgb(244, 244, 242);
    static final int COLOR_MUTED = Color.rgb(164, 169, 174);
    static final int COLOR_ACCENT = Color.rgb(255, 196, 73);
    static final int COLOR_ACCENT_TEXT = Color.rgb(24, 19, 8);
    static final int COLOR_WARNING = Color.rgb(255, 177, 73);

    private SettingsUiTheme() {}

    static void applyActivityChrome(Activity activity) {
        Window window = activity.getWindow();
        window.getDecorView().setBackgroundColor(COLOR_BACKGROUND);

        if (Build.VERSION.SDK_INT >= 30) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            window.setStatusBarColor(COLOR_BACKGROUND);
            window.setNavigationBarColor(COLOR_BACKGROUND);
        }
    }

    static void styleText(TextView view, boolean muted) {
        view.setTextColor(muted ? COLOR_MUTED : COLOR_TEXT);
    }

    static void styleCard(View view, float density) {
        GradientDrawable background = rounded(COLOR_SURFACE, density, 20f);
        background.setStroke(dp(density, 1f), COLOR_BORDER);
        view.setBackground(background);
    }

    static void styleEditText(EditText view, float density) {
        GradientDrawable background = rounded(COLOR_SURFACE_STRONG, density, 13f);
        background.setStroke(dp(density, 1f), COLOR_BORDER);
        view.setBackground(background);
        view.setTextColor(COLOR_TEXT);
        view.setHintTextColor(COLOR_MUTED);
        view.setPadding(dp(density, 12f), dp(density, 8f), dp(density, 12f), dp(density, 8f));
        view.setMinHeight(dp(density, 48f));
    }

    static void styleButton(Button button, float density, boolean selected) {
        GradientDrawable background = rounded(
                selected ? COLOR_ACCENT : COLOR_SURFACE_STRONG,
                density,
                13f
        );
        background.setStroke(dp(density, 1f), selected ? COLOR_ACCENT : COLOR_BORDER);
        button.setBackground(background);
        button.setTextColor(selected ? COLOR_ACCENT_TEXT : COLOR_TEXT);
        button.setAllCaps(false);
        button.setMinHeight(dp(density, 48f));
        button.setMinimumHeight(dp(density, 48f));
        button.setPadding(dp(density, 10f), dp(density, 8f), dp(density, 10f), dp(density, 8f));
    }

    static void styleDisclosureButton(Button button, float density, boolean expanded) {
        GradientDrawable background = rounded(COLOR_SURFACE_STRONG, density, 13f);
        background.setStroke(dp(density, 1f), expanded ? COLOR_ACCENT : COLOR_BORDER);
        button.setBackground(background);
        button.setTextColor(expanded ? COLOR_ACCENT : COLOR_TEXT);
        button.setAllCaps(false);
        button.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
        button.setMinHeight(dp(density, 48f));
        button.setMinimumHeight(dp(density, 48f));
        button.setPadding(dp(density, 14f), 0, dp(density, 14f), 0);
    }

    static void stylePrimaryButton(Button button, float density) {
        GradientDrawable background = rounded(COLOR_ACCENT, density, 13f);
        button.setBackground(background);
        button.setTextColor(COLOR_ACCENT_TEXT);
        button.setAllCaps(false);
        button.setMinHeight(dp(density, 48f));
        button.setMinimumHeight(dp(density, 48f));
        button.setPadding(dp(density, 10f), dp(density, 8f), dp(density, 10f), dp(density, 8f));
    }

    static void styleSecondaryButton(Button button, float density) {
        styleButton(button, density, false);
    }

    static void styleSeekBar(SeekBar seekBar) {
        ColorStateList accent = ColorStateList.valueOf(COLOR_ACCENT);
        seekBar.setProgressTintList(accent);
        seekBar.setThumbTintList(accent);
        seekBar.setMinHeight(dp(seekBar.getResources().getDisplayMetrics().density, 48f));
        seekBar.setMinimumHeight(dp(seekBar.getResources().getDisplayMetrics().density, 48f));
    }

    static void styleSwitch(Switch toggle) {
        toggle.setTextColor(COLOR_TEXT);
        toggle.setMinHeight(dp(toggle.getResources().getDisplayMetrics().density, 48f));
        toggle.setMinimumHeight(dp(toggle.getResources().getDisplayMetrics().density, 48f));
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(
                states,
                new int[]{COLOR_ACCENT, Color.rgb(128, 132, 136)}
        ));
        toggle.setTrackTintList(new ColorStateList(
                states,
                new int[]{COLOR_ACCENT, Color.rgb(56, 59, 62)}
        ));
    }

    private static GradientDrawable rounded(int color, float density, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusDp * density);
        return drawable;
    }

    private static int dp(float density, float value) {
        return Math.round(value * density);
    }
}
