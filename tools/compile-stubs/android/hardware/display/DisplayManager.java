package android.hardware.display;
import android.os.Handler;
import android.view.Display;
public class DisplayManager {
    public interface DisplayListener { void onDisplayAdded(int id); void onDisplayRemoved(int id); void onDisplayChanged(int id); }
    public void registerDisplayListener(DisplayListener l, Handler h){}
    public void unregisterDisplayListener(DisplayListener l){}
    public Display getDisplay(int id){return null;}
    public float getBrightness(int id){return Float.NaN;}
}
