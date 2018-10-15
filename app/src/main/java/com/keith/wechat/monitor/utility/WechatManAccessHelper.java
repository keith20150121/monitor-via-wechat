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
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.keith.wechat.monitor.MonitorEntrance;
import com.keith.wechat.monitor.R;
import com.keith.wechat.monitor.utility.sequence.EventStash;
import com.keith.wechat.monitor.utility.sequence.MultiModeSequence;
import com.keith.wechat.monitor.utility.sequence.Sequence;
import com.keith.wechat.monitor.utility.sequence.UntilSequence;

import java.util.List;

import static android.content.Context.CLIPBOARD_SERVICE;


public class WechatManAccessHelper {
    private static final String TAG = "no-man-duty-wmah";
    public static final String CHAT_LIST = "com.tencent.mm:id/mq";

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

        public static class LatestMessage {
            private MonitorEntrance mHost;

            public void register(MonitorEntrance host) {
                mHost = host;
            }

            public void unregister() {
                mHost = null;
            }

            private EventStash.ISequence.Callback mLatestMessageCallback =
                    new EventStash.ISequence.Callback() {
                        private final AccessibilityHelper.ConditionCallback mLatestChatContentFinder =
                                new AccessibilityHelper.ConditionCallback() {
                                    private String mCopy;

                                    @Override
                                    public boolean onNodeInfoFound(AccessibilityNodeInfo info, Object... args) {
                                        if (null == mCopy) {
                                            mCopy = mHost.getResources().getString(R.string.wechat_copy);
                                        }
                                        Log.d(TAG, "mLatestChatContentFinder.onNodeInfoFound:" + info.getText());
                                        if (mCopy.equals(info.getText())) {
                                            AccessibilityHelper.performClick(info);
                                            mHost.push(mFromClipboard, AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED);
                                            return true;
                                        }
                                        return false;
                                    }
                                };

                        private final EventStash.ISequence.Callback mFromClipboard =
                                new EventStash.ISequence.Callback() {
                                    @Override
                                    public boolean onCompleted(AccessibilityEvent event) {
                                        if (AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED == event.getEventType()) {
                                            if ("android.widget.Toast$TN".contentEquals(event.getClassName())) {
                                                mHost.removeMessages(MSG_TIMEOUT_RETRY);
                                                mHost.dispatchTextMessage(AccessibilityHelper.fromClipboard(mHost));
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
                                    AccessibilityHelper.find(mHost.getRootInActiveWindow(), mLatestChatContentFinder);
                                    return true;
                                }
                            }
                            return false;
                        }
                    };

            static abstract class EventStashCallbackWithNode
                    implements EventStash.ISequence.Callback {
                protected AccessibilityNodeInfo mNode;

                public void setArgs(AccessibilityNodeInfo node) {
                    mNode = node;
                }
            }

            private EventStashCallbackWithNode mOnSelectedEvent = new EventStashCallbackWithNode() {
                @Override
                public boolean onCompleted(AccessibilityEvent event) {
                    mNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);

                    int[] types = new int[] {
                            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                            AccessibilityEvent.TYPE_VIEW_SCROLLED };
                    LatestMessage.this.mHost.push(new Sequence(mLatestMessageCallback, types));

                    return true;
                }
            };

            private MultiModeSequence mManualAccessSequence;
            private final Runnable mManualAccessRetryRunnable = new Runnable() {
                @Override
                public void run() {
                    if (null == mHost) {
                        Log.e(TAG, "Runnable:mHost is null!");
                        return;
                    }
                    LatestMessage.this.mHost.remove(mManualAccessSequence);
                    mManualAccessSequence.reset();
                    LatestMessage.this.simulateManualAccess();
                }
            };

            private static final int MSG_TIMEOUT_RETRY = MonitorEntrance.MainThreadHandler.MSG_USER + 1;
            public void simulateManualAccess() {
                AccessibilityNodeInfo root = mHost.getRootInActiveWindow();
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

                final AccessibilityNodeInfo last = ret.get(count - 1);
                if (!WechatManAccessHelper.LauncherUIChat.isSender(mHost, last)) {
                    Log.d(TAG, "Not sender message, skip.");
                    return;
                }
                Log.d(TAG, "last:" + last.toString());

                last.performAction(AccessibilityNodeInfo.ACTION_SELECT);

                UntilSequence sequence = new UntilSequence(mOnSelectedEvent,
                        AccessibilityEvent.TYPE_VIEW_SELECTED);
                mOnSelectedEvent.setArgs(last);
                mHost.push(sequence);
            }

            public void simulateManualAccess2() {
                AccessibilityNodeInfo root = mHost.getRootInActiveWindow();
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
                if (!WechatManAccessHelper.LauncherUIChat.isSender(mHost, last)) {
                    Log.d(TAG, "Not sender message, skip.");
                    return;
                }
                Log.d(TAG, "last:" + last.toString());

                last.performAction(AccessibilityNodeInfo.ACTION_SELECT);
                last.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);

                if (null == mManualAccessSequence) {
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
                    mManualAccessSequence = new MultiModeSequence(events, mLatestMessageCallback);
                }

                mHost.push(mManualAccessSequence);
                mHost.executeDelayed(mManualAccessRetryRunnable, MSG_TIMEOUT_RETRY, 1000);
        /*mStash.push(mLatestMessageCallback,
                AccessibilityEvent.TYPE_VIEW_SELECTED,
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED);*/
            }

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
