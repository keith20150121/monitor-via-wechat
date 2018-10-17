package com.keith.wechat.monitor.utility;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.keith.wechat.monitor.utility.sequence.Sequence;

import java.util.LinkedList;

import static android.content.Context.CLIPBOARD_SERVICE;

public class AccessibilityHelper {
    public interface ConditionCallback {
        boolean onNodeInfoFound(AccessibilityNodeInfo info, Object... args);
    }

    private static final String TAG = "no-man-duty-ash";

    public static void performClick(AccessibilityNodeInfo nodeInfo) {
        performActionUpwards(nodeInfo, AccessibilityNodeInfo.ACTION_CLICK);
    }

    //public static void performLongClick(AccessibilityNodeInfo nodeInfo) {
    //    performActionUpwards(nodeInfo, AccessibilityNodeInfo.ACTION_LONG_CLICK);
    //}

    public static void performActionUpwards(AccessibilityNodeInfo nodeInfo, int action) {
        if (nodeInfo == null) {
            return;
        }
        if (nodeInfo.isClickable()) {
            nodeInfo.performAction(action);
        } else {
            performActionUpwards(nodeInfo.getParent(), action);
        }
    }

    public static void performBack(AccessibilityService service) {
        if (service == null) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }


    public static AccessibilityNodeInfo find(AccessibilityNodeInfo root, ConditionCallback callback, Object... args) {
        if (null == root) {
            Log.e(TAG, "root is null!");
            return null;
        }
        final int count = root.getChildCount();
        Log.d(TAG, String.format("root class=%s, text=%s, child:%d", root.getClassName(), root.getText(), count));
        AccessibilityNodeInfo ret = null;
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo == null) {
                Log.d(TAG, "nodeinfo = null");
                continue;
            }
            Log.d(TAG, "class=" + nodeInfo.getClassName());
            Log.e(TAG, "ds=" + nodeInfo.getContentDescription());

            if (callback.onNodeInfoFound(nodeInfo, args)) {
                return nodeInfo;
            }

            ret = find(nodeInfo, callback, args);
            if (null != ret) {
                return ret;
            }
        }
        return ret;
    }

    public static String fromClipboard(Context context) {
        ClipboardManager cm = (ClipboardManager)context.getSystemService(CLIPBOARD_SERVICE);
        ClipData data = cm.getPrimaryClip();
        ClipData.Item item = data.getItemAt(0);
        String content = item.getText().toString();
        return content;
    }
}
