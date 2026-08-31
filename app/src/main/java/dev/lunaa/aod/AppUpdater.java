package dev.lunaa.aod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppUpdater {
    private static final String TAG = "LunaaAODUpdater";
    public static final String GITHUB_REPO = "github-lover-9999/LunaaAdaptiveAod";
    public static final String RELEASES_API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static final class ReleaseInfo {
        public final String tagName;
        public final String title;
        public final String changelog;
        public final String apkDownloadUrl;
        public final String apkName;
        public final boolean hasUpdate;

        public ReleaseInfo(String tagName, String title, String changelog, String apkDownloadUrl, String apkName, boolean hasUpdate) {
            this.tagName = tagName;
            this.title = title;
            this.changelog = changelog;
            this.apkDownloadUrl = apkDownloadUrl;
            this.apkName = apkName;
            this.hasUpdate = hasUpdate;
        }
    }

    public interface CheckCallback {
        void onSuccess(ReleaseInfo releaseInfo);
        void onError(String message);
    }

    public interface DownloadCallback {
        void onProgress(int percent);
        void onDownloaded(File apkFile);
        void onError(String message);
    }

    private static void postToMain(Runnable r) {
        try {
            Looper looper = Looper.getMainLooper();
            if (looper != null) {
                new Handler(looper).post(r);
                return;
            }
        } catch (Throwable ignored) {}
        r.run();
    }

    public static void checkForUpdates(final Context context, final CheckCallback callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String jsonStr = fetchReleaseJson(RELEASES_API_URL);
                    if (jsonStr == null || jsonStr.isEmpty()) {
                        postError(callback, "Unable to reach GitHub (check connection or Private DNS)");
                        return;
                    }

                    final ReleaseInfo releaseInfo = parseReleaseJson(jsonStr, AodReleaseInfo.VERSION_NAME, AodReleaseInfo.VERSION_CODE);
                    postToMain(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onSuccess(releaseInfo);
                        }
                    });
                } catch (final Throwable t) {
                    try {
                        Log.w(TAG, "Update check failed", t);
                    } catch (Throwable ignored) {}
                    postError(callback, "Failed to check for updates: " + t.getMessage());
                }
            }
        });
    }

    private static String fetchReleaseJson(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "LunaaAdaptiveAod-Updater");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(10_000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                reader.close();
                return sb.toString();
            }
        } catch (Throwable t) {
            try {
                Log.w(TAG, "Standard HTTP check failed: " + t.getMessage() + ", trying root curl fallback");
            } catch (Throwable ignored) {}
        } finally {
            if (conn != null) conn.disconnect();
        }

        return fetchJsonViaRoot(urlString);
    }

    private static String fetchJsonViaRoot(String urlString) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "su", "-c", "curl -s -L -H 'User-Agent: LunaaAdaptiveAod-Updater' -H 'Accept: application/vnd.github.v3+json' \"" + urlString + "\""
            });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            process.waitFor();
            String result = sb.toString().trim();
            if (result.startsWith("{") && result.endsWith("}")) {
                return result;
            }
        } catch (Throwable t) {
            try {
                Log.w(TAG, "Root curl check failed", t);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static ReleaseInfo parseReleaseJson(String jsonStr, String currentVersionName, int currentVersionCode) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return new ReleaseInfo("", "", "", null, "", false);
        }

        String tagName = extractJsonField(jsonStr, "tag_name");
        String title = extractJsonField(jsonStr, "name");
        if (title.isEmpty()) title = tagName;
        String changelog = extractJsonField(jsonStr, "body");

        String apkDownloadUrl = null;
        String apkName = "LunaaAdaptiveAod-update.apk";

        Matcher assetMatcher = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+\\.apk)\"[\\s\\S]*?\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").matcher(jsonStr);
        if (assetMatcher.find()) {
            apkName = assetMatcher.group(1);
            apkDownloadUrl = assetMatcher.group(2);
        } else {
            Matcher downloadMatcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"").matcher(jsonStr);
            if (downloadMatcher.find()) {
                apkDownloadUrl = downloadMatcher.group(1);
                apkName = apkDownloadUrl.substring(apkDownloadUrl.lastIndexOf('/') + 1);
            }
        }

        boolean isNewer = isNewerVersion(currentVersionName, currentVersionCode, tagName, changelog);
        return new ReleaseInfo(tagName, title, changelog, apkDownloadUrl, apkName, isNewer);
    }

    private static String extractJsonField(String json, String field) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"((?:\\\\\"|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            return value.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return "";
    }

    public static boolean isNewerVersion(String currentVersionName, int currentVersionCode, String remoteTag, String changelog) {
        if (remoteTag == null || remoteTag.isEmpty()) return false;
        String cleanRemote = remoteTag.startsWith("v") ? remoteTag.substring(1) : remoteTag;
        String cleanLocal = currentVersionName.startsWith("v") ? currentVersionName.substring(1) : currentVersionName;

        if (cleanRemote.equalsIgnoreCase(cleanLocal)) {
            return false;
        }

        String[] remoteParts = cleanRemote.split("[.-]");
        String[] localParts = cleanLocal.split("[.-]");

        int length = Math.max(remoteParts.length, localParts.length);
        for (int i = 0; i < length; i++) {
            String r = i < remoteParts.length ? remoteParts[i] : "0";
            String l = i < localParts.length ? localParts[i] : "0";

            int rNum = parseLeadingInt(r);
            int lNum = parseLeadingInt(l);
            if (rNum != lNum) {
                return rNum > lNum;
            }

            String rSuffix = r.replaceAll("^[0-9]+", "");
            String lSuffix = l.replaceAll("^[0-9]+", "");
            int suffixCompare = rSuffix.compareToIgnoreCase(lSuffix);
            if (suffixCompare != 0) {
                return suffixCompare > 0;
            }
        }
        return false;
    }

    private static int parseLeadingInt(String s) {
        try {
            Matcher m = Pattern.compile("^([0-9]+)").matcher(s);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    public static void downloadApk(final Context context, final String downloadUrl, final String targetName, final DownloadCallback callback) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                File dir = new File(context.getCacheDir(), "updates");
                if (!dir.exists()) dir.mkdirs();
                final File targetFile = new File(dir, targetName != null ? targetName : "update.apk");

                HttpURLConnection conn = null;
                InputStream in = null;
                FileOutputStream out = null;
                boolean standardSuccess = false;
                try {
                    URL url = new URL(downloadUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "LunaaAdaptiveAod-Updater");
                    conn.setConnectTimeout(10_000);
                    conn.setReadTimeout(20_000);

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == 307 || code == 308) {
                        String newUrl = conn.getHeaderField("Location");
                        conn.disconnect();
                        url = new URL(newUrl);
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestProperty("User-Agent", "LunaaAdaptiveAod-Updater");
                    }

                    int totalBytes = conn.getContentLength();
                    in = conn.getInputStream();
                    out = new FileOutputStream(targetFile);

                    byte[] buffer = new byte[8192];
                    int read;
                    int downloaded = 0;
                    int lastPercent = 0;

                    while ((read = in.read(buffer)) != -1 && read > 0) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        if (totalBytes > 0) {
                            final int percent = (int) ((downloaded * 100L) / totalBytes);
                            if (percent != lastPercent) {
                                lastPercent = percent;
                                postToMain(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (callback != null) callback.onProgress(percent);
                                    }
                                });
                            }
                        }
                    }
                    out.flush();
                    standardSuccess = true;
                } catch (final Throwable t) {
                    try {
                        Log.w(TAG, "Standard download failed: " + t.getMessage() + ", attempting root curl download");
                    } catch (Throwable ignored) {}
                } finally {
                    try { if (in != null) in.close(); } catch (Throwable ignored) {}
                    try { if (out != null) out.close(); } catch (Throwable ignored) {}
                    if (conn != null) conn.disconnect();
                }

                if (!standardSuccess) {
                    standardSuccess = downloadViaRoot(downloadUrl, targetFile);
                }

                if (standardSuccess && targetFile.exists() && targetFile.length() > 0) {
                    postToMain(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onDownloaded(targetFile);
                        }
                    });
                } else {
                    postToMain(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) callback.onError("Download failed via both standard HTTP and root fallback");
                        }
                    });
                }
            }
        });
    }

    private static boolean downloadViaRoot(String downloadUrl, File targetFile) {
        try {
            String command = "curl -s -L -H 'User-Agent: LunaaAdaptiveAod-Updater' \"" + downloadUrl + "\" -o \"" + targetFile.getAbsolutePath() + "\" && chmod 644 \"" + targetFile.getAbsolutePath() + "\"";
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exit = process.waitFor();
            return exit == 0 && targetFile.exists() && targetFile.length() > 1000;
        } catch (Throwable t) {
            try {
                Log.e(TAG, "Root curl download failed", t);
            } catch (Throwable ignored) {}
            return false;
        }
    }

    public static boolean installApkWithRoot(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) return false;
        try {
            String apkPath = apkFile.getAbsolutePath();
            String command = "pm install -r \"" + apkPath + "\" && "
                    + "APK_PATH=$(pm path dev.lunaa.aod | head -n 1 | cut -d: -f2) && "
                    + "sqlite3 /data/adb/lspd/config/modules_config.db \"UPDATE modules SET apk_path='$APK_PATH' WHERE module_pkg_name='dev.lunaa.aod';\" && "
                    + "killall com.android.systemui";

            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exit = process.waitFor();
            return exit == 0;
        } catch (Throwable t) {
            try {
                Log.w(TAG, "Root install attempt failed; falling back to PackageInstaller", t);
            } catch (Throwable ignored) {}
            return false;
        }
    }

    public static void startSystemInstall(Activity activity, File apkFile) {
        if (activity == null || apkFile == null || !apkFile.exists()) return;
        try {
            Uri contentUri = Uri.parse("content://dev.lunaa.aod.fileprovider/" + apkFile.getName());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (Throwable t) {
            try {
                Log.e(TAG, "Standard PackageInstaller intent failed", t);
            } catch (Throwable ignored) {}
        }
    }

    private static void postError(final CheckCallback callback, final String message) {
        postToMain(new Runnable() {
            @Override
            public void run() {
                if (callback != null) callback.onError(message);
            }
        });
    }
}