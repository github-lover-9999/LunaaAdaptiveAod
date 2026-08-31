package android.content.pm;
public class PackageManager {
    public static class NameNotFoundException extends Exception { private static final long serialVersionUID = 1L; }
    public String[] getPackagesForUid(int uid){return null;}
    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {return null;}
}
