package com.keith.wechat.monitor;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.res.Resources;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {
    public static final String TAG = "no-man-duty-nl";

    private String PKG_WECHAT;
    private String PKG_EXPOSED;

    @Override
    public void onCreate() {
        super.onCreate();

        Resources r = getResources();
        PKG_WECHAT = r.getString(R.string.wechat_package);
        PKG_EXPOSED = r.getString(R.string.exposed_package);
    }

    private void launchNotificationActivity(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            PendingIntent pendingIntent = notification.contentIntent;
            if (null != pendingIntent) {
                pendingIntent.send();
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        final String pkg = sbn.getPackageName();

        Log.i(TAG, "open" + "-----" + pkg);
        Log.i(TAG, "open" + "------" + sbn.getNotification().tickerText);
        Log.i(TAG, "open" + "-----" + sbn.getNotification().extras.get("android.title"));
        Log.i(TAG, "open" + "-----" + sbn.getNotification().extras.get("android.text"));

        if (null != pkg) {
            if (PKG_WECHAT.equals(pkg) || PKG_EXPOSED.equals(pkg)) {
                launchNotificationActivity(sbn);
            }
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.i(TAG, "remove" + "-----" + sbn.getPackageName());
    }
}