package com.keith.wechat.monitor.utility.sequence;

import android.view.accessibility.AccessibilityEvent;

public class Any implements EventStash.ISequence {
    protected final Callback mCallback;

    @Override
    public void reset() {

    }

    public Any(Callback callback) {
        mCallback = callback;
    }

    @Override
    public Callback callback() {
        return mCallback;
    }

    @Override
    public boolean expectedOtherwiseRemove(AccessibilityEvent event) {
        return true;
    }

    @Override
    public boolean process(AccessibilityEvent event) {
        return mCallback.onCompleted(event);
    }

}
