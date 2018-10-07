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
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.keith.wechat.monitor.utility.Shell;

public class MonitorEntrance extends AccessibilityService {
    private static final String TAG = "no-man-duty-entrance";

    public static final String VIDEO_WINDOW = "com.tencent.mm.plugin.voip.ui.VideoActivity";

    //锁屏、解锁相关
    private KeyguardManager mKeyguardMgr;
    private KeyguardLock mKeyguardLock;
    //唤醒屏幕相关
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

    @SuppressLint("NewApi")
    private boolean fill() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            return findEditText(rootNode, "正在忙,稍后回复你");
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
            if (nodeInfo.getContentDescription() != null) {
                int nindex = nodeInfo.getContentDescription().toString().indexOf(mSender);
                int cindex = nodeInfo.getContentDescription().toString().indexOf(mContent);
                android.util.Log.e(TAG, "nindex=" + nindex + " cindex=" +cindex);
                if (nindex != -1) {
                    mSenderInfo = nodeInfo;
                    android.util.Log.i(TAG, "find node info");
                }
            }
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

    //唤醒屏幕和解锁
    /*private void wakeAndUnlock(boolean unLock) {
        if (unLock)
        {
            //若为黑屏状态则唤醒屏幕
            if(!mPowerMgr.isScreenOn()) {
                //获取电源管理器对象，ACQUIRE_CAUSES_WAKEUP这个参数能从黑屏唤醒屏幕
               // mWakeupLock = mPowerMgr.newWakeLock(
               //         PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                //点亮屏幕
                mWakeupLock.acquire();
                Log.i(TAG, "亮屏");
            }
            //若在锁屏界面则解锁直接跳过锁屏
            if(mKeyguardMgr.inKeyguardRestrictedInputMode()) {
                //设置解锁标志，以判断抢完红包能否锁屏
                enableKeyguard = false;
                //解锁
                mKeyguardLock.disableKeyguard();
                Log.i(TAG, "解锁");
            }
        }
        else
        {
            //如果之前解过锁则加锁以恢复原样
            if(!enableKeyguard) {
                //锁屏
                mKeyguardLock.reenableKeyguard();
                Log.i(TAG, "加锁");
            }
            //若之前唤醒过屏幕则释放之使屏幕不保持常亮
            if(mWakeupLock != null) {
                mWakeupLock.release();
                mWakeupLock = null;
                Log.i(TAG, "关灯");
            }
        }
    }*/
    //通过文本查找节点
    public  AccessibilityNodeInfo findNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(text);
        if(list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
    //模拟点击事件
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
    //模拟返回事件
    public  void performBack(AccessibilityService service) {
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
                //launchNotificationActivity(event);
                sendNotificationReply(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {
                if (VIDEO_WINDOW.equals(className)) {
                    pickUp();
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
        Toast.makeText(this, "抢红包服务被中断啦~", Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "开启");
        //获取电源管理器对象
        mPowerMgr =(PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeupLock = mPowerMgr.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        //得到键盘锁管理器对象
        mKeyguardMgr = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        //初始化一个键盘锁管理器对象
        mKeyguardLock = mKeyguardMgr.newKeyguardLock("unLock");
        //初始化音频
        //player = MediaPlayer.create(this, R.raw.songtip_m);

        Toast.makeText(this, "_已开启抢红包服务_", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "关闭");
        //wakeAndUnlock(false);
        Toast.makeText(this, "_已关闭抢红包服务_", Toast.LENGTH_LONG).show();
    }
}
