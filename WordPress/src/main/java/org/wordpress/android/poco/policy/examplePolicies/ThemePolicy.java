package org.wordpress.android.poco.policy.examplePolicies;

import android.webkit.WebView;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.Policy;

public class ThemePolicy extends Policy {
    private Action onPageFinished = new Action("*.onPageFinished(*,java.lang.String)");
    private Action actTheme = new Action("org.wordpress.android.ui.themes.ThemeBrowserActivity.activateTheme(*)");

    @Override
    public void onTrigger(Event e) {
        if(e.isAction()) {
           if(e.matches(actTheme) ) {
               int decision = showConfirmDialog("Allow activing the theme?");
               switch (decision) {
                   case PERMIT_OPTION: setOutput(e);                     break;
                   case DENY_OPTION:   setOutput(new Result(e, null));   break;
                   case KILL_OPTION:   setOutput(null);                  break;
               }
           }
        }
        else {
            if (e.matches(onPageFinished)) {
                WebView view = (WebView) e.getArgs()[0];
                view.loadUrl("javascript:(function(){document.body.innerHTML = document.body.innerHTML.replace('Activate this design', '').replace('Free', '')})()");
            }
        }
    }
}
