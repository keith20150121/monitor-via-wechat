package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import com.keith.wechat.monitor.utility.AccessibilityHelper;

public class Sequence implements AccessibilityHelper.EventStash.ISequence {
    private static final String TAG = "no-man-s";

    protected final int[] mEvents;
    protected final Callback mCallback;
    protected int mIndex = 0;

    public Sequence(Callback callback, int... types) {
        mCallback = callback;
        mEvents = new int[types.length];
        System.arraycopy(types, 0, mEvents, 0, types.length);
    }

    @Override
    public boolean expectedOtherwiseRemove(AccessibilityEvent event) {
        Log.d(TAG, String.format("event-type:%d, expected:%d(index:%d)",
                event.getEventType(), mEvents[mIndex], mIndex));
        final int current = mEvents[mIndex];
        return event.getEventType() == current;
    }

    @Override
    public boolean process(AccessibilityEvent event) {
        final int current = mEvents[mIndex];
        if (event.getEventType() == current) {
            if (++mIndex == mEvents.length) {
                if (mCallback.onCompleted(event)) {
                    return true;
                } else {
                    mIndex = 0;
                }
            }
        }
        return false;
    }
}