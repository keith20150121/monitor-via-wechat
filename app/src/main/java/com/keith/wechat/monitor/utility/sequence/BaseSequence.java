package com.keith.wechat.monitor.utility.sequence;

public abstract class BaseSequence implements EventStash.ISequence {
    protected final int[] mEvents;
    protected final Callback mCallback;
    protected int mIndex = 0;

    public BaseSequence(Callback callback, int... types) {
        mCallback = callback;
        mEvents = new int[types.length];
        System.arraycopy(types, 0, mEvents, 0, types.length);
    }

    public Callback callback() {
        return mCallback;
    }

    public void reset() {
        mIndex = 0;
    }
}
