package android.widget;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.View;
public class SeekBar extends View {
    public SeekBar(Context context){super(context);}
    public void setMax(int max){}
    public int getProgress(){return 0;}
    public void setProgress(int progress){}
    public void setProgressTintList(ColorStateList tint){}
    public void setThumbTintList(ColorStateList tint){}
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener){}
    public interface OnSeekBarChangeListener {
        void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser);
        void onStartTrackingTouch(SeekBar seekBar);
        void onStopTrackingTouch(SeekBar seekBar);
    }
}
