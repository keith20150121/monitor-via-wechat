package com.keith.wechat.monitor.utility.sequence;

import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.LinkedList;

public class EventStash {
    private static final String TAG = "no-man-es";

    public interface ISequence {
        interface Callback {
            boolean onCompleted(AccessibilityEvent event);
        }
        boolean expectedOtherwiseRemove(AccessibilityEvent event);
        boolean process(AccessibilityEvent event);
        void reset();
    }

    private final LinkedList<ISequence> mList = new LinkedList<>();

    public void push(ISequence.Callback callback, int... eventTypes) {
        //Log.d(TAG, "push enter:" + Thread.currentThread().getId());
        if (null == eventTypes) return;
        Sequence sequence = new Sequence(callback, eventTypes);
        mList.push(sequence);
        //Log.d(TAG, "push exit:" + Thread.currentThread().getId());
    }

    public void push(ISequence sequence) {
        //Log.d(TAG, "push2 enter:" + Thread.currentThread().getId());
        mList.push(sequence);
        //Log.d(TAG, "push2 exit:" + Thread.currentThread().getId());
    }

    public void remove(ISequence sequence) {
        //Log.d(TAG, "remove enter:" + Thread.currentThread().getId());
        mList.remove(sequence);
        //Log.d(TAG, "remove exit:" + Thread.currentThread().getId());
    }

    public void process(AccessibilityEvent event) {
        //Log.d(TAG, "process enter:" + Thread.currentThread().getId());
        ArrayList<ISequence> toBeRemoved = new ArrayList<>();
        LinkedList<ISequence> clone = (LinkedList<ISequence>)mList.clone();
        for (ISequence sequence : clone) {
            if (!sequence.expectedOtherwiseRemove(event)) {
                //mList.remove(sequence);
                toBeRemoved.add(sequence);
                continue;
            }

            if (sequence.process(event)) {
                //mList.remove(sequence);
                toBeRemoved.add(sequence);
            }
        }
        if (toBeRemoved.size() > 0) {
            mList.removeAll(toBeRemoved);
        }
        //Log.d(TAG, "process exit:" + Thread.currentThread().getId());
    }
}
