from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def require(cond, msg):
    if not cond:
        raise AssertionError(msg)

controller = read('app/src/main/java/dev/lunaa/aod/OplusExtraBrightnessController.java')
policy = read('app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java')
codec = read('app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java')
snapshot = read('app/src/main/java/dev/lunaa/aod/AodSettingsSnapshot.java')
activity = read('app/src/main/java/dev/lunaa/aod/SettingsActivity.java')
gradle = read('app/build.gradle.kts')

require('requestEnableEdge' in controller, 'missing app-root 0->1 edge request')
require('REASSERT_INTERVAL_MS' not in controller, 'unsafe periodic reassert still present')
require('ExtraBrightnessDimLayer' in controller, 'HBM must be gated by dim-layer availability')

require('/sys/kernel/oplus_display/hbm' not in controller, 'must not use generic /hbm node')
require('STOCK_UDFPS_HANDOFF_MS' in controller, 'must wait for stock UDFPS handoff')
require('sessionLatched' in controller and 'deferred until ambient exit' in controller,
        'HBM must stay safely dimmed until ambient exit')
attempt = controller[controller.index('private void attemptEnable()'):controller.index('private void cancelPendingEnable()')]
require(attempt.index('dimLayer.show') < attempt.index('requestEnableEdge'),
        'protective dim layer must attach before the HBM edge')
require('requestReset' in controller and 'dimLayer.hide()' in controller,
        'ambient exit root reset cleanup missing')
require('getManualLevel() == BrightnessLevelConfig.MAX_LEVEL' in policy, 'Manual level-3 HBM eligibility missing')
require('KEY_EXTRA_BRIGHT_PERCENT' in codec, 'extra-bright level is not persisted')
require('getExtraBrightPercent' in snapshot, 'extra-bright level missing from snapshot')
require('extraBrightnessLevelSeekBar' in activity, 'extra-bright level control missing from UI')
require('versionName = "1.6.0"' in gradle, 'release version is not 1.5.6')

level_file = ROOT / 'app/src/main/java/dev/lunaa/aod/ExtraBrightnessLevel.java'
require(level_file.exists(), 'ExtraBrightnessLevel.java missing')
level = level_file.read_text(encoding='utf-8')
require('DEFAULT_PERCENT = AodSettingsDefaults.DEFAULT_EXTRA_LEVEL_2_PERCENT' in level, 'default Extra Bright must follow level-2 mapping')
require('MIN_PERCENT = 1' in level and 'MAX_PERCENT = 100' in level,
        'Extra Bright advanced range must be 1..100%')

layer_file = ROOT / 'app/src/main/java/dev/lunaa/aod/ExtraBrightnessDimLayer.java'
require(layer_file.exists(), 'ExtraBrightnessDimLayer.java missing')
layer = layer_file.read_text(encoding='utf-8')
require('TYPE_NAVIGATION_BAR_PANEL = 2024' in layer, 'must use SystemUI navigation-bar panel layer')
require('overlayAlphaForPercent' in layer, 'dim layer must derive alpha from Extra Bright percent')

print('PASS v1.6.0 source requirements')
