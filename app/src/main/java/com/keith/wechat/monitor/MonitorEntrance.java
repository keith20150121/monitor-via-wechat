package com.keith.wechat.monitor;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.keith.wechat.monitor.utility.sequence.EventStash;
import com.keith.wechat.monitor.utility.WechatManAccessHelper;

public class MonitorEntrance extends AccessibilityService {
    private static final String TAG = "no-man-duty-entrance";

    public static final String VIDEO_WINDOW = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    public static final String TEXT_PREVIEW_WINDOW = "com.tencent.mm.ui.chatting.TextPreviewUI";
    public static final String TEXT_PREVIEW_ID = "com.tencent.mm:id/ann";
    public static final String CHAT_WINDOW = "com.tencent.mm.ui.LauncherUI";

    private PowerManager mPowerMgr;
    private PowerManager.WakeLock mWakeupLock = null;

    private final EventStash mStash = new EventStash();
    private final LinkedList<TextMessageReceiver> mTextMessageReceivers =
            new LinkedList<>();

    private final CommandParser mCommandParser = new CommandParser();

    private final WechatManAccessHelper.LauncherUIChat.LatestMessage mLatestMessage =
            new WechatManAccessHelper.LauncherUIChat.LatestMessage();

    private final WechatManAccessHelper.VideoActivity mVideo =
            new WechatManAccessHelper.VideoActivity();

    public void push(EventStash.ISequence.Callback callback, int... eventTypes) {
        mStash.push(callback, eventTypes);
    }

    public void push(EventStash.ISequence sequence) {
        mStash.push(sequence);
    }

    public void remove(EventStash.ISequence sequence) {
        mStash.remove(sequence);
    }

    public int generateMessageId() {
        return mHandler.generateId();
    }

    public interface TextMessageReceiver {
        String onTextMessageReceived(String content, String id);
    }

    public void registerTextMessageReceiver(TextMessageReceiver receiver) {
        mTextMessageReceivers.push(receiver);
    }

    public void unregisterTextMessageReceiver(TextMessageReceiver receiver) {
        mTextMessageReceivers.remove(receiver);
    }

    public void dispatchTextMessage(String content) {
        Log.d(TAG, "message:" + content);
        StringBuilder sb = new StringBuilder();
        for (TextMessageReceiver receiver : mTextMessageReceivers) {
            final String ret = receiver.onTextMessageReceived(content, ""); // todo
            if (null == ret || ret.isEmpty()) continue;
            sb.append(ret);
            sb.append("\n");
        }
        if (0 < sb.length()) {
            if (!WechatManAccessHelper.LauncherUIChat.sendMessage(this, sb.toString())) {
                final String err = getResources().getString(R.string.robot_cannot_find_edit_textbox);
                Log.e(TAG, err);
                Toast.makeText(this,
                        String.format("%s\n%s", err, sb.toString()), Toast.LENGTH_SHORT).show();
            }
        }
        //Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
    }

    public void executeDelayed(Runnable r, long timeout) {
        executeDelayed(r, MainThreadHandler.MSG_RUNNABLE, timeout);
    }

