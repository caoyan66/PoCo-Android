package org.wordpress.android.poco.policy.examplePolicies;

import android.os.Handler;
import android.util.Log;

import com.airbnb.lottie.L;

import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.policy.Policy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLOutput;

import dagger.multibindings.ElementsIntoSet;


public class CPUMonitor extends Policy {
    private int appPID;
    private Runtime r;
    private boolean havePopped = false;

    public CPUMonitor (final double maxCPuPercent, final String pkgName) {
        appPID = android.os.Process.myPid();
        r = Runtime.getRuntime();

        final Handler handler = new Handler();
        Runnable trafficMonitorRun = new Runnable() {
            public void run() {
                Long cupInfo = getCPUInfo(pkgName);
                if(!havePopped && cupInfo != null && cupInfo > maxCPuPercent) {
                    havePopped = true;
                    notifyUser("The app has consumed more than " + (maxCPuPercent*100) + "% of the CPU resources.\n");
                }
                handler.postDelayed(this, 10000);
            } };

        handler.postDelayed(trafficMonitorRun, 10000);
    }

    public void onTrigger(Event e) { }

    private Long getCPUInfo(String pkgName) {
        InputStreamReader isr = null;
        try{
            isr = new InputStreamReader(r.exec("top -n 1").getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            String load = reader.readLine();
            while(load != null) {
                if(load.contains(pkgName) && load.contains(String.valueOf(appPID)))
                    break;
                else
                    load = reader.readLine();
            }
            if(load != null) {
                String[] cpuInfos = load.split("\\s+"); //cpuInfos[5] CPU;
                if(cpuInfos[5].length() >1 && cpuInfos[5].endsWith("%")) { //not available
                    long val = Long.parseLong(cpuInfos[5].substring(0, cpuInfos[5].length() - 1));
                    double cpuinfo = val/100.00;
                    return (long)cpuinfo;
                }
                else
                    return 0L;
            }
        }
        catch (IOException ex) {
            Log.e("ex.printStackTrace(); ",ex.getMessage());
        }
        finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) { Log.e("ex.printStackTrace(); ",e.getMessage()); }
            }
        }
        return null;
    }
}