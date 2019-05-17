package org.wordpress.android.poco.policy.examplePolicies;

import android.location.Location;
import android.util.Log;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;

import java.lang.reflect.Method;

//location blurring
public class BlockingDeviceLocation extends Policy {
    private Action getLoc = new Action("android.location.LocationListener+.getLocation()");
    private Action LastKnownLoc = new Action("android.location.LocationManager.getLastKnownLocation(*)");

    public void onTrigger(Event e) {
        if(e.isResult() ) {
            if (e.matches(getLoc) || e.matches(LastKnownLoc)) {
                Object loc = ((Result) e).getEvtRes();
                if(loc != null) {
                    Location location = (Location) loc;
                    location.setLatitude(location.getLatitude() * 1E6);
                    location.setLongitude(location.getLongitude() * 1E6);
                    setOutput(new Result(e.getEvtSig(), location));
                }
            }
            else if( e.matches(new Action("java.lang.reflect.Method.invoke(*)")) ) {
                Method mtd = (Method)e.getCaller();
                Class listener=null;
                try {
                    listener = Class.forName("android.location.LocationListener");
                }
                catch (ClassNotFoundException e1) { e1.printStackTrace(); }

                String callingClazz = mtd.getDeclaringClass().getCanonicalName();

                if(callingClazz.startsWith("android.location.LocationManager") &&
                        mtd.getName().equals("getLastKnownLocation"))
                    setOutput(new Result(e, null));
                else if ( listener.isAssignableFrom(mtd.getDeclaringClass()) &&
                        mtd.getName().equals("getLocation"))
                    setOutput(new Result(e, null));
            }
        }
    }

    public boolean vote (CFG cfg) {
        return !cfg.contains(getLoc) && !cfg.contains(LastKnownLoc);
    }
}