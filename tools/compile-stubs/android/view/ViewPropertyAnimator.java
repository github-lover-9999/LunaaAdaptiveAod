package android.view;
public class ViewPropertyAnimator {
    public ViewPropertyAnimator scaleX(float value){return this;}
    public ViewPropertyAnimator scaleY(float value){return this;}
    public ViewPropertyAnimator alpha(float value){return this;}
    public ViewPropertyAnimator setDuration(long duration){return this;}
    public ViewPropertyAnimator withEndAction(Runnable action){return this;}
    public void cancel(){}
    public void start(){}
}
