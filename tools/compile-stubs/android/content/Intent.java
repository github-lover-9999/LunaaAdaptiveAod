package android.content;
public class Intent {
    public static final int FLAG_RECEIVER_FOREGROUND=0x10000000;
    private String action;
    public Intent(String action){this.action=action;}
    public Intent setComponent(ComponentName component){return this;}
    public Intent addFlags(int flags){return this;}
    public String getAction(){return action;}
}
