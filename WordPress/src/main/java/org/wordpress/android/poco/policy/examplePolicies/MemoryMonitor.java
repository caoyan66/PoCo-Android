package org.wordpress.android.poco.policy.examplePolicies;

import android.os.Handler;

import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.policy.Policy;

public class MemoryMonitor extends Policy {
    private boolean havePopped;
    private Runtime r;

    public MemoryMonitor( final double maxMemPercent) {
        final Handler handler = new Handler();
        r = Runtime.getRuntime();
        Runnable trafficMonitorRun = new Runnable() {
            public void run() {
                if (!havePopped && r.totalMemory() / r.maxMemory() > maxMemPercent) {
                    havePopped = true;
                    notifyUser("The app has consumed more than " + maxMemPercent + "% of the total memory");
                }
                handler.postDelayed(this, 10000);
            } };
        handler.postDelayed(trafficMonitorRun, 10000);
    }

    public void onTrigger(Event e) { }
}