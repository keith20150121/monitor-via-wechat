package com.keith.wechat.monitor;

import java.util.Calendar;
import java.util.List;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.keith.wechat.monitor.utility.Shell;

public class MonitorEntrance extends AccessibilityService {
    private static final String TAG = "no-man-duty-entrance";

    public static final String VIDEO_WINDOW = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    public static final String CHAT_WINDOW = "com.tencent.mm.ui.LauncherUI";

    private KeyguardManager mKeyguardMgr;
    //private KeyguardLock mKeyguardLock;
    private PowerManager mPowerMgr;
    private PowerManager.WakeLock mWakeupLock = null;

    private String mSender;
    private AccessibilityNodeInfo mSenderInfo;
    private String mContent;

    private final Runnable mDelayReleaseWakeLock = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mWakeupLock.isHeld()) {
                mWakeupLock.release();
            }
        }
    };

    private void justWakeupAndRelease() {
        if (mWakeupLock.isHeld()) return;
        mWakeupLock.acquire(400);
        AsyncTask.execute(mDelayReleaseWakeLock);
    }

    /**
     *
     * @param event
     */
    private void sendNotificationReply(AccessibilityEvent event) {
        if (event.getParcelableData() != null
                && event.getParcelableData() instanceof Notification) {
            Notification notification = (Notification) event
                    .getParcelableData();
            String content = notification.tickerText.toString();
            String[] cc = content.split(":");
            mSender = cc[0].trim();
            mContent = cc[1].trim();

            android.util.Log.i(TAG, "sender name =" + mSender);
            android.util.Log.i(TAG, "sender content =" + mContent);


            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    private void send() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        final String go = getResources().getString(R.string.wechat_send_message);
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByText(go);
            if (list != null && list.size() > 0) {
                for (AccessibilityNodeInfo n : list) {
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }

            }
            //pressBackButton();
        }

    }


    @SuppressLint("NewApi")
    private boolean fill(String content) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, content);
        }
        return false;
    }


    private boolean findEditText(AccessibilityNodeInfo rootNode, String content) {
        int count = rootNode.getChildCount();

        android.util.Log.d(TAG, "root class=" + rootNode.getClassName() + ","+ rootNode.getText()+","+count);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = rootNode.getChild(i);
            if (nodeInfo == null) {
                android.util.Log.d(TAG, "nodeinfo = null");
                continue;
            }

            android.util.Log.d(TAG, "class=" + nodeInfo.getClassName());
            android.util.Log.e(TAG, "ds=" + nodeInfo.getContentDescription());
            /*if (nodeInfo.getContentDescription() != null) {
                int nindex = nodeInfo.getContentDescription().toString().indexOf(mSender);
                int cindex = nodeInfo.getContentDescription().toString().indexOf(mContent);
                android.util.Log.e(TAG, "nindex=" + nindex + " cindex=" +cindex);
                if (nindex != -1) {
                    mSenderInfo = nodeInfo;
                    android.util.Log.i(TAG, "find node info");
                }
            }*/
            if ("android.widget.EditText".contentEquals(nodeInfo.getClassName())) {
                android.util.Log.i(TAG, "==================");
                Bundle arguments = new Bundle();
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                        AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                        true);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                        arguments);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                ClipData clip = ClipData.newPlainText("label", content);
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboardManager.setPrimaryClip(clip);
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                return true;
            }

            if (findEditText(nodeInfo, content)) {
                return true;
            }
        }

        return false;
    }

    //播放提示声音
    private MediaPlayer player;
    public void playSound(Context context) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        //夜间不播放提示音
        if(hour > 7 && hour < 22) {
            player.start();
        }
    }

    public  AccessibilityNodeInfo findNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if(list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public void performClick(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) {
            return;
        }
        if (nodeInfo.isClickable()) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        } else {
            performClick(nodeInfo.getParent());
        }
    }

    public void performBack(AccessibilityService service) {
        if(service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }

    private void launchNotificationActivity(AccessibilityEvent event) {
        try {
            Notification notification = (Notification) event.getParcelableData();
            PendingIntent pendingIntent = notification.contentIntent;
            pendingIntent.send();
        } catch (CanceledException e) {
            e.printStackTrace();
        }
    }

    //实现辅助功能
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String className = null;
        if (null != event.getClassName()) {
            className = event.getClassName().toString();
        }
        Log.i(TAG, String.format("type:%d, class:%s", eventType, className));
        justWakeupAndRelease();
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                Log.d(TAG, "texts:" + texts.toString());
                // Leave it to NotificationListener
                //sendNotificationReply(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {
                if (VIDEO_WINDOW.equals(className)) {
                    pickUp();
                } else if (CHAT_WINDOW.equals(className)) {
                    if (fill("等一下")) {
                        send();
                    }
                }
            }
            break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: {
                if (VIDEO_WINDOW.equals(className)) {
                    //switchCamera();
                }
            }
            break;
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
                pickUp();
                break;
        }
    }

    private boolean switchCamera() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            return false;
        }
        Resources r = getResources();
        final String switchCamera = r.getString(R.string.wechat_switch_camera);
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(switchCamera);
        if (null != list && list.size() > 0) {
            Log.d(TAG, "switch Camera found!" + list.get(0).getViewIdResourceName());
            performClick(list.get(0));
            return true;
        }
        return false;
    }

    private boolean pickUpInternal() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            return false;
        }

        Resources r = getResources();
        final String accept = r.getString(R.string.wechat_accept_call);

        /*List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/cfx");
        if (null != list && list.size() > 0) {
            Log.d(TAG, "Accept found!" + list.get(0).getViewIdResourceName());
            performClick(list.get(0));
            return true;
        }*/
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(accept);
        if (null != list && list.size() > 0) {
            AccessibilityNodeInfo ani = list.get(0);
            Log.d(TAG, String.format("Accept found! id:%s, class:%s",
                    ani.getViewIdResourceName(), ani.getClassName()));
            performClick(list.get(0));
            return true;
        }
        return false;
    }

    private final Runnable mDelayAcceptCall = new Runnable() {
        @Override
        public void run() {
            Shell.exec("input touchscreen tap 0 0");
            /*try {
                Thread.sleep(200);
            } catch (Exception e) {
                Log.d(TAG, "sleep action is interrupted?");
                e.printStackTrace();
            }

            if (!MonitorEntrance.this.pickUpInternal()) {
                Log.d(TAG, "looking for Accept button...");
                AsyncTask.execute(mDelayAcceptCall);
            }*/
        }
    };

    private void pickUp() {
        pickUpInternal();
        /*if (!pickUpInternal()) {
            AsyncTask.execute(mDelayAcceptCall);
        }*/
    }

    public void ensureNotificationListenerAuthority() {
        String string = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners"); // Settings.ENABLED_NOTIFICATION_LISTENERS
        if (null == string || !string.contains(NotificationListener.class.getName())) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    //打开红包
    @SuppressLint("NewApi")
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null) {
            return;
        }

        Log.i(TAG, "查找打开按钮...");
        AccessibilityNodeInfo targetNode = null;

        //如果红包已经被抢完则直接返回
        targetNode = findNodeInfosByText(nodeInfo, "看看大家的手气");
        if(targetNode != null) {
            performBack(this);
            return;
        }
        //通过组件名查找开红包按钮，还可通过组件id直接查找但需要知道id且id容易随版本更新而变化，旧版微信还可直接搜“開”字找到按钮
        if(targetNode == null) {
            Log.i(TAG, "打开按钮中...");
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                AccessibilityNodeInfo node = nodeInfo.getChild(i);
                if("android.widget.Button".equals(node.getClassName())) {
                    targetNode = node;
                    break;
                }
            }
        }
        //若查找到打开按钮则模拟点击
        if(targetNode != null) {
            final AccessibilityNodeInfo n = targetNode;
            performClick(n);
        }
    }

    @Override
    public void onInterrupt() {
        String interrupted = getResources().getString(R.string.robot_is_interrupted);
        Toast.makeText(this, interrupted, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        String on = getResources().getString(R.string.robot_is_on);
        Log.i(TAG, on);
        mPowerMgr =(PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeupLock = mPowerMgr.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        mKeyguardMgr = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        //mKeyguardLock = mKeyguardMgr.newKeyguardLock("unLock");
        //player = MediaPlayer.create(this, R.raw.songtip_m);
        ensureNotificationListenerAuthority();
        Toast.makeText(this, on, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        String off = getResources().getString(R.string.robot_is_off);
        Log.i(TAG, off);
        Toast.makeText(this, off, Toast.LENGTH_LONG).show();
    }
}
