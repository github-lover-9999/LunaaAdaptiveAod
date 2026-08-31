package android.content;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
public class Context {
    @Deprecated public static final int MODE_WORLD_READABLE=1;
    public static final int MODE_PRIVATE=0;
    public static final String SENSOR_SERVICE="sensor";
    public static final String WINDOW_SERVICE="window";
    public <T> T getSystemService(Class<T> serviceClass){return null;}
    public Object getSystemService(String name){return null;}
    public SharedPreferences getSharedPreferences(String name,int mode){return null;}
    public Resources getResources(){return null;}
    public PackageManager getPackageManager(){return null;}
    public String getString(int resId){return null;}
    public String getString(int resId,Object... formatArgs){return null;}
    public int getDisplayId(){return 0;}
    public void sendOrderedBroadcast(Intent intent, String receiverPermission, Bundle options,
            BroadcastReceiver resultReceiver, Handler scheduler, int initialCode,
            String initialData, Bundle initialExtras){}
}
