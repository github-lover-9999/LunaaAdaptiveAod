from pathlib import Path
root = Path(__file__).resolve().parents[1]
controller = (root/'app/src/main/java/dev/lunaa/aod/OplusExtraBrightnessController.java').read_text()
collector = (root/'tools/collect-aod-qa.ps1').read_text()
gradle = (root/'app/build.gradle.kts').read_text()

def req(cond, msg):
    if not cond:
        raise AssertionError(msg)

req('STOCK_UDFPS_HANDOFF_MS = 250L' in controller, 'handoff must be 250 ms')
req('5_000L' not in controller.split('ROOT_ENABLE_RESULT_TIMEOUT_MS')[0], 'old 5s handoff must be gone')
req('[switch]$CleanAod' in collector, 'collector must expose -CleanAod')
req('adb logcat -c' in collector or '& adb logcat -c' in collector, 'clean mode must clear logcat')
req('input keyevent 26' in collector, 'clean mode must switch screen off')
req('Start-Sleep -Seconds 18' in collector, 'clean mode must wait while AOD is active')
req('versionCode = 18' in gradle, 'versionCode must be 18')
req('versionName = "1.6.0"' in gradle, 'versionName must be 1.6.0')
print('PASS v1.6.0 handoff source requirements')
