package com.keith.wechat.monitor.utility;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ImageView;

import com.keith.wechat.monitor.R;

import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;


public class WechatManAccessHelper {
    private static final String TAG = "no-man-duty-wmah";

    public static class LauncherUIChat {
        private static final AccessibilityHelper.ConditionCallback sEditTextFinder = new AccessibilityHelper.ConditionCallback() {
            @Override
            public boolean onNodeInfoFound(AccessibilityNodeInfo nodeInfo, Object... args) {
                if (EditText.class.getName().contentEquals(nodeInfo.getClassName())) {
                    android.util.Log.i(TAG, "==================");
                    Bundle arguments = new Bundle();
                    arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_MOVEMENT_GRANULARITY_INT,
                            AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD);
                    arguments.putBoolean(AccessibilityNodeInfo.ACTION_ARGUMENT_EXTEND_SELECTION_BOOLEAN,
                            true);
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY,
                            arguments);
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    ClipData clip = ClipData.newPlainText("label", (String) args[0]);
                    ClipboardManager clipboardManager = (ClipboardManager) args[1];
                    clipboardManager.setPrimaryClip(clip);
                    nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                    return true;
                }
                return false;
            }
        };

        public static boolean findEditTextAndPaste(AccessibilityService service, String content) {
            AccessibilityNodeInfo rootNode = service.getRootInActiveWindow();
            if (rootNode == null) return false;
            return null != AccessibilityHelper.find(rootNode, sEditTextFinder, content, service.getSystemService(CLIPBOARD_SERVICE));
        }

        public static void send(AccessibilityService service) {
            AccessibilityNodeInfo nodeInfo = service.getRootInActiveWindow();
            if (nodeInfo != null) {
                final String go = service.getResources().getString(R.string.wechat_send_message);
                List<AccessibilityNodeInfo> list = nodeInfo
                        .findAccessibilityNodeInfosByText(go);
                if (list != null && list.size() > 0) {
                    for (AccessibilityNodeInfo n : list) {
                        n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }

                }
            }

        }

        public static boolean sendMessage(AccessibilityService service, String content) {
            if (findEditTextAndPaste(service, content)) {
                send(service);
                return true;
            } else {
                return false;
            }
        }

        /***
         * @param context
         * @param messageNode
         * @return current message node is from sender or not
         *
         * Judge the ImageView nearby View (messageNode) is on the left side or right side.
         * Left side : sender
         * Right side : self
         *
         */
        public static boolean isSender(Context context, AccessibilityNodeInfo messageNode) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(dm);

            final int grand = 3;
            final int screenWidth = dm.widthPixels;
            Rect bound = new Rect();
            AccessibilityNodeInfo c = messageNode;
            AccessibilityNodeInfo p = null;
            int i;
            for (i = 0; i < grand; ++i) {
                p = c.getParent();
                if (p == null) {
                    Log.e(TAG, "msg view's parent is null? " + i);
                    return true;
                }
                p.getBoundsInScreen(bound);
                //Log.d(TAG, "parent:" + p.toString());
                //Log.d(TAG, "c:" + c.toString());
                Log.d(TAG, String.format("parent bound:%s, screen:%d", bound.toString(), screenWidth));
                if (bound.width() == screenWidth) {
                    break;
                }
                c = p;
            }
            if (i == grand) {
                Log.e(TAG, "Not found parent layout that width is screen width size." + i);
                return true;
            }
            final int count = p.getChildCount();
            //if (count != 2) { // maybe 2 or 3!
            Log.d(TAG, "children:" + count);
            //    return true;
            //}
            for (i = 0; i < count; ++i) {
                AccessibilityNodeInfo child = p.getChild(i);
                if (child == c) {
                    continue;
                }
                if (ImageView.class.getName().equals(child.getClassName())) {
                    child.getBoundsInScreen(bound);
                    Log.d(TAG, String.format("i:%d, %s", i, bound.toString()));
                    return bound.left < screenWidth / 2;
                }
            }
            Log.e(TAG, "error?");
            return true;
        }
    }

    public static class VideoActivity {
        public static boolean answerTheCall(AccessibilityService service) {
            AccessibilityNodeInfo nodeInfo = service.getRootInActiveWindow();
            if (nodeInfo == null) {
                return false;
            }

            Resources r = service.getResources();
            final String accept = r.getString(R.string.wechat_accept_call);

            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText(accept);
            if (null != list && list.size() > 0) {
                AccessibilityNodeInfo ani = list.get(0);
                Log.d(TAG, String.format("Accept found! id:%s, class:%s",
                        ani.getViewIdResourceName(), ani.getClassName()));
                AccessibilityHelper.performClick(list.get(0));
                return true;
            }
            return false;
        }
    }
}
