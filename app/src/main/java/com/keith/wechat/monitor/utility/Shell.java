package com.keith.wechat.monitor.utility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Shell {
    static public String exec(String cmd) {
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
