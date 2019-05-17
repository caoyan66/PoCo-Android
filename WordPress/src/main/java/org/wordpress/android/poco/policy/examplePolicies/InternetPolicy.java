package org.wordpress.android.poco.policy.examplePolicies;

import android.content.Intent;
import android.util.Log;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.EventInfo;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.MtdArgs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This policy prevents the target app from visiting malicious urls.
 */

public class InternetPolicy extends Policy {
    private List<String> blackList;
    private Action loadWebview = new Action("android.webkit.WebView.loadUrl(*)");
    private Action openConn = new Action("java.net.URL.openConnection())");
    private Action urlActivity = new Action("android.content.Context.startActivity(*)");
    private Action urlActivity2 = new Action("android.app.Activity.startActivityForResult(*)");

    public InternetPolicy(String fileName) {
        blackList = new ArrayList<>();
        try( InputStream is = context.getAssets().open(fileName);
             BufferedReader bfr = new BufferedReader(new InputStreamReader(is))) {
                String line = null;
                while((line = bfr.readLine()) != null) {
                    blackList.add(line);
                }
        }
        catch (Exception ex) { Log.e("Aspects", ex.toString()); }
    }

    @Override
    public void onTrigger(Event e) {
        if(e.isAction()) {
            String url = null;

            if (e.matches(loadWebview)) {
                url = (String) e.getArg(0);
            }
            else if (e.matches(openConn)) {
                url = ((URL) e.getCaller()).toString();
            }
            else if (e.matches(urlActivity) || e.matches(urlActivity2)) {
                Object arg = e.getArg(0);
                if (arg != null) {
                    Intent intent = (Intent) arg;
                    String stringAct = intent.getAction();
                    if(stringAct!=null) {
                        if (intent.getAction().equals(Intent.ACTION_VIEW) && intent.getData() != null)
                            url = intent.getData().toString();
                    }
                    else {
                        if(intent.getExtras() != null && intent.getExtras().containsKey("url_to_load"))
                            url = intent.getExtras().getString("url_to_load");
                    }
                }
            }
            if (url != null && isInBlackList(url))
                setOutput(new Result(e, null));
        }
    }

    public boolean vote (CFG cfg) {
        if (cfg.containsUnresolved(loadWebview) || cfg.containsUnresolved(openConn) )
            return false;
        List<EventInfo> evts = cfg.getConcernedEvts(urlActivity);
        if(evts != null && evts.size() > 0) {
            for(EventInfo evt: evts) {
                if(evt.isResolvable()) {
                    MtdArgs mtdArgs = evt.getArgs();
                    if(mtdArgs != null) {
                        Object[]args = mtdArgs.getArgs();
                        if(args.length >0 && args[0] != null) {
                            Intent intent = (Intent) args[0];
                            if (intent.getAction().equals("android.intent.action.CALL") && intent.getData() != null)
                                if (!isInBlackList(intent.getData().toString()))
                                    return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isInBlackList(String url) {
        if(blackList != null) {
            for(String website: blackList) {
                if (url.contains(website))
                    return true;
            }
        }
        return false;
    }
}