package org.wordpress.android.poco.policy.examplePolicies;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;

import java.lang.reflect.Method;

public class BlockAdsPolicy extends Policy {
    private Action openAd1 = new Action("com.google.android.gms.ads.*(*)");
    private Action openAd2 = new Action("com.inmobi.androidsdk.*(*)");
    private Action reflection = new Action("java.lang.reflect.Method.invoke(*)");

    public void onTrigger(Event e) {
        if(e.isAction()) {
            if (e.matches(openAd1) || e.matches(openAd2)) {
                setOutput(new Result(e, null));
            }
            else if (e.matches(reflection)) {
                Method mtd = (Method) e.getCaller();
                String clzName = mtd.getDeclaringClass().getCanonicalName();
                if (clzName.equals("com.google.android.gms.ads") || clzName.equals("com.inmobi.androidsdk"))
                    setOutput(new Result(e, null));
            }
        }
    }

    public boolean vote (CFG cfg) {
         if (cfg.containsIncludeUnreslv(openAd1) ||
                 cfg.containsIncludeUnreslv(openAd2) ||
                 cfg.containsIncludeUnreslv(reflection))
             return false;

         return true;
    }
}