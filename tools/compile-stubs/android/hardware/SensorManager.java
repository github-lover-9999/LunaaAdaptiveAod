package android.hardware;
import android.os.Handler;
public class SensorManager {
    public static final int SENSOR_DELAY_NORMAL=3;
    public Sensor getDefaultSensor(int type){return null;}
    public Sensor getDefaultSensor(int type, boolean wakeUp){return null;}
    public boolean registerListener(SensorEventListener l,Sensor s,int d){return false;}
    public boolean registerListener(SensorEventListener l,Sensor s,int d,Handler h){return false;}
    public void unregisterListener(SensorEventListener l){}
}
