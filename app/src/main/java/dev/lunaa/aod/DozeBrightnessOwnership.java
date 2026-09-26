package dev.lunaa.aod;

/**
 * Decides whether the module or stock SystemUI owns the doze brightness.
 *
 * <p>The module owns it only while SystemUI shows AOD with the display in a low-power state.
 * While the panel runs at full power the doze brightness is shown as normal screen brightness:
 * during the screen-off animation (stock keeps the display on for ENTER_DOZE_DELAY, 4 s), during
 * a pulse (UDFPS touch or notification), and on the fingerprint unlock window, which SystemUI
 * forces to the doze brightness. A session therefore starts owned by stock, the module takes
 * the brightness once the display has left full power, and it hands the brightness back before
 * a pulse or FINISH turns the display on again. During the screen-off animation the module only
 * replaces the dim stock default with the normal screen brightness.</p>
 */
final class DozeBrightnessOwnership {
    private boolean aodState;
    private boolean handedBack = true;
    private boolean screenOffAnimation = true;

    /** @return true exactly when the module owned the brightness and must hand it back now */
    boolean onDozeState(String stateName) {
        if (DozeStatePolicy.isAmbientIntentState(stateName)) {
            aodState = true;
            return false;
        }
        if (!DozeStatePolicy.isStockOwnedState(stateName)) return false;
        aodState = false;
        screenOffAnimation = false;
        if (handedBack) return false;
        handedBack = true;
        return true;
    }

    /** @return true when the module owns the brightness after this call */
    boolean reclaim(int displayState) {
        if (handedBack && aodState && DisplayStatePolicy.isLowPowerState(displayState)) {
            handedBack = false;
            screenOffAnimation = false;
        }
        return !handedBack;
    }

    /** @return true while a fresh session still shows the screen-off animation at full power */
    boolean isScreenOffAnimation() {
        return screenOffAnimation && aodState && handedBack;
    }

    boolean isOwnedByModule() {
        return !handedBack;
    }

    /**
     * Brightness for the screen-off animation, which stock shows at its dim default doze
     * brightness: the brightness the screen shows right before locking, so locking neither dims
     * nor brightens the panel before AOD starts. That is below the setting when the screen was
     * dimmed for a timeout. It is capped at the setting because HDR video or an app brightness
     * override does not carry over to the lock screen.
     *
     * @return NaN when neither value is usable, so stock keeps its own brightness
     */
    static float screenOffAnimationBrightness(float shownBrightness, float settingBrightness) {
        float brightness;
        if (isUsable(shownBrightness) && isUsable(settingBrightness)) {
            brightness = Math.min(shownBrightness, settingBrightness);
        } else {
            brightness = isUsable(shownBrightness) ? shownBrightness : settingBrightness;
        }
        if (!isUsable(brightness)) return Float.NaN;
        return Math.max(AodSettingsSnapshot.MIN_BRIGHTNESS,
                Math.min(AodSettingsSnapshot.MAX_BRIGHTNESS, brightness));
    }

    private static boolean isUsable(float brightness) {
        return Float.isFinite(brightness) && brightness >= 0f;
    }

    /**
     * Brightness left for stock SystemUI: never above the AOD the module showed, and never above
     * the normal screen brightness the user sees after unlocking.
     *
     * @return NaN when the module never wrote a brightness, so there is nothing to hand back
     */
    static float handbackBrightness(float moduleBrightness, float screenBrightness) {
        if (Float.isNaN(moduleBrightness)) return Float.NaN;
        float safeScreen = Float.isFinite(screenBrightness) && screenBrightness >= 0f
                ? screenBrightness : AodSettingsSnapshot.MIN_BRIGHTNESS;
        float handback = Math.min(moduleBrightness, safeScreen);
        return Math.max(AodSettingsSnapshot.MIN_BRIGHTNESS,
                Math.min(AodSettingsSnapshot.MAX_BRIGHTNESS, handback));
    }
}
