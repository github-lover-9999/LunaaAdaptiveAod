package android.view;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
public class View {
    public static final int VISIBLE=0;
    public static final int GONE=8;
    public View(Context context){}
    public void setPadding(int left,int top,int right,int bottom){}
    public void setEnabled(boolean enabled){}
    public void setBackground(Drawable background){}
    public void setBackgroundColor(int color){}
    public void setMinHeight(int minHeight){}
    public void setMinimumHeight(int minHeight){}
    public void setContentDescription(CharSequence contentDescription){}
    public void setVisibility(int visibility){}
    public int getVisibility(){return VISIBLE;}
    public Resources getResources(){return null;}
    public ViewPropertyAnimator animate(){return new ViewPropertyAnimator();}
    public void setOnApplyWindowInsetsListener(OnApplyWindowInsetsListener listener){}
    public void requestApplyInsets(){}
    public interface OnClickListener { void onClick(View v); }
    public interface OnApplyWindowInsetsListener { WindowInsets onApplyWindowInsets(View v,WindowInsets insets); }
}
