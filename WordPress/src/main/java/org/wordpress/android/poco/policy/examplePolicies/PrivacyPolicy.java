package org.wordpress.android.poco.policy.examplePolicies;

import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.CFG;
import org.wordpress.android.poco.policy.Policy;

/**
 * To protect privacy on mobile devices, this policy
 * 1) prevents the target application from accessing the phone's IMEI, IMSI,
 *    and ICC information by providing the requesting application with fake information.
 *
 * 2) issues a warning when application requests the phone number via a
 *    confirmation dialog, where a user can choose to permit or deny the request by
 *    offering real and fake numbers respectively.
 */
public class PrivacyPolicy extends Policy {
    private Action getDeviceId = new Action("android.telephony.TelephonyManager.getDeviceId()");
    private Action getImei = new Action("android.telephony.TelephonyManager.getImei(*)");
    private Action getImsi = new Action("android.telephony.TelephonyManager.getSubscriberId()");
    private Action getphoneNumber = new Action("android.telephony.TelephonyManager.getLine1Number()");
    private String fakeImei, fakeImsi, fakePh;
    public PrivacyPolicy(String fakeImei, String fakeImsi, String fakePh) {
        this.fakeImei = fakeImei;
        this.fakeImsi = fakeImsi;
        this.fakePh = fakePh;
    }

    public void onTrigger(Event e) {
        if(e.isAction()) {
            if (e.matches(getDeviceId) || e.matches(getImei)) {
                notifyUser("The app requested your phone's IMEI information.");
                setOutput(new Result(e,fakeImei));
            } else if (e.matches(getImsi)) {
                notifyUser("The app requested your phone's IMSI information.");
                setOutput(new Result(e, fakeImsi));
            } else if (e.matches(getphoneNumber)) {
                int decision = showConfirmDialog("The app is attempting to get your phone number.\\nAllow this operation?");
                switch (decision) {
                    case PERMIT_OPTION: setOutput(e);                       break;
                    case DENY_OPTION:   setOutput(new Result(e, fakePh));   break;
                    case KILL_OPTION:   setOutput(null);                    break;
                }
            }
        }
    }
    public boolean vote (CFG cfg) {
        return !cfg.contains(getImei) && !cfg.contains(getDeviceId) &&
               !cfg.contains(getImsi) &&
               !cfg.contains(getphoneNumber);
    }
}