package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.LinkedList;

public class MultiModeSequence implements EventStash.ISequence {
    private static final String TAG = "no-man-mms";

    protected int mIndex;
    protected int[][] mTotal;
    protected final Callback mCallback;
    protected final LinkedList<int[]> mPreferredList = new LinkedList<>();

    public MultiModeSequence(Callback callback, int[]... types) {
        this(types, callback);
    }

    public MultiModeSequence(int[][] types, Callback callback) {
        mCallback = callback;
        mTotal = new int[types.length][];
        System.arraycopy(types, 0, mTotal, 0, types.length);
    }

    public Callback callback() {
        return mCallback;
    }

    protected boolean tryPresetEvents(int[] events, AccessibilityEvent event) {
        if (mIndex >= events.length) {
            Log.e(TAG, String.format("out of bound, index:%d, length:%d", mIndex, events.length));
            return false;
        }
        Log.d(TAG, String.format("event-type:%d, expected:%d(index:%d)",
                event.getEventType(), events[mIndex], mIndex));
        final int current = events[mIndex];
        return event.getEventType() == current;
    }

    private final ArrayList<int[]> mToBeRemoved = new ArrayList<>();
    @Override
    public boolean expectedOtherwiseRemove(AccessibilityEvent event) {
        if (0 != mPreferredList.size()) {
            mToBeRemoved.clear();
            for (int[] types : mPreferredList) {
                if (!tryPresetEvents(types, event)) {
                    //mPreferredList.remove(types);
                    mToBeRemoved.add(types);
                }
            }
            mPreferredList.removeAll(mToBeRemoved);
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
                    reset();
                    return mCallback.onCompleted(event);
                }
            }
        }
        return false;
    }

    public void reset() {
        mIndex = 0;
        mPreferredList.clear();
    }

}
