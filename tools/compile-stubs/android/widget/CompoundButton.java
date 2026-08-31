package android.widget;
import android.content.Context;
public class CompoundButton extends Button {
    public CompoundButton(Context context){super(context);}
    public void setChecked(boolean checked){}
    public boolean isChecked(){return false;}
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener){}
    public interface OnCheckedChangeListener { void onCheckedChanged(CompoundButton buttonView, boolean isChecked); }
}
