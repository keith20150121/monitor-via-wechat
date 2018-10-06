package com.keith.wechat.monitor;

import android.app.Activity;

import com.keith.wechat.monitor.pages.VideoActivityWatcher;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by swd3 on 10/5/18.
 */

public class SimulateInput implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!"com.tencent.mm".equals(lpparam.packageName)) {
            return;
        }
        XposedHelpers.findAndHookMethod("com.tencent.mm.plugin.voip.ui.VideoActivity",
                lpparam.classLoader, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        Activity thiz = (Activity)param.thisObject;
                        VideoActivityWatcher.getInstance().activate(thiz);
                    }
                });
    }
}
