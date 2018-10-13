package com.keith.wechat.monitor;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ListView;
import android.widget.Toast;

import com.keith.wechat.monitor.utility.AccessibilityHelper;
import com.keith.wechat.monitor.utility.sequence.MultiModeSequence;
import com.keith.wechat.monitor.utility.Shell;
import com.keith.wechat.monitor.utility.WechatManAccessHelper;

public class MonitorEntrance extends AccessibilityService {
    private static final String TAG = "no-man-duty-entrance";

    public static final String VIDEO_WINDOW = "com.tencent.mm.plugin.voip.ui.VideoActivity";
    public static final String TEXT_PREVIEW_WINDOW = "com.tencent.mm.ui.chatting.TextPreviewUI";
    public static final String TEXT_PREVIEW_ID = "com.tencent.mm:id/ann";
    public static final String CHAT_WINDOW = "com.tencent.mm.ui.LauncherUI";
    public static final String CHAT_LIST = "com.tencent.mm:id/mq";

    //private KeyguardManager mKeyguardMgr;
    //private KeyguardLock mKeyguardLock;
    private PowerManager mPowerMgr;
    private PowerManager.WakeLock mWakeupLock = null;

    private final AccessibilityHelper.EventStash mStash = new AccessibilityHelper.EventStash();
    private final LinkedList<TextMessageReceiver> mTextMessageReceivers =
            new LinkedList<>();

    private final CommandParser mCommandParser = new CommandParser();

    private String mSender;
    private AccessibilityNodeInfo mSenderInfo;
    private String mContent;

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

    private void executeDelayed(Runnable r, long timeout) {
        executeDelayed(r, MainThreadHandler.MSG_RUNNABLE, timeout);
    }

    private void executeDelayed(Runnable r, int what, long timeout) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = r;
        mHandler.sendMessageDelayed(msg, timeout);
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


    private AccessibilityHelper.ConditionCallback mSwitchCameraFinder = new AccessibilityHelper.ConditionCallback() {
        @Override
        public boolean onNodeInfoFound(AccessibilityNodeInfo info, Object... args) {
            String sc = (String)args[0];
            if (sc.contentEquals(info.getContentDescription())) {
                Log.d(TAG, "Switch Camera found! perform click!");
                AccessibilityHelper.performClick(info);
                return true;
            }
            return false;
        }
    };

