from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

def read(rel):
    return (ROOT / rel).read_text(encoding='utf-8')

def require(cond, msg):
    if not cond:
        raise AssertionError(msg)

receiver = read('app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java')
controller = read('app/src/main/java/dev/lunaa/aod/OplusExtraBrightnessController.java')
adaptive = read('app/src/main/java/dev/lunaa/aod/AdaptiveAodController.java')
hooks = read('app/src/main/java/dev/lunaa/aod/SystemUiHooks.java')
dim = read('app/src/main/java/dev/lunaa/aod/ExtraBrightnessDimLayer.java')
reader = read('app/src/main/java/dev/lunaa/aod/XposedSettingsReader.java')
defaults = read('app/src/main/java/dev/lunaa/aod/AodSettingsDefaults.java')
codec = read('app/src/main/java/dev/lunaa/aod/AodSettingsCodec.java')
readme = read('README.md')
qa = read('QA.md')

# Root command sequencing must be latest-command-wins so an AOD-exit reset can cancel a stale enable.
require('RootCommandGate' in receiver, 'root bridge has no generation gate for enable/reset races')
require('isCurrent' in receiver, 'root bridge does not re-check command generation before writes')
require('newSingleThreadExecutor' in receiver, 'root bridge should serialize root workers')
require('waitFor(' in receiver and 'TimeUnit' in receiver, 'root subprocess has no bounded wait')
require('Thread.sleep' in receiver, '0->1 edge delay should be controlled by Java, not shell sleep syntax')
require('sleep 0.10' not in receiver, 'shell fractional sleep remains in root edge path')

# Session lifecycle must invalidate callbacks and guarantee cleanup on FINISH/destroy.
require('sessionGeneration' in controller, 'Extra Bright callbacks are not scoped to an AOD session')
require('finishSession' in controller, 'Extra Bright controller has no explicit FINISH cleanup API')
require('policy-off deferred while root edge in flight' in controller, 'policy-off can remove dim layer while HBM edge is still in flight')
require('success && ambientActive' in controller, 'successful HBM edge is not latched independently of late policy changes')
require('ROOT_ENABLE_RESULT_TIMEOUT_MS' in controller and 'enableResultWatchdog' in controller,
        'lost enable bridge result can leave controller in an unbounded in-flight state')
require('ROOT_RESET_RESULT_TIMEOUT_MS' in controller and 'cleanupResultWatchdog' in controller,
        'lost reset bridge result can leave the dim layer stuck after AOD exit')
require('cleanup reset failed; protective dim layer retained for stock exit' in controller,
        'failed reset removes the protective dim layer before stock display exit is guaranteed')
require('extraBrightnessController.finishSession()' in adaptive, 'Doze FINISH/destroy does not force Extra Bright session cleanup')

# Reuse the already-proven SystemUI context instead of reintroducing ActivityThread fallback.
require('ActivityThread' not in dim and 'currentApplication' not in dim,
        'dim layer reintroduced the ActivityThread.currentApplication fallback removed in v1.1')
require('ExtraBrightnessDimLayer(Context' in dim, 'dim layer does not receive validated SystemUI Context')
require('keeping existing protective layer' in dim,
        'dim-layer update failure can remove an already-active protective layer after HBM starts')
require('extraBright context unavailable; normal adaptive AOD remains active' in hooks,
        'missing hidden SystemUI Context still aborts the entire adaptive controller instead of only Extra Bright')

# Automatic HBM must not use indefinitely stale lux.
require('lastObservedLuxMs' in adaptive, 'Extra Bright lux has no timestamp')
require('EXTRA_BRIGHTNESS_MAX_LUX_AGE_MS' in adaptive, 'Extra Bright has no stale-lux cutoff')

# Read/parse failures must fall back to stock, not silently enable Balanced control.
require('disabledBalanced' in defaults, 'no disabled safe settings snapshot exists')
require('disabledBalanced' in reader, 'Xposed settings read failure does not fail closed')
require('disabledBalanced' in codec, 'settings codec corruption does not fail closed')

# Docs must not preserve already disproved assumptions.
require('UID 1000 / `com.android.systemui`' not in readme, 'README still claims fixed UID 1000 validation')
require('extraBright=true via=direct' not in qa and 'extraBright=true via=su' not in qa,
        'QA still expects obsolete pre-root-bridge log format')

print('PASS pre-release audit requirements')
