package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class Sequence extends BaseSequence {
    private static final String TAG = "no-man-s";

    public Sequence(Callback callback, int... types) {
        super(callback, types);
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