    public void executeDelayed(Runnable r, int what, long timeout) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = r;
        mHandler.sendMessageDelayed(msg, timeout);
    }

    public void removeMessages(int what) {
        mHandler.removeMessages(what);
    }

    private final Runnable mDelayReleaseWakeLock = new Runnable() {
        @Override
        public void run() {
            if (mWakeupLock.isHeld()) {
                mWakeupLock.release();
            }
        }
    };

    private void justWakeupAndRelease() {
        if (mWakeupLock.isHeld()) return;
        mWakeupLock.acquire(400);
        executeDelayed(mDelayReleaseWakeLock, 200);
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
            //mSender = cc[0].trim();
            //mContent = cc[1].trim();

            //android.util.Log.i(TAG, "sender name =" + mSender);
            //android.util.Log.i(TAG, "sender content =" + mContent);


            PendingIntent pendingIntent = notification.contentIntent;
            try {
                pendingIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
    }

    public static class MainThreadHandler extends Handler {
        public static final int MSG_RUNNABLE = 200;
        public int MSG_USER = 202;
        private final Object mLock = new Object();

        private WeakReference<MonitorEntrance> mService;

        public int generateId() {
            synchronized (mLock) {
                return MSG_USER++;
            }
        }

        public MainThreadHandler(MonitorEntrance service) {
            mService = new WeakReference<MonitorEntrance>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            MonitorEntrance service = mService.get();
            if (null == service) {
                Log.e(TAG, "service is dead.");
                return;
            }
            if (msg.obj instanceof Runnable) {
                Runnable r = (Runnable) msg.obj;
                r.run();
            }
        }
    }

    public MainThreadHandler mHandler = new MainThreadHandler(this);

    private MediaPlayer player;
    public void playSound(Context context) {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        //夜间不播放提示音
        if(hour > 7 && hour < 22) {
            player.start();
        }
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

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        mStash.process(event);
        int eventType = event.getEventType();
        String className = null;
        if (null != event.getClassName()) {
            className = event.getClassName().toString();
        }
        CharSequence c = event.getContentDescription();
        String describeContent = null != c ? c.toString() : "";
        List<CharSequence> texts = event.getText();
        String text = null != texts ? texts.toString() : "";
        Log.i(TAG, String.format("type:%d, class:%s, describe:%s, text:%s",
                eventType, className, describeContent, text));
        //justWakeupAndRelease(); //bugs todo java.lang.RuntimeException: WakeLock under-locked no-man-duty-entrance
        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                // Leave it to NotificationListener
                //sendNotificationReply(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED: {
                if (VIDEO_WINDOW.equals(className)) {
                    mVideo.answerTheCall();
                    mVideo.switchCamera();
                } else if (CHAT_WINDOW.equals(className)) {
                    /*if (WeChatManAccessHelper.LauncherUIChat.findEditTextAndPaste(this, "Wait for a minute.")) {
                        send();
                    }*/
                } else if (TEXT_PREVIEW_WINDOW.equals(className)) {
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (null == root) break;
                    List<AccessibilityNodeInfo> list = root.findAccessibilityNodeInfosByViewId(TEXT_PREVIEW_ID);
                    if (null == list || 0 == list.size()) {
                        Log.e(TAG, "cannot find TextView!");
                        break;
                    }
                    c = list.get(0).getText();
                    if (null != c) {
                        dispatchTextMessage(c.toString());
                    } else {
                        Log.e(TAG, "Preview text is null!");
                    }
                }
            }
            break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED: {
                /*if (RelativeLayout.class.getName().equals(className)) {
                    exchange();
                }*/
            }
            break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED: {
                /*executeDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MonitorEntrance.this.mLatestMessage.proceed();
                    }
                }, 1000);*/
                mLatestMessage.proceed();
            }
            break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED: {
            }
        }
    }

    public void ensureNotificationListenerAuthority() {
        String string = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners"); // Settings.ENABLED_NOTIFICATION_LISTENERS
        if (null == string || !string.contains(NotificationListener.class.getName())) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    public void LogAndShowToast(String text) {
        Log.d(TAG, text);
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    public void LogAndShowToast(int id) {
        LogAndShowToast(getResources().getString(id));
    }

    @Override
    public void onInterrupt() {
        LogAndShowToast(R.string.robot_is_interrupted);
    }

    private static MonitorEntrance sInstance;
    public static MonitorEntrance getService() {
        return sInstance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        mLatestMessage.register(this);
        mVideo.register(this);
        //AccessibilityServiceInfo info = getServiceInfo();
        //info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS; flagIncludeNotImportantViews|
        mPowerMgr =(PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeupLock = mPowerMgr.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        sInstance = this;
        LogAndShowToast(R.string.robot_is_on);
        ensureNotificationListenerAuthority();
        registerTextMessageReceiver(mCommandParser);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLatestMessage.unregister();
        mVideo.unregister();
        sInstance = null;
        LogAndShowToast(R.string.robot_is_off);
        unregisterTextMessageReceiver(mCommandParser);
    }
}
