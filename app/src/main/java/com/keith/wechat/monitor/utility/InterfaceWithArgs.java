package com.keith.wechat.monitor.utility;

import com.keith.wechat.monitor.utility.sequence.EventStash;

public class InterfaceWithArgs {
    public static abstract class RunnableWithArgs implements Runnable {
        public void setArgs(Object... args) { }
        public Object getResult() {
            return null;
        }
    }

    public static abstract class SequenceCallbackWithArgs implements EventStash.ISequence.Callback {
        public abstract void setArgs(Object... args);
        public Object get(int index) {return null;}
    }

}
