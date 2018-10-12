package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.keith.wechat.monitor.utility.AccessibilityHelper;

import java.util.LinkedList;

public class MultiModeSequence implements AccessibilityHelper.EventStash.ISequence {
    private static final String TAG = "no-man-mms";

    protected int mIndex;
    protected int[][] mTotal;
    protected final Callback mCallback;
    protected final LinkedList<int[]> mPreferredList = new LinkedList<>();

    public MultiModeSequence(Callback callback, int[]... types) {
        mCallback = callback;
        mTotal = new int[types.length][];
        System.arraycopy(types, 0, mTotal, 0, types.length);
    }

    protected boolean tryPresetEvents(int[] events, AccessibilityEvent event) {
        Log.d(TAG, String.format("event-type:%d, expected:%d(index:%d)",
                event.getEventType(), events[mIndex], mIndex));
        final int current = events[mIndex];
        return event.getEventType() == current;
    }

    @Override
    public boolean expectedOtherwiseRemove(AccessibilityEvent event) {
        if (0 != mPreferredList.size()) {
            for (int[] types : mPreferredList) {
                if (!tryPresetEvents(types, event)) {
                    mPreferredList.remove(types);
                }
            }
        } else {
            for (int[] types : mTotal) {
                if (tryPresetEvents(types, event)) {
                    mPreferredList.push(types);
                }
            }
        }
        return 0 != mPreferredList.size();
    }

    @Override
    public boolean process(AccessibilityEvent event) {
        for (int[] types : mPreferredList) {
            if (event.getEventType() == types[mIndex]) {
                if (++mIndex == types.length) {
                    if (mCallback.onCompleted(event)) {
                        return true;
                    } else {
                        mIndex = 0;
                        mPreferredList.clear();
                    }
                }
            }
        }
        return false;
    }

}
