package android.widget;
import android.content.Context;
public class Toast {
    public static final int LENGTH_SHORT=0;
    public static Toast makeText(Context context,int resId,int duration){return new Toast();}
    public void show(){}
}
