package dev.lunaa.aod;

import android.app.BroadcastOptions;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

final class RootHbmBridgeClient {
    private static final String TAG = "LunaaAOD";
    static final String ACTION_ENABLE_EDGE = "dev.lunaa.aod.action.ROOT_HBM_ENABLE_EDGE";
    static final String ACTION_RESET = "dev.lunaa.aod.action.ROOT_HBM_RESET";
    static final String TARGET_PACKAGE = "dev.lunaa.aod";
    static final String TARGET_RECEIVER = "dev.lunaa.aod.RootHbmBridgeReceiver";
    static final int RESULT_SUCCESS = 1;
    static final int RESULT_FAILURE = 0;

    interface Callback {
        void onResult(boolean success, String detail);
    }

    private final Context context;
    private final Handler handler;

    RootHbmBridgeClient(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    boolean requestEnableEdge(Callback callback) {
        return request(ACTION_ENABLE_EDGE, callback);
    }

    boolean requestReset(Callback callback) {
        return request(ACTION_RESET, callback);
    }

    private boolean request(String action, Callback callback) {
        if (context == null || handler == null) {
            if (callback != null) callback.onResult(false, "context-unavailable");
            return false;
        }

        Intent intent = new Intent(action)
                .setComponent(new ComponentName(TARGET_PACKAGE, TARGET_RECEIVER))
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        BroadcastOptions options = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            options = BroadcastOptions.makeBasic().setShareIdentityEnabled(true);
        }
        BroadcastReceiver resultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ignored, Intent resultIntent) {
                boolean ok = getResultCode() == RESULT_SUCCESS;
                String detail = getResultData();
                Log.i(TAG, "extraBright rootBridge result=" + ok + " detail=" + detail);
                if (callback != null) callback.onResult(ok, detail);
            }
        };

        try {
            if (options != null) {
                context.sendOrderedBroadcast(
                        intent,
                        null,
                        options.toBundle(),
                        resultReceiver,
                        handler,
                        RESULT_FAILURE,
                        null,
                        null
                );
            } else {
                context.sendOrderedBroadcast(
                        intent,
                        null,
                        resultReceiver,
                        handler,
                        RESULT_FAILURE,
                        null,
                        null
                );
            }
            Log.i(TAG, "extraBright rootBridge dispatched");
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "extraBright rootBridge dispatch failed", t);
            if (callback != null) callback.onResult(false, "dispatch-failed");
            return false;
        }
    }
}
