from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def require(cond, msg):
    if not cond:
        raise AssertionError(msg)

# Defaults and fixed automatic profiles.
defaults = read('app/src/main/java/dev/lunaa/aod/AodSettingsDefaults.java')
preset = read('app/src/main/java/dev/lunaa/aod/AodPreset.java')
require('DEFAULT_MANUAL_LEVEL_2_PERCENT = 50;' in defaults,
        'Manual Balanced default must be 50%')
require('DEFAULT_MANUAL_LEVEL_3_PERCENT = 100;' in defaults,
        'Manual Bright must remain 100%')
require('DEFAULT_EXTRA_BRIGHT_LEVEL = 1;' in defaults,
        'Extra Bright default strength must be Low (level 1)')
require('BALANCED(1, 40, 100, 100)' in preset or 'BALANCED(1, 0, 100, 100)' in preset,
        'Automatic Balanced must use a 100% envelope')
require('BRIGHT(2, 70, 100, 100)' in preset,
        'Automatic Bright Daylight normal envelope must remain 100% before HBM')

# Model/persistence contract for separate Manual Extra Bright.
snapshot = read('app/src/main/java/dev/lunaa/aod/AodSettingsSnapshot.java')
codec = read('app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java')
require('manualExtraBrightEnabled' in snapshot,
        'Snapshot must carry separate Manual Extra Bright state')
require('isManualExtraBrightEnabled()' in snapshot,
        'Snapshot must expose Manual Extra Bright state')
require('KEY_MANUAL_EXTRA_BRIGHT_ENABLED' in codec and 'manual_extra_bright_enabled' in codec,
        'Codec must persist Manual Extra Bright state')

# Policy: manual HBM explicit; automatic Bright Daylight remains ambient driven.
policy = read('app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java')
require('settings.isManualExtraBrightEnabled()' in policy,
        'Manual HBM must require explicit Extra Bright toggle')
require('ENABLE_LUX = 1_500f' in policy and 'DISABLE_LUX = 700f' in policy,
        'Automatic HBM thresholds must remain 1500/700 lux')
require('ENABLE_DWELL_MS = 600L' in policy and 'DISABLE_DWELL_MS = 1_500L' in policy,
        'Automatic HBM dwell must remain 600/1500 ms')

# UI contract.
activity = read('app/src/main/java/dev/lunaa/aod/SettingsActivity.java')
strings = read('app/src/main/res/values/strings.xml')
theme = read('app/src/main/java/dev/lunaa/aod/SettingsUiTheme.java')
require('presetCapSeekBar' not in activity,
        'Automatic percentage/cap slider must be removed')
require('manualExtraBrightnessSwitch' in activity,
        'Manual Bright must expose separate Extra Bright switch')
require('Bright Daylight' in strings,
        'Automatic brightest preset must be labeled Bright Daylight')
require('new int[]{COLOR_ACCENT, Color.rgb(56, 59, 62)}' in theme,
        'Checked switch track must use bright yellow accent fill')
require('actionBottom + bottom' in activity,
        'Bottom action bar must retain system/IME inset')

# Late SystemUI reset recovery.
controller = read('app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java')
require('400L' in controller and 'udfps-recovery' in controller,
        'Controller must reapply brightness again after 400 ms stock/UDFPS reset race')

print('PASS: v1.6.4 source requirements')
