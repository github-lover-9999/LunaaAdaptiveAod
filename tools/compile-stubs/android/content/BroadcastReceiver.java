package android.content;
public abstract class BroadcastReceiver {
    public abstract void onReceive(Context context, Intent intent);
    public int getResultCode(){return 0;}
    public String getResultData(){return null;}
    public void setResultCode(int code){}
    public void setResultData(String data){}
    public int getSentFromUid(){return -1;}
    public String getSentFromPackage(){return null;}
    public PendingResult goAsync(){return new PendingResult();}
    public static class PendingResult {
        public void setResultCode(int code){}
        public void setResultData(String data){}
        public void finish(){}
    }
}
