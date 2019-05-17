package org.wordpress.android.poco.policy.examplePolicies;

import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Telephony;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;


/**
 * THe policy requests the user's permission to allow an application to access the call logs
 * and deny access to the stored SMS message notifying the user of the application's attempt.
 */

public class CallInfoPolicy extends Policy {
    String crQuerySig = "android.content.ContentResolver.query(android.net.Uri,*)";
    private Action getSMSInbox = new Action(crQuerySig, new Object[]{Telephony.Sms.Inbox.CONTENT_URI});
    private Action getSMSSent = new Action(crQuerySig, new Object[]{Telephony.Sms.Sent.CONTENT_URI});
    private Action getCallLogs = new Action(crQuerySig, new Object[]{CallLog.Calls.CONTENT_URI});
    private Action getContact = new Action(crQuerySig, new Object[]{ContactsContract.Contacts.CONTENT_URI});

    public void onTrigger(Event e) {
        if(e.isAction()) {
            if (e.matches(getSMSInbox) || e.matches(getSMSSent)) {
                notifyUser("The app requested to access your SMS messages.");
                setOutput(new Result(e, null));
            }
            else if (e.matches(getContact)) {
                notifyUser("The app requested to access your contact info.");
                setOutput(new Result(e, null));
            }
            else if (e.matches(getCallLogs)) {
                int decision = showConfirmDialog("The app is attempting to get your call logs.\nAllow this operation?");
                switch (decision) {
                    case PERMIT_OPTION: setOutput(e);                   break;
                    case DENY_OPTION:   setOutput(new Result(e, null)); break;
                    case KILL_OPTION:   setOutput(null);                break;
                }
            }
        }
    }

    public boolean vote (CFG cfg) {
        return !cfg.containsIncludeUnreslv(getSMSInbox) &&
                !cfg.containsIncludeUnreslv(getSMSInbox) &&
                !cfg.containsIncludeUnreslv(getCallLogs) &&
                !cfg.containsIncludeUnreslv(getContact) ;
    }

}