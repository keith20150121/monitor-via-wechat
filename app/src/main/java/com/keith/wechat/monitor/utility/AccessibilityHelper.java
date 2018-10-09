package com.keith.wechat.monitor.utility;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

public class AccessibilityHelper {
    public interface ConditionCallback {
        boolean onNodeInfoFound(AccessibilityNodeInfo info, Object... args);
    }

    public static class AccessibilityEventSequence {
        private int[] mEvents;
        private int mIndex = 0;

        public AccessibilityEventSequence(int... eventType) {
            if (null != eventType) {

            }
        }
    }

    private static final String TAG = "no-man-duty-ash";

    public static void performClick(AccessibilityNodeInfo nodeInfo) {
        performActionUpwards(nodeInfo, AccessibilityNodeInfo.ACTION_CLICK);
    }

    public static void performLongClick(AccessibilityNodeInfo nodeInfo) {
        performActionUpwards(nodeInfo, AccessibilityNodeInfo.ACTION_LONG_CLICK);
    }

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
                ClipData clip = ClipData.newPlainText("label", (String)args[0]);
                ClipboardManager clipboardManager = (ClipboardManager)args[1];
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
        return null != AccessibilityHelper.find(rootNode, sEditTextFinder, content, service.getSystemService(Context.CLIPBOARD_SERVICE));
    }
}
