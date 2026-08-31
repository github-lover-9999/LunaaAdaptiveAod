package android.widget;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
public class EditText extends TextView {
    public EditText(Context context){super(context);}
    public void setHint(int resId){}
    @Override public void setSingleLine(boolean singleLine){}
    public void setInputType(int type){}
    public void addTextChangedListener(TextWatcher watcher){}
    public Editable getText(){return null;}
    public void setError(CharSequence error){}
    public void setSelection(int index){}
    public int length(){return 0;}
}