    private boolean switchCamera() {
        Log.d(TAG, "enter Switch Camera");
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            return false;
        }
        Resources r = getResources();
        final String sc = r.getString(R.string.wechat_switch_camera);
        boolean ret = null != AccessibilityHelper.find(rootNode, mSwitchCameraFinder, sc);
        Log.d(TAG, "leave Switch Camera");
        return ret;
    }

    private AccessibilityHelper.EventStash.ISequence.Callback mLatestMessageCallback =
            new AccessibilityHelper.EventStash.ISequence.Callback() {
        private AccessibilityHelper.ConditionCallback mLatestChatContentFinder =
                new AccessibilityHelper.ConditionCallback() {
            private String mCopy;

            @Override
            public boolean onNodeInfoFound(AccessibilityNodeInfo info, Object... args) {
                if (null == mCopy) {
                    mCopy = MonitorEntrance.this.getResources().getString(R.string.wechat_copy);
                }
                Log.d(TAG, "mLatestChatContentFinder.onNodeInfoFound:" + info.getText());
                if (mCopy.equals(info.getText())) {
                    AccessibilityHelper.performClick(info);
                    mStash.push(mFromClipboard, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
                    return true;
                }
                return false;
            }
        };

        private AccessibilityHelper.EventStash.ISequence.Callback mFromClipboard =
                new AccessibilityHelper.EventStash.ISequence.Callback() {
            @Override
            public boolean onCompleted(AccessibilityEvent event) {
                if (AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED == event.getEventType()) {
                    if ("android.widget.Toast$TN".contentEquals(event.getClassName())) {
                        MonitorEntrance.this.mHandler.removeMessages(
                                MainThreadHandler.MSG_TIMEOUT_LONGCLICK_LATEST_CONTENT);

                        MonitorEntrance.this.dispatchTextMessage(
                                AccessibilityHelper.fromClipboard(MonitorEntrance.this));
                        return true;
                    }
                }
                return true;
            }
        };

        @Override
        public boolean onCompleted(AccessibilityEvent event) {
            Log.d(TAG, "mLatestMessageCallback onCompleted");
            if (AccessibilityEvent.TYPE_VIEW_SCROLLED == event.getEventType()) {
                final String name = ListView.class.getName();
                if (name.contentEquals(event.getClassName())) {
                    AccessibilityHelper.find(getRootInActiveWindow(), mLatestChatContentFinder);
                    return true;
                }
            }
            return false;
        }
    };

    private MultiModeSequence mLongClickLatestContentSequence;

    private Runnable mLongClickLatestChatContentRunnable = new Runnable() {
        @Override
        public void run() {
            MonitorEntrance.this.LongClickLatestChatContent();
        }
    };

    private void LongClickLatestChatContent() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (null == root) return;
        List<AccessibilityNodeInfo> ret =
                root.findAccessibilityNodeInfosByViewId(CHAT_LIST);
        if (null == ret) {
            Log.d(TAG, "chat list return null!");
            return;
        }
        final int count = ret.size();
        Log.d(TAG, "id/mq count:" + count);
        if (count == 0) {
            return;
        }

        /*for (AccessibilityNodeInfo i : ret) {
            i.performAction(AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
        }*/

        final AccessibilityNodeInfo last = ret.get(count - 1);
        if (!WechatManAccessHelper.LauncherUIChat.isSender(this, last)) {
            Log.d(TAG, "Not sender message, skip.");
            return;
        }
        Log.d(TAG, "last:" + last.toString());

        last.performAction(AccessibilityNodeInfo.ACTION_SELECT);
        last.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);

        if (null == mLongClickLatestContentSequence) {
            int[][] events = new int[3][];
            events[0] = new int[] {
                    AccessibilityEvent.TYPE_VIEW_SELECTED,
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            };
            events[1] = new int[] {
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_SELECTED,
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            };
            events[2] = new int[] {
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_SELECTED,
                    AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            };
            mLongClickLatestContentSequence = new MultiModeSequence(events, mLatestMessageCallback);
        }

        mStash.push(mLongClickLatestContentSequence);

        executeDelayed(mLongClickLatestChatContentRunnable,
                MainThreadHandler.MSG_TIMEOUT_LONGCLICK_LATEST_CONTENT, 1000);
        /*mStash.push(mLatestMessageCallback,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED);*/
    }

    static class MainThreadHandler extends Handler {
        public static final int MSG_RUNNABLE = 200;
        public static final int MSG_TIMEOUT_LONGCLICK_LATEST_CONTENT = 201;

        private WeakReference<MonitorEntrance> mService;

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
            switch (msg.what) {
                case MSG_TIMEOUT_LONGCLICK_LATEST_CONTENT:
                    service.mStash.remove(service.mLongClickLatestContentSequence);
                    service.mLongClickLatestContentSequence.reset();
                case MSG_RUNNABLE:
                    Runnable r =  (Runnable)msg.obj;
                    if (null != r) {
                        r.run();
                    }
                    break;
            }
        }
    }

    private MainThreadHandler mHandler = new MainThreadHandler(this);

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
                    WechatManAccessHelper.VideoActivity.answerTheCall(this);
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
                    switchCamera();
                }*/
            }
            break;
            case AccessibilityEvent.TYPE_VIEW_SCROLLED: {
                LongClickLatestChatContent();
            }
            break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED: {
                //tryDoubleClick();
            }
        }
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

    public void ensureNotificationListenerAuthority() {
        String string = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners"); // Settings.ENABLED_NOTIFICATION_LISTENERS
        if (null == string || !string.contains(NotificationListener.class.getName())) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onInterrupt() {
        String interrupted = getResources().getString(R.string.robot_is_interrupted);
        Toast.makeText(this, interrupted, Toast.LENGTH_LONG).show();
    }

    private static MonitorEntrance sInstance;
    public static MonitorEntrance getService() {
        return sInstance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        String on = getResources().getString(R.string.robot_is_on);
        Log.i(TAG, on);
        mPowerMgr =(PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeupLock = mPowerMgr.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
        //mKeyguardMgr = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        //mKeyguardLock = mKeyguardMgr.newKeyguardLock("unLock");
        //player = MediaPlayer.create(this, R.raw.songtip_m);
        sInstance = this;
        ensureNotificationListenerAuthority();
        registerTextMessageReceiver(mCommandParser);
        Toast.makeText(this, on, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        String off = getResources().getString(R.string.robot_is_off);
        Log.i(TAG, off);
        unregisterTextMessageReceiver(mCommandParser);
        Toast.makeText(this, off, Toast.LENGTH_LONG).show();
    }
}
