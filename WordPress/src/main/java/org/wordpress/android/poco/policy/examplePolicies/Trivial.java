package org.wordpress.android.poco.policy.examplePolicies;

import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;
import org.wordpress.android.poco.policy.Rtrace;

public class Trivial extends Policy {
    public void onTrigger(Event e) { }
    public boolean vote (CFG cfg) { return true;}
    public void onOblig(Rtrace rt) {}
}
