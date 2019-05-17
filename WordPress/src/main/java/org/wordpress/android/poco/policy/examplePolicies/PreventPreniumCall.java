package org.wordpress.android.poco.policy.examplePolicies;

import android.content.Intent;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.EventInfo;

import java.util.List;
import java.util.Set;

public class PreventPreniumCall extends Policy {
    private Action callActivity = new Action("android.content.Context.startActivity(*)");
    private Set<String> preniumPrefixs;

    public PreventPreniumCall(Set<String> preniumPrefix){ this.preniumPrefixs = preniumPrefix; }

    public void onTrigger(Event e) {
        if(e.isAction() && e.matches(callActivity)) {
            Object arg = e.getArg(0);
            if(arg != null) {
                Intent intent = (Intent) arg;
                String act = intent.getAction();
                if(act!= null && act.equals(Intent.ACTION_CALL)) {
                    String callNumber = intent.getData().toString().substring(4);
                    if(preniumPrefixs.contains(callNumber))
                        setOutput(new Result(e, null));
                }
            }
        }
    }

    public boolean vote(CFG cfg) {
        List<EventInfo> evts = cfg.getConcernedEvts(callActivity);
        if(evts != null && evts.size() > 0) {
            for(EventInfo evt: evts) {
                if(evt.isResolvable()) {
                    Object[]args = evt.getArgs().getArgs();
                    if(args != null && args.length >0 && args[0] != null) {
                        Intent intent = (Intent) args[0];
                        if(intent.getAction().equals("android.intent.action.CALL")) {
                            String callNumber = intent.getData().toString().substring(4);
                            if (preniumPrefixs.contains(callNumber))
                                return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}