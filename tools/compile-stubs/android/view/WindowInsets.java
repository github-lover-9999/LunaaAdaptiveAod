package android.view;
import android.graphics.Insets;
public class WindowInsets {
    public Insets getInsets(int typeMask){return null;}
    public static final class Type {
        private Type(){}
        public static int systemBars(){return 1;}
        public static int displayCutout(){return 2;}
        public static int ime(){return 4;}
    }
}
