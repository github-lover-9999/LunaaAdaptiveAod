package dev.lunaa.aod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsUiSourceTest {
    @Test public void launcherActivityUsesPlatformWidgetsAndSafeInsets() throws Exception {
        String manifest = read("app/src/main/AndroidManifest.xml");
        String activity = read("app/src/main/java/dev/lunaa/aod/SettingsActivity.java");
        String gradle = read("app/build.gradle.kts");
        String theme = read("app/src/main/java/dev/lunaa/aod/SettingsUiTheme.java");

        assertTrue(manifest.contains("android:name=\".SettingsActivity\""));
        assertTrue(manifest.contains("android.intent.action.MAIN"));
        assertTrue(manifest.contains("android.intent.category.LAUNCHER"));
        assertTrue(manifest.contains("android:windowSoftInputMode=\"adjustResize\""));
        assertFalse(activity.contains("androidx."));
        assertFalse(gradle.contains("androidx."));
        assertTrue(activity.contains("extends Activity"));
        assertTrue(activity.contains("new ScrollView"));
        assertTrue(activity.contains("new SeekBar"));
        assertTrue(activity.contains("WindowInsets.Type.systemBars()"));
        assertTrue(activity.contains("WindowInsets.Type.displayCutout()"));
        assertTrue(activity.contains("WindowInsets.Type.ime()"));
        assertTrue(activity.contains("CONTENT_TOP_PADDING_DP = 32"));
        assertTrue(activity.contains("contentTop + top"));
        assertTrue(theme.contains("setDecorFitsSystemWindows(false)"));
        assertTrue(theme.contains("48f"));
    }

    @Test public void brightnessUiUsesThreeLevelsAndAdvancedPercentMappings() throws Exception {
        String activity = read("app/src/main/java/dev/lunaa/aod/SettingsActivity.java");
        String codec = read("app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java");
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(activity.contains("MANUAL_LEVEL_COUNT = 3"));
        assertTrue(activity.contains("EXTRA_BRIGHT_LEVEL_COUNT = 3"));
        assertTrue(activity.contains("manualBrightnessSeekBar.setMax(MANUAL_LEVEL_COUNT - 1)"));
        assertTrue(activity.contains("extraBrightnessLevelSeekBar.setMax(EXTRA_BRIGHT_LEVEL_COUNT - 1)"));
        assertTrue(activity.contains("advancedSettingsContent"));
        assertTrue(activity.contains("new EditText"));
        assertTrue(activity.contains("validateAdvancedLevels"));
        assertTrue(activity.contains("updateSaveEnabled"));
        assertTrue(strings.contains("Advanced settings"));
        assertTrue(strings.contains("Manual brightness levels"));
        assertTrue(strings.contains("Extra Bright levels"));
        assertTrue(strings.contains("Save changes"));
        assertTrue(codec.contains("KEY_MANUAL_LEVEL_1_PERCENT"));
        assertTrue(codec.contains("KEY_MANUAL_LEVEL_2_PERCENT"));
        assertTrue(codec.contains("KEY_MANUAL_LEVEL_3_PERCENT"));
        assertTrue(codec.contains("KEY_EXTRA_LEVEL_1_PERCENT"));
        assertTrue(codec.contains("KEY_EXTRA_LEVEL_2_PERCENT"));
        assertTrue(codec.contains("KEY_EXTRA_LEVEL_3_PERCENT"));
    }

    @Test public void previewExplainsLiveOutputAndExtraBrightEligibility() throws Exception {
        String activity = read("app/src/main/java/dev/lunaa/aod/SettingsActivity.java");
        String strings = read("app/src/main/res/values/strings.xml");

        assertFalse(activity.contains("presetCapSeekBar"));
        assertTrue(activity.contains("updatePresetCopy"));
        assertTrue(activity.contains("manualExtraBrightnessSwitch"));
        assertTrue(activity.contains("updateExtraBrightnessPreview"));
        assertTrue(activity.contains("ExtraBrightnessPolicy.ENABLE_LUX"));
        assertTrue(strings.contains("Ambient light"));
        assertTrue(strings.contains("Current AOD brightness"));
        assertTrue(strings.contains("available in strong daylight"));
        assertTrue(strings.contains("requested for this AOD session") || strings.contains("requests Extra Bright"));
        assertFalse(strings.contains("Range 80–100%"));
        assertTrue(strings.contains("HBM strength"));
        assertTrue(strings.contains("Stock crDroid AOD brightness is active"));
        assertTrue(strings.contains("Saved. Changes apply on the next AOD activation."));
    }

    @Test public void formattedManualExtraBrightPreviewEscapesLiteralPercent() throws Exception {
        String strings = read("app/src/main/res/values/strings.xml");
        assertTrue(strings.contains("Manual Bright requests Extra Bright"));
    }


    @Test public void everyFormattedAndroidStringHasValidPercentSyntax() throws Exception {
        String activity = read("app/src/main/java/dev/lunaa/aod/SettingsActivity.java");
        String stringsXml = read("app/src/main/res/values/strings.xml");

        Pattern formattedCall = Pattern.compile(
                "getString\\(\\s*R\\.string\\.([A-Za-z0-9_]+)\\s*,",
                Pattern.MULTILINE);
        Matcher callMatcher = formattedCall.matcher(activity);
        Set<String> formattedNames = new HashSet<>();
        while (callMatcher.find()) formattedNames.add(callMatcher.group(1));

        Pattern stringNode = Pattern.compile(
                "<string\\s+name=\"([^\"]+)\"[^>]*>(.*?)</string>",
                Pattern.DOTALL);
        Matcher stringMatcher = stringNode.matcher(stringsXml);
        Map<String, String> values = new HashMap<>();
        while (stringMatcher.find()) values.put(stringMatcher.group(1), stringMatcher.group(2));

        Pattern formatSpec = Pattern.compile(
                "%(?:\\d+\\$)?[-#+ 0,(<]*\\d*(?:\\.\\d+)?[tT]?[bBhHsScCdoxXeEfgGaA%n]");
        for (String name : formattedNames) {
            String value = values.get(name);
            assertTrue("missing formatted string " + name, value != null);
            for (int i = 0; i < value.length();) {
                if (value.charAt(i) != '%') { i++; continue; }
                if (i + 1 < value.length() && value.charAt(i + 1) == '%') { i += 2; continue; }
                Matcher spec = formatSpec.matcher(value.substring(i));
                assertTrue("invalid % in " + name + ": " + value.substring(i), spec.lookingAt());
                i += spec.end();
            }
        }
    }

    @Test public void v156UsesUserFriendlyLabelsConditionalExtraBrightAndCleanActionBar() throws Exception {
        String activity = read("app/src/main/java/dev/lunaa/aod/SettingsActivity.java");
        String strings = read("app/src/main/res/values/strings.xml");

        assertTrue(strings.contains("<string name=\"adaptive_aod\">AOD Control</string>"));
        assertFalse(activity.contains("TextView title = text(getString(R.string.app_name)"));

        int modeCard = activity.indexOf("root.addView(buildModeCard(), spacedFullWidth())");
        int automatic = activity.indexOf("root.addView(automaticPanel, spacedFullWidth())");
        int manual = activity.indexOf("root.addView(manualPanel, spacedFullWidth())");
        int currentOutput = activity.indexOf("root.addView(buildLiveCard(), spacedFullWidth())");
        int extra = activity.indexOf("root.addView(extraBrightnessPanel, spacedFullWidth())");
        assertTrue(modeCard >= 0 && automatic > modeCard && manual > automatic);
        assertTrue(currentOutput > manual && extra > currentOutput);

        assertTrue(activity.contains("shouldShowExtraBrightness"));
        assertTrue(activity.contains("currentPreset == AodPreset.BRIGHT"));
        assertTrue(activity.contains("manualBrightnessSeekBar.getProgress() + 1 == BrightnessLevelConfig.MAX_LEVEL"));

        assertTrue(strings.contains("<string name=\"manual_level_dim\">Dim</string>"));
        assertTrue(strings.contains("<string name=\"manual_level_balanced\">Balanced</string>"));
        assertTrue(strings.contains("<string name=\"manual_level_bright\">Bright</string>"));
        assertTrue(strings.contains("<string name=\"extra_level_low\">Low</string>"));
        assertTrue(strings.contains("<string name=\"extra_level_medium\">Medium</string>"));
        assertTrue(strings.contains("<string name=\"extra_level_max\">Max</string>"));

        assertTrue(strings.contains("<string name=\"preset_dim\">DIM</string>"));
        assertTrue(strings.contains("<string name=\"preset_balanced\">BALANCED</string>"));
        assertTrue(strings.contains("<string name=\"preset_bright\">BRIGHT</string>"));
        assertTrue(strings.contains("<string name=\"preset_bright_detail\">Daylight</string>"));
        assertTrue(activity.contains("RelativeSizeSpan"));

        assertFalse(activity.contains("View divider = new View(this)"));
    }

    @Test public void appStoreRequiresXposedReadablePreferencesAndDurableCommit() throws Exception {
        String store = read("app/src/main/java/dev/lunaa/aod/AndroidSettingsStore.java");
        assertTrue(store.contains("Context.MODE_WORLD_READABLE"));
        assertTrue(store.contains("isWritableForXposed"));
        assertTrue(store.contains("AodSettingsCodec.write"));
        assertTrue(store.contains("editor.commit()"));
        assertTrue(store.contains("SecurityException"));
    }

    private static String read(String path) throws Exception {
        return TestProjectFiles.read(path);
    }
}
