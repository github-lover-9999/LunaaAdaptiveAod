package dev.lunaa.aod;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;


public final class SettingsActivity extends Activity implements SensorEventListener {
    private static final int CONTENT_HORIZONTAL_PADDING_DP = 18;
    private static final int CONTENT_TOP_PADDING_DP = 32;
    private static final int CONTENT_BOTTOM_PADDING_DP = 24;
    private static final int MANUAL_LEVEL_COUNT = 3;
    private static final int EXTRA_BRIGHT_LEVEL_COUNT = 3;

    private AndroidSettingsStore settingsStore;
    private SensorManager sensorManager;
    private Sensor lightSensor;

    private Switch enabledSwitch;
    private Button automaticModeButton;
    private Button manualModeButton;
    private LinearLayout automaticPanel;
    private LinearLayout manualPanel;
    private final Button[] presetButtons = new Button[3];

    private TextView presetDescription;

    private TextView updateStatusText;
    private TextView updateDetailsText;
    private Button updateActionButton;
    private ProgressBar updateProgressBar;
    private AppUpdater.ReleaseInfo latestRelease;
    private TextView extraBrightnessHint;

    private SeekBar manualBrightnessSeekBar;
    private TextView manualBrightnessValue;

    private LinearLayout extraBrightnessPanel;
    private Switch automaticExtraBrightnessSwitch;
    private Switch manualExtraBrightnessSwitch;
    private SeekBar extraBrightnessLevelSeekBar;
    private TextView extraBrightnessLevelValue;

    private Button advancedSettingsToggle;
    private LinearLayout advancedSettingsContent;
    private final EditText[] manualLevelFields = new EditText[3];
    private final EditText[] extraLevelFields = new EditText[3];
    private TextView advancedValidationValue;

    private TextView masterDetailValue;
    private TextView ambientValue;
    private TextView previewValue;
    private TextView extraPreviewValue;
    private TextView statusValue;
    private TextView warningValue;
    private Button saveButton;

    private float currentLux = Float.NaN;
    private int currentRevision;
    private AodMode currentMode = AodMode.AUTOMATIC;
    private AodPreset currentPreset = AodPreset.BALANCED;
    private final int[] manualLevelPercents = {
            AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_1_PERCENT,
            AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_2_PERCENT,
            AodSettingsDefaults.DEFAULT_MANUAL_LEVEL_3_PERCENT
    };
    private final int[] extraLevelPercents = {
            AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_1_PERCENT,
            AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_2_PERCENT,
            AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_3_PERCENT
    };
    private String savedFormSignature = "";
    private boolean advancedExpanded;
    private boolean updatingForm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsUiTheme.applyActivityChrome(this);
        settingsStore = new AndroidSettingsStore(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;

        setContentView(buildContent());

        AodSettingsSnapshot saved = settingsStore.load();
        currentRevision = saved.getRevision();
        applySnapshot(saved);
        savedFormSignature = formSignature();

        if (!settingsStore.isWritableForXposed()) {
            statusValue.setText(R.string.shared_prefs_unavailable);
        }
        updatePreview();
        updateSaveEnabled();
        triggerUpdateCheck(false);
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
        float lux = event.values[0];
        if (!Float.isFinite(lux) || lux < 0f) return;
        currentLux = lux;
        if (ambientValue != null) {
            ambientValue.setText(getString(
                    R.string.ambient_value,
                    describeAmbientLight(currentLux),
                    currentLux
            ));
        }
        updatePreview();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private View buildContent() {
        LinearLayout screen = new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(SettingsUiTheme.COLOR_BACKGROUND);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int horizontal = dp(CONTENT_HORIZONTAL_PADDING_DP);
        root.setPadding(
                horizontal,
                dp(CONTENT_TOP_PADDING_DP),
                horizontal,
                dp(CONTENT_BOTTOM_PADDING_DP)
        );
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        View actionBar = buildActionBar();
        screen.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));
        screen.addView(actionBar, fullWidth());
        applyWindowInsets(screen, root, actionBar);

        TextView intro = text(getString(R.string.settings_intro), 14f, false, true);
        intro.setPadding(0, dp(7), 0, dp(16));
        root.addView(intro);

        root.addView(buildMasterCard(), spacedFullWidth());
        root.addView(buildModeCard(), spacedFullWidth());

        automaticPanel = buildAutomaticPanel();
        root.addView(automaticPanel, spacedFullWidth());

        manualPanel = buildManualPanel();
        root.addView(manualPanel, spacedFullWidth());

        root.addView(buildLiveCard(), spacedFullWidth());

        extraBrightnessPanel = buildExtraBrightnessPanel();
        root.addView(extraBrightnessPanel, spacedFullWidth());

        root.addView(buildAdvancedSettingsPanel(), spacedFullWidth());
        root.addView(buildUpdateCard(), spacedFullWidth());

        warningValue = text("", 13f, true, false);
        warningValue.setTextColor(SettingsUiTheme.COLOR_WARNING);
        warningValue.setPadding(dp(4), dp(4), dp(4), dp(6));
        root.addView(warningValue, fullWidth());

