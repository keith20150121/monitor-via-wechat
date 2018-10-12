package com.keith.wechat.monitor;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.util.Log;

import com.keith.wechat.monitor.feature.FlashLight;

import java.util.LinkedList;

public class CommandParser implements MonitorEntrance.TextMessageReceiver {
    private final LinkedList<String> mContentList = new LinkedList<>();

    private static final String TAG = "no-man-duty-cp";

    public static final int NOT_INIT = 0;
    public static final int EXIST = 1;
    public static final int NON_EXIST = 2;

    public final static String CMD_FLASH_LIGHT = "flashlight ";

    public final static String SWITCH_ON = "on";
    public final static String SUCCEEDED = "SUCCEEDED";
    public final static String FAIL = "FAILED";

    private FlashLight mFlashLightController;

    private boolean setFlashLight(boolean on) {
        if (null == mFlashLightController) {
            MonitorEntrance service = MonitorEntrance.getService();
            if (null == service) return false;
            mFlashLightController = new FlashLight(service);
        }
        return mFlashLightController.setFlashlight(on);
    }

    @Override
    public String onTextMessageReceived(String content, String id) {
        if (null == content) return null;
        mContentList.push(content);

        if (content.startsWith(CMD_FLASH_LIGHT)) {
             return setFlashLight(content.endsWith(SWITCH_ON)) ? SUCCEEDED : FAIL;
        }
        return null;
    }
}
