package android.view;
public interface WindowManager {
    void addView(View view, ViewGroup.LayoutParams params);
    void updateViewLayout(View view, ViewGroup.LayoutParams params);
    void removeView(View view);
    class LayoutParams extends ViewGroup.LayoutParams {
        public static final int FLAG_NOT_FOCUSABLE=0x00000008;
        public static final int FLAG_NOT_TOUCHABLE=0x00000010;
        public static final int FLAG_LAYOUT_IN_SCREEN=0x00000100;
        public static final int FLAG_LAYOUT_NO_LIMITS=0x00000200;
        public int type; public int flags; public int format; public int gravity; public float alpha=1f;
        public LayoutParams(int w,int h,int type,int flags,int format){ super(w,h); this.type=type; this.flags=flags; this.format=format; }
        public void setTitle(CharSequence title){}
    }
}
