from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
def read(rel): return (ROOT / rel).read_text(encoding='utf-8')
def req(cond, msg):
    if not cond: raise AssertionError(msg)

profile = read('app/src/main/java/dev/lunaa/aod/AutomaticBrightnessProfile.java')
policy = read('app/src/main/java/dev/lunaa/aod/ExtraBrightnessPolicy.java')
resolver = read('app/src/main/java/dev/lunaa/aod/RuntimeFieldResolver.java')
hooks = read('app/src/main/java/dev/lunaa/aod/SystemUiHooks.java')
probe = read('app/src/main/java/dev/lunaa/aod/HbmCapabilityProbe.java')
identity = read('app/src/main/java/dev/lunaa/aod/LunaaDevicePolicy.java')
receiver = read('app/src/main/java/dev/lunaa/aod/RootHbmBridgeReceiver.java')
collector = read('tools/collect-aod-qa.ps1')
gradle = read('app/build.gradle.kts')

req('0.03f' in profile and '0.08f' in profile and '0.18f' in profile, 'automatic preset floors missing')
req('ENABLE_LUX = 1_500f' in policy and 'DISABLE_LUX = 700f' in policy, 'auto HBM lux hysteresis incorrect')
req('ENABLE_DWELL_MS = 600L' in policy and 'DISABLE_DWELL_MS = 1_500L' in policy, 'auto HBM dwell incorrect')
req('readExactOrUniqueAssignable' in resolver and 'matches > 1' in resolver, 'ambiguous runtime-field fallback must fail closed')
req('resetBrightnessToDefault hook unavailable; continuing without reset hook' in hooks, 'optional reset hook fallback missing')
req('LunaaDevicePolicy.isSupportedIdentity' in probe, 'device identity gate missing')
for path in ['/sys/kernel/oplus_display/notify_fppress','/sys/kernel/oplus_display/dimlayer_hbm','/sys/class/backlight/panel0-backlight/brightness']:
    req(path in probe, f'missing capability path {path}')
req(receiver.index('HbmCapabilityProbe.probeViaRoot') < receiver.index('runRootWrite('), 'capability probe must precede first HBM write')
req('hbm_mode' not in receiver and 'hbm_mode' not in probe, 'generic Axion HBM node must not be written')
for prop in ['ro.product.device','ro.product.model','ro.product.manufacturer','ro.crdroid.version','ro.axion.version','ro.lineage.version']:
    req(prop in collector, f'QA collector missing {prop}')
req('aod_settings.xml' in collector and 'capability-' in collector, 'QA prefs/capability capture missing')
req('versionCode = 18' in gradle and 'versionName = "1.6.0"' in gradle, 'release metadata must be v1.6.0/code18')
print('PASS v1.6.0 compatibility requirements')
