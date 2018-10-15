package com.keith.wechat.monitor.utility.sequence;

import android.view.accessibility.AccessibilityEvent;

import java.util.LinkedList;

public class EventStash {
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
        if (null == eventTypes) return;
        Sequence sequence = new Sequence(callback, eventTypes);
        mList.push(sequence);
    }

    public void push(ISequence sequence) {
        mList.push(sequence);
    }

    public void remove(ISequence sequence) {
        mList.remove(sequence);
    }

    public void process(AccessibilityEvent event) {
        for (ISequence sequence : mList) {
            if (!sequence.expectedOtherwiseRemove(event)) {
                mList.remove(sequence);
                continue;
            }

            if (sequence.process(event)) {
                mList.remove(sequence);
            }
        }
    }
}
