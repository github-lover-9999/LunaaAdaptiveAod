# Shared UI components

This project is a native Android Java application without an external UI component library. `SettingsActivity` currently creates all controls programmatically with platform widgets. There are no standalone shared UI primitives yet.

## Current UI implementation
- File: `app/src/main/java/dev/lunaa/aod/SettingsActivity.java`
- Components: `TextView`, `Switch`, `SeekBar`, `Button`, `EditText`, `LinearLayout`, `ScrollView`

```java
package dev.lunaa.aod;

import android.app.Activity;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

public final class SettingsActivity extends Activity implements SensorEventListener {
    private static final int MULTIPLIER_STEP = 5;
    private static final int SEEK_MAX =
            (AodSettingsSnapshot.MAX_MULTIPLIER_PERCENT
                    - AodSettingsSnapshot.MIN_MULTIPLIER_PERCENT) / MULTIPLIER_STEP;

    private AndroidSettingsStore settingsStore;
    private SensorManager sensorManager;
    private Sensor lightSensor;

    private Switch enabledSwitch;
    private SeekBar multiplierSeekBar;
    private TextView multiplierValue;
    private TextView ambientValue;
    private TextView previewValue;
    private TextView statusValue;
    private TextView warningValue;
    private Button saveButton;

    private final EditText[] luxFields = new EditText[AodSettingsSnapshot.POINT_COUNT];
    private final EditText[] brightnessFields = new EditText[AodSettingsSnapshot.POINT_COUNT];

    private float currentLux = Float.NaN;
    private int currentRevision;
    private boolean updatingForm;

    private final TextWatcher draftWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            if (!updatingForm) updatePreview();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsStore = new AndroidSettingsStore(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;

        setContentView(buildContent());

        AodSettingsSnapshot saved = settingsStore.load();
        currentRevision = saved.getRevision();
        applySnapshot(saved, true);

        if (!settingsStore.isWritableForXposed()) {
            saveButton.setEnabled(false);
            statusValue.setText(R.string.shared_prefs_unavailable);
        }
        updatePreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else if (ambientValue != null) {
            ambientValue.setText(R.string.light_sensor_unavailable);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length == 0) return;
        currentLux = Math.max(0f, event.values[0]);
        if (ambientValue != null) {
            ambientValue.setText(String.format(Locale.US, "Ambient light: %.1f lux", currentLux));
        }
        updatePreview();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, dp(36));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text(getString(R.string.app_name), 26f, true);
        root.addView(title);

        TextView intro = text(getString(R.string.settings_intro), 14f, false);
        intro.setPadding(0, dp(8), 0, dp(16));
        root.addView(intro);

        enabledSwitch = new Switch(this);
        enabledSwitch.setText(R.string.adaptive_aod);
        enabledSwitch.setTextSize(17f);
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> updatePreview());
        root.addView(enabledSwitch, fullWidth());

        statusValue = text(getString(R.string.apply_next_cycle), 13f, false);
        statusValue.setPadding(0, dp(8), 0, dp(12));
        root.addView(statusValue);

        ambientValue = text(getString(R.string.ambient_waiting), 15f, false);
        root.addView(ambientValue);
        previewValue = text(getString(R.string.preview_waiting), 15f, true);
        previewValue.setPadding(0, dp(4), 0, dp(20));
        root.addView(previewValue);

        root.addView(sectionTitle(getString(R.string.overall_brightness)));
        LinearLayout multiplierRow = horizontal();
        multiplierSeekBar = new SeekBar(this);
        multiplierSeekBar.setMax(SEEK_MAX);
        multiplierSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateMultiplierLabel();
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        multiplierRow.addView(multiplierSeekBar, new LinearLayout.LayoutParams(0, dp(48), 1f));
        multiplierValue = text("100%", 16f, true);
        multiplierValue.setMinWidth(dp(64));
        multiplierRow.addView(multiplierValue, wrap());
        root.addView(multiplierRow, fullWidth());

        TextView multiplierHelp = text(getString(R.string.multiplier_help), 13f, false);
        multiplierHelp.setPadding(0, 0, 0, dp(14));
        root.addView(multiplierHelp);

        LinearLayout presets = horizontal();
        presets.addView(presetButton(getString(R.string.preset_dim), AodSettingsDefaults.dim()), weighted());
        presets.addView(presetButton(getString(R.string.preset_balanced), AodSettingsDefaults.balanced()), weighted());
        presets.addView(presetButton(getString(R.string.preset_bright), AodSettingsDefaults.bright()), weighted());
        root.addView(presets, fullWidth());

        root.addView(sectionTitle(getString(R.string.advanced_curve)));
        TextView curveHelp = text(getString(R.string.curve_help), 13f, false);
        curveHelp.setPadding(0, 0, 0, dp(8));
        root.addView(curveHelp);

        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            root.addView(buildCurveRow(i), fullWidth());
        }

        warningValue = text("", 13f, true);
        warningValue.setPadding(0, dp(8), 0, dp(8));
        root.addView(warningValue);

        LinearLayout actions = horizontal();
        Button resetButton = new Button(this);
        resetButton.setText(R.string.reset);
        resetButton.setOnClickListener(v -> {
            currentRevision = settingsStore.load().getRevision();
            applySnapshot(AodSettingsDefaults.balanced(), true);
            statusValue.setText(R.string.reset_not_saved);
        });
        actions.addView(resetButton, weighted());

        saveButton = new Button(this);
        saveButton.setText(R.string.save);
        saveButton.setOnClickListener(v -> saveForm());
        actions.addView(saveButton, weighted());
        root.addView(actions, fullWidth());

        return scrollView;
    }

    private View buildCurveRow(int index) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setPadding(0, dp(8), 0, dp(8));

        TextView label = text(
                "Point " + (index + 1) + " — " + AodSettingsDefaults.hintAt(index),
                13f,
                true
        );
        block.addView(label);

        LinearLayout fields = horizontal();
        EditText lux = new EditText(this);
        lux.setHint(R.string.lux_hint);
        lux.setSingleLine(true);
        lux.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        lux.addTextChangedListener(draftWatcher);
        luxFields[index] = lux;
        fields.addView(lux, weighted());

        EditText brightness = new EditText(this);
        brightness.setHint(R.string.brightness_hint);
        brightness.setSingleLine(true);
        brightness.setInputType(InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        brightness.addTextChangedListener(draftWatcher);
        brightnessFields[index] = brightness;
        fields.addView(brightness, weighted());

        block.addView(fields, fullWidth());
        return block;
    }

    private Button presetButton(String label, AodSettingsSnapshot preset) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(v -> applyPreset(preset));
        return button;
    }

    private void applyPreset(AodSettingsSnapshot preset) {
        updatingForm = true;
        try {
            for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
                luxFields[i].setText(formatNumber(preset.luxAt(i)));
                brightnessFields[i].setText(formatBrightness(preset.brightnessAt(i)));
                luxFields[i].setError(null);
                brightnessFields[i].setError(null);
            }
        } finally {
            updatingForm = false;
        }
        statusValue.setText(R.string.preset_not_saved);
        updatePreview();
    }

    private void applySnapshot(AodSettingsSnapshot snapshot, boolean includeMultiplierAndEnabled) {
        updatingForm = true;
        try {
            if (includeMultiplierAndEnabled) {
                enabledSwitch.setChecked(snapshot.isEnabled());
                int progress = (snapshot.getMultiplierPercent()
                        - AodSettingsSnapshot.MIN_MULTIPLIER_PERCENT) / MULTIPLIER_STEP;
                multiplierSeekBar.setProgress(Math.max(0, Math.min(SEEK_MAX, progress)));
            }
            for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
                luxFields[i].setText(formatNumber(snapshot.luxAt(i)));
                brightnessFields[i].setText(formatBrightness(snapshot.brightnessAt(i)));
                luxFields[i].setError(null);
                brightnessFields[i].setError(null);
            }
        } finally {
            updatingForm = false;
        }
        updateMultiplierLabel();
        updatePreview();
    }

    private void saveForm() {
        if (!settingsStore.isWritableForXposed()) {
            statusValue.setText(R.string.shared_prefs_unavailable);
            return;
        }
        AodSettingsSnapshot draft = readDraft(true, currentRevision + 1);
        if (draft == null) {
            statusValue.setText(R.string.fix_curve_errors);
            return;
        }
        if (settingsStore.save(draft)) {
            currentRevision = draft.getRevision();
            statusValue.setText("Saved revision " + currentRevision
                    + ". Changes apply on the next AOD activation.");
        } else {
            statusValue.setText(R.string.save_failed);
        }
        updatePreview();
    }

    private AodSettingsSnapshot readDraft(boolean showErrors, int revision) {
        float[] lux = new float[AodSettingsSnapshot.POINT_COUNT];
        float[] brightness = new float[AodSettingsSnapshot.POINT_COUNT];
        boolean valid = true;
        float previousLux = -1f;
        boolean highBrightness = false;

        for (int i = 0; i < AodSettingsSnapshot.POINT_COUNT; i++) {
            if (showErrors) {
                luxFields[i].setError(null);
                brightnessFields[i].setError(null);
            }

            Float luxValue = parseFinite(luxFields[i].getText().toString());
            if (luxValue == null || luxValue < 0f || (i > 0 && luxValue <= previousLux)) {
                valid = false;
                if (showErrors) {
                    luxFields[i].setError(i == 0
                            ? getString(R.string.invalid_lux)
                            : getString(R.string.lux_must_increase));
                }
            } else {
                lux[i] = luxValue;
                previousLux = luxValue;
            }

            Float brightnessValue = parseFinite(brightnessFields[i].getText().toString());
            if (brightnessValue == null
                    || brightnessValue < AodSettingsSnapshot.MIN_BRIGHTNESS
                    || brightnessValue > AodSettingsSnapshot.MAX_BRIGHTNESS) {
                valid = false;
                if (showErrors) brightnessFields[i].setError(getString(R.string.invalid_brightness));
            } else {
                brightness[i] = brightnessValue;
                highBrightness |= brightnessValue > 0.50f;
            }
        }

        warningValue.setText(highBrightness ? R.string.high_brightness_warning : R.string.empty);
        if (!valid) return null;

        try {
            return new AodSettingsSnapshot(
                    enabledSwitch.isChecked(),
                    multiplierPercent(),
                    lux,
                    brightness,
                    Math.max(0, revision)
            );
        } catch (IllegalArgumentException invalid) {
            if (showErrors) warningValue.setText(invalid.getMessage());
            return null;
        }
    }

    private void updatePreview() {
        if (previewValue == null || multiplierSeekBar == null || warningValue == null) return;
        updateMultiplierLabel();
        AodSettingsSnapshot draft = readDraft(false, currentRevision);
        if (draft == null) {
            previewValue.setText(R.string.preview_invalid);
            return;
        }
        if (Float.isNaN(currentLux)) {
            previewValue.setText(R.string.preview_waiting);
            return;
        }
        float target = BrightnessCurve.targetForLux(currentLux, draft);
        String suffix = draft.isEnabled() ? "" : " (adaptive disabled)";
        previewValue.setText(String.format(
                Locale.US,
                "Calculated AOD brightness: %.3f%s",
                target,
                suffix
        ));
    }

    private int multiplierPercent() {
        return AodSettingsSnapshot.MIN_MULTIPLIER_PERCENT
                + multiplierSeekBar.getProgress() * MULTIPLIER_STEP;
    }

    private void updateMultiplierLabel() {
        if (multiplierValue != null && multiplierSeekBar != null) {
            multiplierValue.setText(multiplierPercent() + "%");
        }
    }

    private static Float parseFinite(String raw) {
        if (raw == null) return null;
        try {
            float value = Float.parseFloat(raw.trim().replace(',', '.'));
            return Float.isFinite(value) ? value : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatNumber(float value) {
        if (value == Math.round(value)) return Integer.toString(Math.round(value));
        return String.format(Locale.US, "%.2f", value);
    }

    private static String formatBrightness(float value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private TextView sectionTitle(String value) {
        TextView view = text(value, 19f, true);
        view.setPadding(0, dp(18), 0, dp(6));
        return view;
    }

    private TextView text(String value, float sizeSp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

```
