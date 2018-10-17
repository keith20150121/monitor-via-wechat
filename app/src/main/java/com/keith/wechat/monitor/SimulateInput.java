package com.keith.wechat.monitor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.keith.wechat.monitor.pages.VideoActivityWatcher;
import com.keith.wechat.monitor.utility.Shell;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by swd3 on 10/5/18.
 */

public class SimulateInput implements IXposedHookLoadPackage {

    private final static String TAG = "no-man-tencent";
    public final static String ACTION_SIMULATION = "com.keith.wechat.monitor.INPUT_SIMULATE";

    private final BroadcastReceiver mReceiverFromRobot = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "onReceive!", Toast.LENGTH_SHORT).show();
            if (null == intent) {
                Log.e(TAG, "no intent?");
                return;
            }
            final String action = intent.getAction();
            if (!ACTION_SIMULATION.equals(action)) {
                Log.e(TAG, "not a simulate action?");
                return;
            }
            final String command = intent.getStringExtra("command");
            AsyncTask.execute(new Shell.ExecRunnable(command));
            //final String ret = Shell.exec(command);
            //Log.e(TAG, ret);
        }
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.tencent.mm".equals(lpparam.packageName)) {
            return;
        }
        //XposedHelpers.findAndHookMethod(MonitorEntrance.CHAT_WINDOW,
        XposedHelpers.findAndHookMethod(MonitorEntrance.CHAT_WINDOW,
                lpparam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Activity thiz = (Activity)param.thisObject;
                        IntentFilter intentFilter = new IntentFilter(ACTION_SIMULATION);
                        thiz.getApplicationContext().registerReceiver(mReceiverFromRobot, intentFilter);
                        Log.d(TAG, "receiver registered.");
                        //Toast.makeText(thiz, "registered!", Toast.LENGTH_SHORT).show();
                    }
                });

    }
}
