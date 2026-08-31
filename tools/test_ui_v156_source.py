from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

def read(rel): return (ROOT / rel).read_text(encoding='utf-8')
def req(cond, msg):
    if not cond: raise AssertionError(msg)

activity = read('app/src/main/java/dev/lunaa/aod/SettingsActivity.java')
strings = read('app/src/main/res/values/strings.xml')
policy = read('app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java')
gradle = read('app/build.gradle.kts')

req('<string name="adaptive_aod">AOD Control</string>' in strings, 'master label must be AOD Control')
req('TextView title = text(getString(R.string.app_name)' not in activity, 'redundant in-content app title remains')
mode = activity.index('root.addView(buildModeCard(), spacedFullWidth())')
auto = activity.index('root.addView(automaticPanel, spacedFullWidth())')
manual = activity.index('root.addView(manualPanel, spacedFullWidth())')
output = activity.index('root.addView(buildLiveCard(), spacedFullWidth())')
extra = activity.index('root.addView(extraBrightnessPanel, spacedFullWidth())')
req(mode < auto < manual < output < extra, 'Current Output must follow mode-specific controls and precede Extra Bright')
req('shouldShowExtraBrightness' in activity, 'conditional Extra Bright UI policy missing')
req('manualBrightnessSeekBar.getProgress() + 1 == BrightnessLevelConfig.MAX_LEVEL' in activity, 'Manual Extra Bright must require Bright position')
req('currentPreset == AodPreset.BRIGHT' in activity, 'Automatic Extra Bright UI must require BRIGHT preset')
req('settings.getPreset() != AodPreset.BRIGHT' in policy, 'runtime Automatic HBM must reject non-BRIGHT presets')
for text in ['Dim','Balanced','Bright','Low','Medium','Max']:
    req(f'>{text}</string>' in strings, f'missing user-friendly level label: {text}')
for text in ['DIM','BALANCED','BRIGHT']:
    req(f'>{text}</string>' in strings, f'missing uppercase Automatic preset label: {text}')
req('RelativeSizeSpan' in activity, 'Automatic preset subtitle must be smaller than main label')
req('View divider = new View(this)' not in activity, 'action bar divider must be removed')
req('versionCode = 18' in gradle and 'versionName = "1.6.0"' in gradle, 'release metadata must be v1.6.0/code18')
print('PASS v1.6.0 settings UX source requirements')
