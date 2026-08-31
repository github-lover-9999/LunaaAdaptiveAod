package android.view;
import android.content.Context;
public class ViewGroup extends View {
    public ViewGroup(Context context){super(context);}
    public void addView(View child){}
    public void addView(View child, LayoutParams params){}
    public static class LayoutParams {
        public static final int MATCH_PARENT=-1;
        public static final int WRAP_CONTENT=-2;
        public LayoutParams(int width,int height){}
    }
}
