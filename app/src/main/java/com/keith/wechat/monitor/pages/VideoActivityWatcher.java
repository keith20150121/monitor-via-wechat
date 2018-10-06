package com.keith.wechat.monitor.pages;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class VideoActivityWatcher {
    static private VideoActivityWatcher sInstance = new VideoActivityWatcher();
    static private final String TAG = "no-man-duty-vw";

    private Activity mSelf;
    private ViewGroup mRoot;

    private View mAccept;
    private View mSwtichCamera;

    static private View find(final Activity self, ViewGroup group, String text) {
        final int count = group.getChildCount();
        for (int i = 0; i < count; ++i) {
            View view = group.getChildAt(i);
            if (view instanceof TextView) {
                TextView tv = (TextView)view;
                Toast.makeText(self, tv.getText(), Toast.LENGTH_SHORT).show();
                if (text.contentEquals(tv.getText())) return view;
            } else if (view instanceof ViewGroup) {
                return find(self, (ViewGroup)view, text);
            } else if (view instanceof ImageView) {
                int[] x_y = new int[2];
                view.getLocationOnScreen(x_y);
                Toast.makeText(self, String.format("ImageView - x:%d, y:%d", x_y[0], x_y[1]), Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    // Accept is ImageView
    public void activate(final Activity self) {
        //Log.d(TAG, "activate");
        Toast.makeText(self, "activate3", Toast.LENGTH_SHORT).show();

        final ViewGroup root = self.findViewById(Window.ID_ANDROID_CONTENT);
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View accept = find(self, root, "接听");
                if (null == accept) {
                    Toast.makeText(self, "Accept button is null!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(self, "Accept button is found, exit!", Toast.LENGTH_SHORT).show();
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    accept.callOnClick();
                }
            }
        });
    }

    static public VideoActivityWatcher getInstance() {
        return sInstance;
    }
}
