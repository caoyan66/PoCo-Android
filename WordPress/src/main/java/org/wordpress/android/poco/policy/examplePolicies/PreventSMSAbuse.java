package org.wordpress.android.poco.policy.examplePolicies;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;
import org.wordpress.android.poco.policy.Rtrace;
import org.wordpress.android.poco.policy.staticAnalysis.scanPolicies.EventInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * To prevent SMS abuse, this policy
 * 1) limits the number of messages the target application can send,
 * 2) prohibits the target application from sending the text message to prime numbers
 * 3) issues a warning when the target application tries to send a number that is not
 *    in contact list.
 * 4) prohibits the target application from intercept sms messages
 */

public class PreventSMSAbuse extends Policy {
    private final int SMSLimit;
    private int numofSentMsg = 0;
    private Action sendSMS = new Action("android.telephony.SmsManager.send*TextMessage(*)");
    private Action smsActivity = new Action("android.app.Activity+.startActivity(*))");

    public PreventSMSAbuse(int limit) {  SMSLimit = limit; }

    public void onTrigger(Event e) {
        if(e.isResult()) {
            if(e.matches(sendSMS)) {
                numofSentMsg = numofSentMsg + 1;
            }else if(e.matches(smsActivity)) {
                Object arg = e.getArg(0);
                if(arg != null) {
                    String action = ((Intent) arg).getAction();
                    if(action != null && action.equals(Intent.ACTION_SENDTO))
                        numofSentMsg = numofSentMsg + 1;
                }
            }
        } else { //action
            String destNumber = null;
            if(e.matches(sendSMS)) {
                destNumber = e.getArg(0).toString();
            } else if(e.matches(smsActivity)) {
                Object arg = e.getArg(0);
                if (arg != null) {
                    Intent intent = (Intent) arg;
                    if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_SENDTO))
                        destNumber = intent.getData().toString(); }
            }

            if( destNumber != null ) {
                if (numofSentMsg > SMSLimit || destNumber.length() <= 7) {
                    setOutput(new Result(e, null));
                } else {
                    Set<String> contact = getContact();
                    if (!contact.contains(destNumber)) {
                        String msg = "The app is attempting to send a message to the number +" + destNumber + ".\\nAllow this operation?";
                        int decision = showConfirmDialog(msg);
                        switch (decision) {
                            case PERMIT_OPTION: setOutput(e);                   break;
                            case DENY_OPTION:   setOutput(new Result(e, null)); break;
                            case KILL_OPTION:   setOutput(null);                break; }
                    }
                }
            }
        }
    }

    public boolean vote(CFG cfg) {
        if (cfg.contains(sendSMS)) return false;

        List<EventInfo> evts = cfg.getConcernedEvts(smsActivity);
        if(evts != null && evts.size() > 0) {
            for(EventInfo evt: evts) {
                if(evt.isResolvable()) {
                    Object[]args = evt.getArgs().getArgs();
                    if(args != null && args.length >0 && args[0] != null) {
                        if (((Intent) args[0]).getAction().equals(Intent.ACTION_SENDTO))
                            return false;
                    }
                }
            }
        }
        return true;
    }

    public void onOblig(Rtrace rt) {
        Event[] sendSMSevts = rt.locateEvts(sendSMS);
        if(sendSMSevts != null)
            numofSentMsg = numofSentMsg + sendSMSevts.length;

        sendSMSevts = rt.locateEvts(smsActivity);
        if(sendSMSevts != null)
            for(Event evt: sendSMSevts) {
                Object[]args = evt.getArgs();
                if(args != null && args.length >0 && args[0] != null &&
                        ((Intent) args[0]).getAction().equals(Intent.ACTION_SENDTO))
                    numofSentMsg = numofSentMsg + 1;
            }
    }

    @SuppressLint("Recycle")
    private Set<String> getContact() {
        Set<String> contacts = new HashSet<String>();
        ContentResolver cr = context.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor != null && cursor.moveToNext()) {
                    String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                    if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                        Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        while (pCur.moveToNext()) {
                            contacts.add( pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
                        }
                        pCur.close();
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(PreventSMSAbuse.class.getName(), ex.toString());
        }

        return contacts;
    }

}