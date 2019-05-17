package org.wordpress.android.poco.policy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import org.wordpress.android.WordPress;
import org.wordpress.android.poco.event.Event;


abstract public class Policy {
	private static boolean _outputNotSet;
	private static Event _outputEvt = null;
    protected Handler handler;
    protected AlertDialog.Builder builder;

    protected final int PERMIT_OPTION = 0;
    protected final int DENY_OPTION = 1;
    protected final int KILL_OPTION = 2;
	protected Context context;
	public void setContext (Context context) {this.context = context; }

    private int decision;

    public Policy() {
    	context = WordPress.getContext();
		builder = new AlertDialog.Builder(WordPress.getContext());
		handler = new Handler() {
			public void handleMessage(Message msg) {
				throw new RuntimeException();
			}
		};
	}

    protected int showConfirmDialog(String msg) {
    	initConfirmDialog();
        builder.setTitle("PoCo Warning").setMessage(msg).create().show();
        try { Looper.loop(); } catch (RuntimeException ex) { }

        return decision;
    }

    private void initConfirmDialog() {
		builder = new AlertDialog.Builder(context);
        builder.setCancelable(false);
        builder.setPositiveButton("Permit", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                decision = 0;
                handler.sendMessage(handler.obtainMessage());
            }
        }).setNegativeButton("Deny", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                decision = 1;
                handler.sendMessage(handler.obtainMessage());
            }
        }).setNeutralButton("Kill", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                decision = 2;
                handler.sendMessage(handler.obtainMessage());
            }
        });

    }

	protected void notifyUser(String msg) {
        Toast.makeText(WordPress.getContext(),msg,Toast.LENGTH_SHORT).show();
    }

	/**
	 * this method responds to a trigger event, which is any security-relevant
	 * operation the system is monitoring
	 *
	 * @param e the trigger event
	 */
	abstract public void onTrigger(Event e);

	/**
	 * a method that takes another policy's obligation, $o$, and returns a vote indicating approval or disapproval of $o$. This method is called {\tt vote}
	 * @param cfg
	 * @return
	 */
	public boolean vote(CFG cfg) {
		return true;
	}

	public void onOblig(Rtrace rt) { }

	public static boolean outputNotSet() {
		return _outputNotSet;
	}

	protected static void setOutput(Event e) {
		_outputNotSet = false;
		_outputEvt = e;
	}

	static void resetOutputFlag(boolean val) {
		_outputNotSet = val;
	}

	public static Event getOutput() {
		return _outputEvt;
	}

	private CFG cfg4onTrigger;

	void setCfg4onTrigger(CFG cfg) {
		cfg4onTrigger = cfg;
	}

	CFG getCfg4onTrigger() {
		return cfg4onTrigger;
	}

	private CFG cfg4onOblig;

	void setCfg4onOblig(CFG cfg) {
		cfg4onOblig = cfg;
	}

	CFG getCfg4onOblig() {
		return cfg4onOblig;
	}
}