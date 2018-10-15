package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class UntilSequence extends Sequence {
    private static final String TAG = "no-man-us";
    private boolean mPassThrough = true;

    public UntilSequence(Callback callback, int... types) {
        super(callback, types);
    }

    @Override
    public boolean expectedOtherwiseRemove(AccessibilityEvent event) {
        final int current = mEvents[mIndex];
        boolean ret = event.getEventType() == current;
        if (ret) {
            Log.d(TAG, String.format("event-type:%d, expected:%d(index:%d), start go through the sequence.",
                    event.getEventType(), mEvents[mIndex], mIndex));
            mPassThrough = false;
        } else {
            Log.d(TAG, String.format("event-type:%d, expected:%d(index:%d) but pass it through",
                    event.getEventType(), mEvents[mIndex], mIndex));
        }
        return true;
    }

    @Override
    public boolean process(AccessibilityEvent event) {
        return !mPassThrough && super.process(event);
    }

    @Override
    public void reset() {
        super.reset();
        mPassThrough = true;
    }
}
