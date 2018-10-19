package com.keith.wechat.monitor.utility;

import android.app.ActivityManager;
import android.content.Context;
import android.text.format.Formatter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shell {

    public static class ExecRunnable implements Runnable {
        private final String mCommand;

        public ExecRunnable(String cmd) {
            mCommand = cmd;
        }

        @Override
        public void run() {
            exec(mCommand);
        }
    }

    public static long getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
        //return Formatter.formatFileSize(context, mi.availMem);
    }

    public static int determinateDelay(Context context) {
        long avai = getAvailMemory(context);
        double g = avai / (1024 * 1024 * 1024.0);
        return g > 1.5 ? 600 : 1000;
    }

    public static String exec(String cmd) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder respBuff = new StringBuilder();
            char[] buff = new char[1024];
            int ch = 0;
            while ((ch = reader.read(buff)) != -1) {
                respBuff.append(buff, 0, ch);
            }
            reader.close();
            return respBuff.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block e.printStackTrace();
        }
        return null;
    }
}