        return screen;
    }

    private void applyWindowInsets(View screen, LinearLayout root, View actionBar) {
        if (Build.VERSION.SDK_INT < 30) return;

        final int contentHorizontal = dp(CONTENT_HORIZONTAL_PADDING_DP);
        final int contentTop = dp(CONTENT_TOP_PADDING_DP);
        final int contentBottom = dp(CONTENT_BOTTOM_PADDING_DP);
        final int actionHorizontal = dp(14);
        final int actionTop = dp(10);
        final int actionBottom = dp(12);

        screen.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars());
            Insets cutout = windowInsets.getInsets(WindowInsets.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsets.Type.ime());

            int left = Math.max(systemBars.left, cutout.left);
            int right = Math.max(systemBars.right, cutout.right);
            int top = Math.max(systemBars.top, cutout.top);
            int bottom = Math.max(systemBars.bottom, ime.bottom);

            root.setPadding(
                    contentHorizontal + left,
                    contentTop + top,
                    contentHorizontal + right,
                    contentBottom
            );
            actionBar.setPadding(
                    actionHorizontal + left,
                    actionTop,
                    actionHorizontal + right,
                    actionBottom + bottom
            );
            return windowInsets;
        });
        screen.requestApplyInsets();
    }

    private View buildMasterCard() {
        LinearLayout card = card();
        TextView eyebrow = text(getString(R.string.module_status), 12f, true, true);
        card.addView(eyebrow);

        enabledSwitch = new Switch(this);
        enabledSwitch.setText(R.string.adaptive_aod);
        enabledSwitch.setTextSize(18f);
        SettingsUiTheme.styleSwitch(enabledSwitch);
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMasterCopy();
            updatePreview();
        });
        card.addView(enabledSwitch, fullWidth());

        masterDetailValue = text("", 13f, false, true);
        masterDetailValue.setPadding(0, dp(5), 0, 0);
        card.addView(masterDetailValue);

        statusValue = text(getString(R.string.apply_next_cycle), 12f, false, true);
        statusValue.setPadding(0, dp(8), 0, 0);
        card.addView(statusValue);
        return card;
    }

    private View buildModeCard() {
        LinearLayout card = card();
        card.addView(cardTitle(getString(R.string.mode)));
        TextView help = text(getString(R.string.mode_help), 13f, false, true);
        help.setPadding(0, dp(3), 0, dp(10));
        card.addView(help);

        LinearLayout selector = horizontal();
        automaticModeButton = new Button(this);
        automaticModeButton.setText(R.string.mode_automatic);
        automaticModeButton.setOnClickListener(v -> setMode(AodMode.AUTOMATIC));
        selector.addView(automaticModeButton, weighted());

        manualModeButton = new Button(this);
        manualModeButton.setText(R.string.mode_manual);
        manualModeButton.setOnClickListener(v -> setMode(AodMode.MANUAL));
        selector.addView(manualModeButton, weighted());
        card.addView(selector, fullWidth());
        return card;
    }

    private View buildLiveCard() {
        LinearLayout card = card();
        card.addView(cardTitle(getString(R.string.live_status)));

        ambientValue = text(getString(R.string.ambient_waiting), 14f, false, true);
        ambientValue.setPadding(0, dp(7), 0, 0);
        card.addView(ambientValue);

        previewValue = text(getString(R.string.preview_waiting), 18f, true, false);
        previewValue.setPadding(0, dp(5), 0, 0);
        card.addView(previewValue);

        extraPreviewValue = text("", 13f, true, true);
        extraPreviewValue.setPadding(0, dp(5), 0, 0);
        extraPreviewValue.setVisibility(View.GONE);
        card.addView(extraPreviewValue);
        return card;
    }

    private LinearLayout buildAutomaticPanel() {
        LinearLayout panel = card();
        panel.addView(cardTitle(getString(R.string.automatic_settings)));
        TextView help = text("Pick a fixed profile. Balanced uses the full normal AOD range; "
                + "Bright Daylight can add Extra Bright in strong daylight.", 13f, false, true);
        help.setPadding(0, dp(3), 0, dp(14));
        panel.addView(help);

        TextView presetTitle = text(getString(R.string.presets), 15f, true, false);
        panel.addView(presetTitle);

        LinearLayout presets = horizontal();
        presetButtons[0] = presetButton(
                presetLabel(getString(R.string.preset_dim), getString(R.string.preset_dim_detail)),
                AodPreset.DIM
        );
        presetButtons[1] = presetButton(
                presetLabel(getString(R.string.preset_balanced), getString(R.string.preset_balanced_detail)),
                AodPreset.BALANCED
        );
        presetButtons[2] = presetButton(
                presetLabel(getString(R.string.preset_bright), getString(R.string.preset_bright_detail)),
                AodPreset.BRIGHT
        );
        for (Button button : presetButtons) {
            presets.addView(button, weighted());
        }
        LinearLayout.LayoutParams presetRowParams = fullWidth();
        presetRowParams.setMargins(0, dp(8), 0, dp(14));
        panel.addView(presets, presetRowParams);

        presetDescription = text("", 13f, false, true);
        panel.addView(presetDescription);

        extraBrightnessHint = text("", 12f, true, true);
        extraBrightnessHint.setPadding(0, dp(7), 0, 0);
        extraBrightnessHint.setVisibility(View.GONE);
        panel.addView(extraBrightnessHint);

        return panel;
    }

    private LinearLayout buildManualPanel() {
        LinearLayout panel = card();
        panel.addView(cardTitle(getString(R.string.manual_settings)));
        TextView help = text(getString(R.string.manual_help), 13f, false, true);
        help.setPadding(0, dp(3), 0, dp(12));
        panel.addView(help);

        LinearLayout header = horizontal();
        TextView title = text(getString(R.string.manual_brightness), 15f, true, false);
        header.addView(title, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));
        manualBrightnessValue = text(manualLevelName(2), 22f, true, false);
        manualBrightnessValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(manualBrightnessValue, wrap());
        panel.addView(header, fullWidth());

        manualBrightnessSeekBar = new SeekBar(this);
        manualBrightnessSeekBar.setMax(MANUAL_LEVEL_COUNT - 1);
        manualBrightnessSeekBar.setContentDescription(getString(R.string.manual_brightness));
        SettingsUiTheme.styleSeekBar(manualBrightnessSeekBar);
        manualBrightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingForm) return;
                int level = progress + 1;
                manualBrightnessValue.setText(manualLevelName(level));
                updateExtraBrightnessLevelVisibility();
                updatePreview();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        panel.addView(manualBrightnessSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        ));
        panel.addView(buildManualLevelScale(), fullWidth());

        TextView manualHint = text(getString(R.string.manual_brightness_help), 12f, false, true);
        manualHint.setPadding(0, dp(4), 0, 0);
        panel.addView(manualHint);
        return panel;
    }

    private LinearLayout buildExtraBrightnessPanel() {
        LinearLayout panel = card();
        panel.setVisibility(View.GONE);
        panel.addView(cardTitle(getString(R.string.extra_bright_level_title)));
        TextView help = text(getString(R.string.extra_bright_level_help), 13f, false, true);
        help.setPadding(0, dp(3), 0, dp(10));
        panel.addView(help);

        if (!LunaaDevicePolicy.isSupportedIdentity(Build.DEVICE, Build.PRODUCT, Build.MODEL, Build.MANUFACTURER)) {
            TextView notice = text("Note: Extra Bright (HBM) is hardware-tailored for Realme GT Master Edition. Standard Adaptive AOD curves remain active.", 12f, false, true);
            notice.setTextColor(SettingsUiTheme.COLOR_MUTED);
            notice.setPadding(0, dp(2), 0, dp(8));
            panel.addView(notice);
        }

        automaticExtraBrightnessSwitch = new Switch(this);
        ((TextView) automaticExtraBrightnessSwitch).setText("Enable Extra Bright (HBM)");
        SettingsUiTheme.styleSwitch(automaticExtraBrightnessSwitch);
        automaticExtraBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingForm) return;
            updatePreview();
        });
        LinearLayout.LayoutParams automaticExtraParams = fullWidth();
        automaticExtraParams.setMargins(0, 0, 0, dp(10));
        panel.addView(automaticExtraBrightnessSwitch, automaticExtraParams);

        manualExtraBrightnessSwitch = new Switch(this);
        ((TextView) manualExtraBrightnessSwitch).setText("Enable Extra Bright");
        SettingsUiTheme.styleSwitch(manualExtraBrightnessSwitch);
        manualExtraBrightnessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingForm) return;
            updatePreview();
        });
        LinearLayout.LayoutParams manualExtraParams = fullWidth();
        manualExtraParams.setMargins(0, 0, 0, dp(10));
        panel.addView(manualExtraBrightnessSwitch, manualExtraParams);

        LinearLayout header = horizontal();
        TextView title = text(getString(R.string.extra_bright_level), 15f, true, false);
        header.addView(title, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        extraBrightnessLevelValue = text(extraLevelName(2), 22f, true, false);
        extraBrightnessLevelValue.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        header.addView(extraBrightnessLevelValue, wrap());
        panel.addView(header, fullWidth());

        extraBrightnessLevelSeekBar = new SeekBar(this);
        extraBrightnessLevelSeekBar.setMax(EXTRA_BRIGHT_LEVEL_COUNT - 1);
        extraBrightnessLevelSeekBar.setContentDescription(getString(R.string.extra_bright_level));
        SettingsUiTheme.styleSeekBar(extraBrightnessLevelSeekBar);
        extraBrightnessLevelSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (updatingForm) return;
                int level = progress + 1;
                extraBrightnessLevelValue.setText(extraLevelName(level));
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        panel.addView(extraBrightnessLevelSeekBar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        panel.addView(buildExtraLevelScale(), fullWidth());

        TextView range = text(getString(R.string.extra_bright_level_range), 12f, false, true);
        range.setPadding(0, dp(4), 0, 0);
        panel.addView(range);
        return panel;
    }

    private View buildAdvancedSettingsPanel() {
        LinearLayout panel = card();
        advancedSettingsToggle = new Button(this);
        advancedSettingsToggle.setText(R.string.advanced_settings_collapsed);
        SettingsUiTheme.styleDisclosureButton(advancedSettingsToggle, density(), false);
        advancedSettingsToggle.setOnClickListener(v -> {
            advancedExpanded = !advancedExpanded;
            advancedSettingsContent.setVisibility(advancedExpanded ? View.VISIBLE : View.GONE);
            advancedSettingsToggle.setText(advancedExpanded
                    ? R.string.advanced_settings_expanded
                    : R.string.advanced_settings_collapsed);
            SettingsUiTheme.styleDisclosureButton(advancedSettingsToggle, density(), advancedExpanded);
        });
        panel.addView(advancedSettingsToggle, fullWidth());

        advancedSettingsContent = new LinearLayout(this);
        advancedSettingsContent.setOrientation(LinearLayout.VERTICAL);
        advancedSettingsContent.setVisibility(View.GONE);
        advancedSettingsContent.setPadding(0, dp(12), 0, 0);

        TextView help = text(getString(R.string.advanced_settings_help), 12f, false, true);
        help.setPadding(0, 0, 0, dp(12));
        advancedSettingsContent.addView(help);

        advancedSettingsContent.addView(text(
                getString(R.string.manual_brightness_levels), 15f, true, false));
        for (int i = 0; i < 3; i++) {
            manualLevelFields[i] = buildPercentField(i + 1, true);
            advancedSettingsContent.addView(
                    buildPercentRow(manualLevelName(i + 1), manualLevelFields[i]), fullWidth());
        }

        TextView extraTitle = text(getString(R.string.extra_bright_levels), 15f, true, false);
        extraTitle.setPadding(0, dp(14), 0, 0);
        advancedSettingsContent.addView(extraTitle);
        for (int i = 0; i < 3; i++) {
            extraLevelFields[i] = buildPercentField(i + 1, false);
            advancedSettingsContent.addView(
                    buildPercentRow(extraLevelName(i + 1), extraLevelFields[i]), fullWidth());
        }

        advancedValidationValue = text("", 12f, true, false);
        advancedValidationValue.setTextColor(SettingsUiTheme.COLOR_WARNING);
        advancedValidationValue.setPadding(0, dp(10), 0, 0);
        advancedSettingsContent.addView(advancedValidationValue, fullWidth());

        panel.addView(advancedSettingsContent, fullWidth());
        return panel;
    }

    private LinearLayout buildPercentRow(String levelName, EditText field) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = text(levelName, 14f, false, false);
        row.addView(label, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(dp(88), dp(48));
        inputParams.setMargins(dp(8), dp(4), dp(6), dp(4));
        row.addView(field, inputParams);
        TextView percent = text("%", 14f, true, true);
        row.addView(percent, wrap());
        return row;
    }

    private EditText buildPercentField(int level, boolean manual) {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setContentDescription(getString(
                manual ? R.string.manual_level_percent_description : R.string.extra_level_percent_description,
                manual ? manualLevelName(level) : extraLevelName(level)));
        SettingsUiTheme.styleEditText(field, density());
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (updatingForm) return;
                if (validateAdvancedLevels()) updatePreview();
                else updateSaveEnabled();
            }
        });
        return field;
    }

    private LinearLayout buildManualLevelScale() {
        return buildNamedLevelScale(
                manualLevelName(1), manualLevelName(2), manualLevelName(3));
    }

    private LinearLayout buildExtraLevelScale() {
        return buildNamedLevelScale(
                extraLevelName(1), extraLevelName(2), extraLevelName(3));
    }

    private LinearLayout buildNamedLevelScale(String first, String second, String third) {
        LinearLayout scale = horizontal();
        TextView one = text(first, 12f, true, true);
        one.setGravity(Gravity.START);
        scale.addView(one, weightedNoMargin());
        TextView two = text(second, 12f, true, true);
        two.setGravity(Gravity.CENTER);
        scale.addView(two, weightedNoMargin());
        TextView three = text(third, 12f, true, true);
        three.setGravity(Gravity.END);
        scale.addView(three, weightedNoMargin());
        return scale;
    }

    private View buildActionBar() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(SettingsUiTheme.COLOR_SURFACE);

        LinearLayout actions = horizontal();
        actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setPadding(dp(14), dp(10), dp(14), dp(12));

        Button resetButton = new Button(this);
        resetButton.setText(R.string.reset);
        SettingsUiTheme.styleSecondaryButton(resetButton, density());
        resetButton.setOnClickListener(v -> {
            animateActionButton(v);
            int persistedRevision = settingsStore.load().getRevision();
            currentRevision = persistedRevision;
            applySnapshot(AodSettingsDefaults.balanced());
            if (automaticExtraBrightnessSwitch != null) {
                automaticExtraBrightnessSwitch.setChecked(true);
            }
            currentRevision = persistedRevision;
            statusValue.setText(R.string.reset_not_saved);
            showToast(R.string.reset_not_saved);
            updateSaveEnabled();
        });
        actions.addView(resetButton, weightedAction(0.8f));

        saveButton = new Button(this);
        saveButton.setText(R.string.save_changes);
        SettingsUiTheme.stylePrimaryButton(saveButton, density());
        saveButton.setOnClickListener(v -> {
            animateActionButton(v);
            saveForm();
        });
        actions.addView(saveButton, weightedAction(1.2f));
        wrapper.addView(actions, fullWidth());
        return wrapper;
    }

    private CharSequence presetLabel(String title, String detail) {
        SpannableStringBuilder label = new SpannableStringBuilder();
        int titleStart = label.length();
        label.append(title);
        label.setSpan(new StyleSpan(Typeface.BOLD), titleStart, label.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (detail != null && !detail.trim().isEmpty()) {
            label.append("\n");
            int detailStart = label.length();
            label.append(detail);
            label.setSpan(new RelativeSizeSpan(0.72f), detailStart, label.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return label;
    }

    private Button presetButton(CharSequence label, AodPreset preset) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(15f);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(false);
        SettingsUiTheme.styleButton(button, density(), false);
        button.setOnClickListener(v -> selectPreset(preset));
        return button;
    }

    private void selectPreset(AodPreset preset) {
        currentPreset = selectablePreset(preset);
        updatePresetButtons();
        updatePresetCopy();
        updateExtraBrightnessLevelVisibility();
        statusValue.setText(R.string.preset_not_saved);
        updatePreview();
    }

    private void applySnapshot(AodSettingsSnapshot snapshot) {
        updatingForm = true;
        try {
            enabledSwitch.setChecked(snapshot.isEnabled());
            currentMode = snapshot.getMode();
            currentPreset = selectablePreset(snapshot.getPreset());
            for (int i = 0; i < 3; i++) {
                manualLevelPercents[i] = snapshot.getManualLevelPercent(i + 1);
                extraLevelPercents[i] = snapshot.getExtraBrightLevelPercent(i + 1);
            }
            manualBrightnessSeekBar.setProgress(snapshot.getManualLevel() - 1);
            automaticExtraBrightnessSwitch.setChecked(settingsStore.loadAutomaticExtraBrightnessEnabled());
            manualExtraBrightnessSwitch.setChecked(snapshot.isManualExtraBrightEnabled());
            extraBrightnessLevelSeekBar.setProgress(snapshot.getExtraBrightLevel() - 1);
            syncAdvancedFieldsFromState();
        } finally {
            updatingForm = false;
        }

        updateModeUi();
        updatePresetButtons();
        updatePresetCopy();
        manualBrightnessValue.setText(
                manualLevelName(manualBrightnessSeekBar.getProgress() + 1));
        extraBrightnessLevelValue.setText(
                extraLevelName(extraBrightnessLevelSeekBar.getProgress() + 1));
        updateExtraBrightnessLevelVisibility();
        updateMasterCopy();
        validateAdvancedLevels();
        updatePreview();
    }

    private void setMode(AodMode mode) {
        currentMode = mode == null ? AodMode.AUTOMATIC : mode;
        updateModeUi();
        updateExtraBrightnessLevelVisibility();
        updatePreview();
    }

    private void updateModeUi() {
        if (automaticPanel == null || manualPanel == null) return;
        boolean automatic = currentMode == AodMode.AUTOMATIC;
        automaticPanel.setVisibility(automatic ? View.VISIBLE : View.GONE);
        manualPanel.setVisibility(automatic ? View.GONE : View.VISIBLE);
        SettingsUiTheme.styleButton(automaticModeButton, density(), automatic);
        SettingsUiTheme.styleButton(manualModeButton, density(), !automatic);
    }

    private void saveForm() {
        if (!settingsStore.isWritableForXposed()) {
            statusValue.setText(R.string.shared_prefs_unavailable);
            showToast(R.string.shared_prefs_unavailable);
            return;
        }
        if (!validateAdvancedLevels()) {
            statusValue.setText(R.string.advanced_invalid);
            updateSaveEnabled();
            return;
        }
        AodSettingsSnapshot draft = readDraft(currentRevision + 1);
        boolean automaticExtraSaved = settingsStore.saveAutomaticExtraBrightnessEnabled(
                automaticExtraBrightnessSwitch == null || automaticExtraBrightnessSwitch.isChecked());
        if (automaticExtraSaved && settingsStore.save(draft)) {
            currentRevision = draft.getRevision();
            savedFormSignature = formSignature();
            statusValue.setText(R.string.save_success);
            showToast(R.string.save_success);
            boolean extraBrightMayBeUsed = draft.isEnabled()
                    && ((draft.isAutomaticMode()
                            && draft.getPreset() == AodPreset.BRIGHT
                            && (automaticExtraBrightnessSwitch == null || automaticExtraBrightnessSwitch.isChecked()))
                        || (draft.isManualMode()
                            && draft.getManualLevel() == BrightnessLevelConfig.MAX_LEVEL
                            && draft.isManualExtraBrightEnabled()));
            if (extraBrightMayBeUsed) RootAccessPrimer.request(this);
        } else {
            statusValue.setText(R.string.save_failed);
            showToast(R.string.save_failed);
        }
        updatePreview();
        updateSaveEnabled();
    }

    private AodSettingsSnapshot readDraft(int revision) {
        AodSettingsSnapshot curve = AodSettingsDefaults.forPreset(currentPreset);
        return new AodSettingsSnapshot(
                enabledSwitch.isChecked(),
                currentMode,
                100,
                AodSettingsSnapshot.MIN_BRIGHTNESS,
                currentPreset,
                AodPreset.DIM.defaultCapPercent(),
                AodPreset.BALANCED.defaultCapPercent(),
                AodPreset.BRIGHT.defaultCapPercent(),
                manualBrightnessSeekBar.getProgress() + 1,
                manualLevelPercents[0],
                manualLevelPercents[1],
                manualLevelPercents[2],
                manualExtraBrightnessSwitch != null && manualExtraBrightnessSwitch.isChecked(),
                extraBrightnessLevelSeekBar.getProgress() + 1,
                extraLevelPercents[0],
                extraLevelPercents[1],
                extraLevelPercents[2],
                curve.copyLux(),
                curve.copyBrightness(),
                Math.max(0, revision)
        );
    }

    private void updatePreview() {
        if (previewValue == null || warningValue == null || enabledSwitch == null) return;
        updateMasterCopy();
        boolean advancedValid = validateAdvancedLevels();
        updateSaveEnabled();
        if (!advancedValid) {
            warningValue.setText(R.string.advanced_invalid);
            return;
        }
        AodSettingsSnapshot draft = readDraft(currentRevision);

        ambientValue.setVisibility(draft.isManualMode() ? View.GONE : View.VISIBLE);
        if (!draft.isEnabled()) {
            previewValue.setText(R.string.preview_stock);
            extraPreviewValue.setVisibility(View.GONE);
            warningValue.setText(R.string.empty);
            return;
        }

        if (draft.isManualMode()) {
            int level = draft.getManualLevel();
            int percent = draft.getManualLevelPercent(level);
            previewValue.setText(getString(
                    R.string.preview_manual_level, manualLevelName(level), percent));
            if (level == BrightnessLevelConfig.MAX_LEVEL) {
                extraPreviewValue.setVisibility(View.VISIBLE);
                if (draft.isManualExtraBrightEnabled()) {
                    extraPreviewValue.setText(getString(
                            R.string.manual_extra_bright_active,
                            extraLevelName(draft.getExtraBrightLevel()),
                            draft.getExtraBrightPercent()));
                    extraPreviewValue.setTextColor(SettingsUiTheme.COLOR_ACCENT);
                } else {
                    extraPreviewValue.setText("Extra Bright is off. Manual Bright stays at 100% normal AOD brightness.");
                    extraPreviewValue.setTextColor(SettingsUiTheme.COLOR_MUTED);
                }
            } else {
                extraPreviewValue.setVisibility(View.GONE);
            }
            warningValue.setText(percent >= 90
                    ? R.string.high_manual_brightness_warning
                    : R.string.empty);
            return;
        }

        if (Float.isNaN(currentLux)) {
            previewValue.setText(R.string.preview_waiting);
            updateExtraBrightnessPreview(draft);
            warningValue.setText(R.string.empty);
            return;
        }

        float target = BrightnessCurve.targetForLux(currentLux, draft);
        previewValue.setText(getString(
                R.string.preview_current_percent,
                Math.round(target * 100f)
        ));
        updateExtraBrightnessPreview(draft);
        warningValue.setText(R.string.empty);
    }

    private void updateExtraBrightnessPreview(AodSettingsSnapshot draft) {
        if (!draft.isAutomaticMode() || draft.getPreset() != AodPreset.BRIGHT) {
            extraPreviewValue.setVisibility(View.GONE);
            return;
        }

        extraPreviewValue.setVisibility(View.VISIBLE);
        if (automaticExtraBrightnessSwitch != null && !automaticExtraBrightnessSwitch.isChecked()) {
            extraPreviewValue.setText("Extra Bright is off. Bright Daylight stays on the normal AOD brightness curve without HBM.");
            extraPreviewValue.setTextColor(SettingsUiTheme.COLOR_MUTED);
            return;
        }
        String strength = extraLevelName(draft.getExtraBrightLevel());
        if (!Float.isNaN(currentLux) && currentLux >= ExtraBrightnessPolicy.ENABLE_LUX) {
            extraPreviewValue.setText(getString(
                    R.string.extra_bright_active_outdoors_level,
                    strength,
                    draft.getExtraBrightPercent()));
            extraPreviewValue.setTextColor(SettingsUiTheme.COLOR_ACCENT);
        } else {
            extraPreviewValue.setText(getString(
                    R.string.extra_bright_available_level,
                    strength,
                    draft.getExtraBrightPercent()));
            extraPreviewValue.setTextColor(SettingsUiTheme.COLOR_MUTED);
        }
    }

    private void updatePresetButtons() {
        for (int i = 0; i < presetButtons.length; i++) {
            Button button = presetButtons[i];
            if (button == null) continue;
            AodPreset preset = i == 0 ? AodPreset.DIM : (i == 1 ? AodPreset.BALANCED : AodPreset.BRIGHT);
            SettingsUiTheme.styleButton(button, density(), currentPreset == preset);
        }
    }

    private void updatePresetCopy() {
        int description;
        if (currentPreset == AodPreset.DIM) {
            description = R.string.preset_dim_description;
        } else if (currentPreset == AodPreset.BRIGHT) {
            description = R.string.preset_bright_description;
        } else {
            description = R.string.preset_balanced_description;
        }
        presetDescription.setText(description);

        boolean extraBrightEligible = currentPreset == AodPreset.BRIGHT;
        extraBrightnessHint.setVisibility(extraBrightEligible ? View.VISIBLE : View.GONE);
        if (extraBrightEligible) extraBrightnessHint.setText(R.string.extra_bright_auto_hint);
        updateSaveEnabled();
    }

    private int getExtraBrightPercent() {
        int level = extraBrightnessLevelSeekBar == null
                ? AodSettingsDefaults.DEFAULT_EXTRA_BRIGHT_LEVEL
                : extraBrightnessLevelSeekBar.getProgress() + 1;
        return extraLevelPercents[level - 1];
    }

    private void updateExtraBrightnessLevelVisibility() {
        if (extraBrightnessPanel == null) return;
        boolean visible = shouldShowExtraBrightness();
        extraBrightnessPanel.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (automaticExtraBrightnessSwitch != null) {
            ((TextView) automaticExtraBrightnessSwitch).setVisibility(
                    visible && currentMode == AodMode.AUTOMATIC ? View.VISIBLE : View.GONE);
        }
        if (manualExtraBrightnessSwitch != null) {
            ((TextView) manualExtraBrightnessSwitch).setVisibility(
                    visible && currentMode == AodMode.MANUAL ? View.VISIBLE : View.GONE);
        }
    }

    private boolean shouldShowExtraBrightness() {
        if (currentMode == AodMode.MANUAL) {
            return manualBrightnessSeekBar != null
                    && manualBrightnessSeekBar.getProgress() + 1 == BrightnessLevelConfig.MAX_LEVEL;
        }
        return currentMode == AodMode.AUTOMATIC && currentPreset == AodPreset.BRIGHT;
    }

    private String manualLevelName(int level) {
        if (level <= 1) return getString(R.string.manual_level_dim);
        if (level >= BrightnessLevelConfig.MAX_LEVEL) return getString(R.string.manual_level_bright);
        return getString(R.string.manual_level_balanced);
    }

    private String extraLevelName(int level) {
        if (level <= 1) return getString(R.string.extra_level_low);
        if (level >= BrightnessLevelConfig.MAX_LEVEL) return getString(R.string.extra_level_max);
        return getString(R.string.extra_level_medium);
    }

    private void animateActionButton(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .alpha(0.82f)
                .setDuration(70L)
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(120L)
                        .start())
                .start();
    }

    private void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private void updateMasterCopy() {
        if (masterDetailValue == null || enabledSwitch == null) return;
        masterDetailValue.setText(enabledSwitch.isChecked()
                ? R.string.module_enabled_detail
                : R.string.module_disabled_detail);
    }

    private static AodPreset selectablePreset(AodPreset preset) {
        if (preset == AodPreset.DIM || preset == AodPreset.BRIGHT) return preset;
        return AodPreset.BALANCED;
    }

    private boolean validateAdvancedLevels() {
        if (advancedValidationValue == null) return true;
        int[] manual = parseLevelFields(manualLevelFields);
        int[] extra = parseLevelFields(extraLevelFields);
        if (manual == null || extra == null) {
            advancedValidationValue.setText(R.string.advanced_error_range);
            return false;
        }
        if (!isNondecreasing(manual) || !isNondecreasing(extra)) {
            advancedValidationValue.setText(R.string.advanced_error_order);
            return false;
        }
        System.arraycopy(manual, 0, manualLevelPercents, 0, 3);
        System.arraycopy(extra, 0, extraLevelPercents, 0, 3);
        advancedValidationValue.setText(R.string.empty);
        return true;
    }

    private int[] parseLevelFields(EditText[] fields) {
        int[] values = new int[3];
        for (int i = 0; i < 3; i++) {
            if (fields[i] == null || fields[i].getText() == null) return null;
            String raw = fields[i].getText().toString().trim();
            if (raw.isEmpty()) return null;
            try {
                int value = Integer.parseInt(raw);
                if (value < BrightnessLevelConfig.MIN_PERCENT
                        || value > BrightnessLevelConfig.MAX_PERCENT) return null;
                values[i] = value;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return values;
    }

    private static boolean isNondecreasing(int[] values) {
        return values != null && values.length == 3
                && values[0] <= values[1]
                && values[1] <= values[2];
    }

    private void syncAdvancedFieldsFromState() {
        for (int i = 0; i < 3; i++) {
            if (manualLevelFields[i] != null) manualLevelFields[i].setText(String.valueOf(manualLevelPercents[i]));
            if (extraLevelFields[i] != null) extraLevelFields[i].setText(String.valueOf(extraLevelPercents[i]));
        }
    }

    private String formSignature() {
        if (enabledSwitch == null || manualBrightnessSeekBar == null || extraBrightnessLevelSeekBar == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(enabledSwitch.isChecked()).append('|')
                .append(currentMode).append('|')
                .append(currentPreset).append('|')
                .append(manualBrightnessSeekBar.getProgress()).append('|')
                .append(manualExtraBrightnessSwitch != null && manualExtraBrightnessSwitch.isChecked()).append('|')
                .append(automaticExtraBrightnessSwitch == null || automaticExtraBrightnessSwitch.isChecked()).append('|')
                .append(extraBrightnessLevelSeekBar.getProgress());
        appendFieldSignature(b, manualLevelFields);
        appendFieldSignature(b, extraLevelFields);
        return b.toString();
    }

    private static void appendFieldSignature(StringBuilder b, EditText[] fields) {
        for (EditText field : fields) {
            b.append('|');
            if (field != null && field.getText() != null) b.append(field.getText().toString().trim());
        }
    }

    private void updateSaveEnabled() {
        if (saveButton == null || settingsStore == null) return;
        boolean valid = advancedValidationValue == null || validateAdvancedLevelsNoMutation();
        saveButton.setEnabled(settingsStore.isWritableForXposed() && valid);
    }

    private boolean validateAdvancedLevelsNoMutation() {
        int[] manual = parseLevelFields(manualLevelFields);
        int[] extra = parseLevelFields(extraLevelFields);
        return manual != null && extra != null && isNondecreasing(manual) && isNondecreasing(extra);
    }

    private String describeAmbientLight(float lux) {
        if (lux < 10f) return getString(R.string.ambient_dark);
        if (lux < 200f) return getString(R.string.ambient_indoor);
        if (lux < 1_000f) return getString(R.string.ambient_bright_indoor);
        if (lux < ExtraBrightnessPolicy.ENABLE_LUX) return getString(R.string.ambient_daylight);
        return getString(R.string.ambient_strong_daylight);
    }

    private LinearLayout card() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        view.setPadding(dp(16), dp(15), dp(16), dp(15));
        SettingsUiTheme.styleCard(view, density());
        return view;
    }

    private TextView cardTitle(String value) {
        return text(value, 18f, true, false);
    }

    private TextView text(String value, float sizeSp, boolean bold, boolean muted) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        SettingsUiTheme.styleText(view, muted);
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

    private LinearLayout.LayoutParams spacedFullWidth() {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, 0, 0, dp(12));
        return params;
    }

    private LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        return params;
    }


    private LinearLayout.LayoutParams weightedNoMargin() {
        return new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private LinearLayout.LayoutParams weightedAction(float weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        );
        params.setMargins(dp(3), dp(2), dp(3), dp(2));
        return params;
    }

    private View buildUpdateCard() {
        LinearLayout card = card();
        TextView eyebrow = text("UPDATES & GITHUB", 12f, true, true);
        eyebrow.setTextColor(SettingsUiTheme.COLOR_MUTED);
        card.addView(eyebrow);

        TextView title = text("GitHub Releases", 18f, true, false);
        title.setPadding(0, dp(4), 0, dp(6));
        card.addView(title);

        updateStatusText = text("Version " + AodReleaseInfo.VERSION_NAME + " (" + AodReleaseInfo.VERSION_CODE + ")", 14f, false, false);
        updateStatusText.setTextColor(SettingsUiTheme.COLOR_TEXT);
        card.addView(updateStatusText);

        updateDetailsText = text("", 13f, false, false);
        updateDetailsText.setTextColor(SettingsUiTheme.COLOR_MUTED);
        updateDetailsText.setVisibility(View.GONE);
        updateDetailsText.setPadding(0, dp(4), 0, dp(8));
        card.addView(updateDetailsText);

        updateProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        updateProgressBar.setMax(100);
        updateProgressBar.setVisibility(View.GONE);
        updateProgressBar.setPadding(0, dp(6), 0, dp(6));
        card.addView(updateProgressBar, fullWidth());

        updateActionButton = new Button(this);
        updateActionButton.setText("Check for updates");
        SettingsUiTheme.styleButton(updateActionButton, density(), false);
        updateActionButton.setOnClickListener(v -> {
            animateActionButton(v);
            if (latestRelease != null && latestRelease.hasUpdate && latestRelease.apkDownloadUrl != null) {
                startUpdateDownload(latestRelease);
            } else {
                triggerUpdateCheck(true);
            }
        });
        card.addView(updateActionButton, fullWidth());

        return card;
    }

    private void triggerUpdateCheck(boolean manual) {
        if (updateStatusText != null) {
            updateStatusText.setText("Checking GitHub for updates...");
        }
        if (updateActionButton != null && !manual) {
            updateActionButton.setEnabled(false);
        }

        AppUpdater.checkForUpdates(this, new AppUpdater.CheckCallback() {
            @Override
            public void onSuccess(AppUpdater.ReleaseInfo releaseInfo) {
                latestRelease = releaseInfo;
                if (updateActionButton != null) updateActionButton.setEnabled(true);
                if (releaseInfo != null && releaseInfo.hasUpdate) {
                    if (updateStatusText != null) {
                        updateStatusText.setText("Update available: " + releaseInfo.tagName);
                        updateStatusText.setTextColor(SettingsUiTheme.COLOR_ACCENT);
                    }
                    if (updateDetailsText != null && releaseInfo.changelog != null && !releaseInfo.changelog.isEmpty()) {
                        updateDetailsText.setText(releaseInfo.changelog.trim());
                        updateDetailsText.setVisibility(View.VISIBLE);
                    }
                    if (updateActionButton != null) {
                        updateActionButton.setText("Download & Install " + releaseInfo.tagName);
                        SettingsUiTheme.stylePrimaryButton(updateActionButton, density());
                    }
                    if (manual) {
                        Toast.makeText(SettingsActivity.this, "New version found: " + releaseInfo.tagName, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    if (updateStatusText != null) {
                        updateStatusText.setText("Latest version installed (" + AodReleaseInfo.VERSION_NAME + ")");
                        updateStatusText.setTextColor(SettingsUiTheme.COLOR_TEXT);
                    }
                    if (updateDetailsText != null) {
                        updateDetailsText.setVisibility(View.GONE);
                    }
                    if (updateActionButton != null) {
                        updateActionButton.setText("Check for updates");
                        SettingsUiTheme.styleButton(updateActionButton, density(), false);
                    }
                    if (manual) {
                        Toast.makeText(SettingsActivity.this, "You have the latest version", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(String message) {
                if (updateActionButton != null) {
                    updateActionButton.setEnabled(true);
                    updateActionButton.setText("Check for updates");
                }
                if (updateStatusText != null) {
                    updateStatusText.setText("Version " + AodReleaseInfo.VERSION_NAME + " (check failed)");
                    updateStatusText.setTextColor(SettingsUiTheme.COLOR_MUTED);
                }
                if (manual) {
                    Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void startUpdateDownload(AppUpdater.ReleaseInfo release) {
        if (release == null || release.apkDownloadUrl == null) return;
        if (updateActionButton != null) {
            updateActionButton.setEnabled(false);
            updateActionButton.setText("Downloading update...");
        }
        if (updateProgressBar != null) {
            updateProgressBar.setProgress(0);
            updateProgressBar.setVisibility(View.VISIBLE);
        }

        AppUpdater.downloadApk(this, release.apkDownloadUrl, release.apkName, new AppUpdater.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                if (updateProgressBar != null) updateProgressBar.setProgress(percent);
                if (updateStatusText != null) updateStatusText.setText("Downloading: " + percent + "%");
            }

            @Override
            public void onDownloaded(File apkFile) {
                if (updateProgressBar != null) updateProgressBar.setVisibility(View.GONE);
                if (updateStatusText != null) updateStatusText.setText("Installing update...");

                // Try silent root install first
                boolean rootSuccess = AppUpdater.installApkWithRoot(SettingsActivity.this, apkFile);
                if (rootSuccess) {
                    Toast.makeText(SettingsActivity.this, "Updated successfully! Restarting...", Toast.LENGTH_SHORT).show();
                    if (updateActionButton != null) {
                        updateActionButton.setEnabled(true);
                        updateActionButton.setText("Updated!");
                    }
                    recreate();
                } else {
                    // Standard package installer intent
                    if (updateActionButton != null) {
                        updateActionButton.setEnabled(true);
                        updateActionButton.setText("Install downloaded APK");
                    }
                    AppUpdater.startSystemInstall(SettingsActivity.this, apkFile);
                }
            }

            @Override
            public void onError(String message) {
                if (updateProgressBar != null) updateProgressBar.setVisibility(View.GONE);
                if (updateActionButton != null) {
                    updateActionButton.setEnabled(true);
                    updateActionButton.setText("Retry download");
                }
                if (updateStatusText != null) {
                    updateStatusText.setText("Download failed: " + message);
                    updateStatusText.setTextColor(SettingsUiTheme.COLOR_WARNING);
                }
                Toast.makeText(SettingsActivity.this, "Download error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    private int dp(int value) {
        return Math.round(value * density());
    }
}
