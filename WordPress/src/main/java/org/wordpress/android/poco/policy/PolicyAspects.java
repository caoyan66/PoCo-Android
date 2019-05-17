package org.wordpress.android.poco.policy;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.view.MenuItem;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.wordpress.android.R;
import org.wordpress.android.poco.event.Action;
import org.wordpress.android.poco.event.Event;
import org.wordpress.android.poco.event.EvtTyp;
import org.wordpress.android.poco.event.Result;
import org.wordpress.android.poco.policy.examplePolicies.BlockAdsPolicy;
import org.wordpress.android.poco.policy.examplePolicies.BlockingDeviceLocation;
import org.wordpress.android.poco.policy.examplePolicies.CPUMonitor;
import org.wordpress.android.poco.policy.examplePolicies.CallInfoPolicy;
import org.wordpress.android.poco.policy.examplePolicies.InternetPolicy;
import org.wordpress.android.poco.policy.examplePolicies.MemoryMonitor;
import org.wordpress.android.poco.policy.examplePolicies.PreventPreniumCall;
import org.wordpress.android.poco.policy.examplePolicies.PreventSMSAbuse;
import org.wordpress.android.poco.policy.examplePolicies.PrivacyPolicy;
import org.wordpress.android.poco.policy.examplePolicies.ThemePolicy;


@Aspect public class PolicyAspects {
    private static final String TAG = PolicyAspects.class.getName();
    private Monitor monitor;
    private Event trigger;
    private Object returnRes;
    private Context currContext;
    private long loading, start_row, start_menu;
    private int i, j, k, m, n, rowID, count, times;
    private long[] end_stats, end_view, end_support, end_themes,end_blogs;
    private boolean loadApp, isView, isSupport;

    @Before("execution(* android.support.v4.app.Fragment+.onStart()) || " +
            "execution(* android.support.v4.app.Fragment+.onRestart())")
    public void logExecutionAspectJ(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
        Fragment fo = (Fragment) target;
        Context c = fo.getActivity();
        if (c != null)
            currContext = c;
    }

    private final String evtsigs = "call(* android.telephony.TelephonyManager.getDeviceId()) || " +
            "call(* android.telephony.TelephonyManager.getImei(..)) || " +
            "call(* android.telephony.TelephonyManager.getSubscriberId()) || " +
            "call(* android.telephony.TelephonyManager.getLine1Number()) ||" +
            "call(* android.telephony.SmsManager.send*TextMessage(..) ) ||" +
            "call(* com.google.android.gms.ads.AdView.*(..)) ||" +
            "execution(* com.google.android.gms.ads.AdListener.*(..)) ||" +
            "call(* android.location.LocationListener+.getLocation()) ||" +
            "call(* android.location.LocationManager.getLastKnownLocation(..)) ||" +
            "call(* android.content.ContentResolver.query(..)) ||" +
            "call(* org.wordpress.android.ui.themes.ThemeBrowserActivity.activateTheme(java.lang.String)) ||" +
            "call(* java.net.URL.openConnection()) ||" +
            "call(* android.content.Context.startActivity(android.content.Intent,..)) ||" +
            "call(* android.app.Activity.startActivityForResult(android.content.Intent,..)) ||" +
            "call(* android.webkit.WebView+.loadUrl(..))";

    public PolicyAspects() {
        loadApp = true;
        Policy privacyPolicy = new PrivacyPolicy("510713763537504", "510713763537504", "1-234-567-8900");
        Policy adPolicy = new BlockAdsPolicy();
        Policy locPolicy = new BlockingDeviceLocation();
        Policy callInfoPolicy = new CallInfoPolicy();
        Policy internetPolicy = new InternetPolicy("blacklist.txt");
        Policy cpuMonitor = new CPUMonitor(0.01, "org.wordpress.android");
        Policy memoryMonitor = new MemoryMonitor(0.01);
        Policy preventPreniumCall = new PreventPreniumCall(null);
        Policy preventSMSAbuse = new PreventSMSAbuse(20);
        Policy themePolicy = new ThemePolicy();

        Policy[] policies = new Policy[]{internetPolicy, privacyPolicy, adPolicy, locPolicy, callInfoPolicy, cpuMonitor, memoryMonitor, preventPreniumCall, preventSMSAbuse, themePolicy};

        monitor = new Monitor(policies);
        i = j = k = m = n = 0;
        end_stats = new long[100];
        end_themes = new long[100];
        end_blogs = new long[100];
        end_support = new long[100];
        end_view = new long[100];
        count = 1;
    }

//    loading time
    @Before("execution(* org.wordpress.android.ui.WPLaunchActivity.onCreate(android.os.Bundle)) ")
    public void showMainActivity(JoinPoint joinPoint) {
        times = 0;
        loading = System.currentTimeMillis();
    }

    @After("execution(* org.wordpress.android.ui.main.WPMainActivity.onResume())")
    public void launchWPMainActivity(JoinPoint joinPoint) {
        if (loadApp) {
            loading = System.currentTimeMillis() - loading;
            System.out.println("time for loading the application:" + times);
            loadApp = false;
        }
    }

    @Before("execution(* android.view.View.OnClickListener+.onClick(android.view.View)) && args(view)")
    public void b4Theme(JoinPoint joinPoint, android.view.View view) {
        start_row = System.currentTimeMillis();
        rowID = view.getId();
        times = 0;
    }

    //themes loading
    @After("execution(* org.wordpress.android.ui.themes.ThemeBrowserActivity.onResume())")
    public void themeLoaded(JoinPoint joinPoint) {
        if(rowID == R.id.row_themes) {
            long span = System.currentTimeMillis() - start_row;
            rowID = 0;
            if(i < 100) {
                end_themes[i++] = span;
                System.out.println("time for loading the themes:" +times);
            }
        }
    }

    //stats loaded
    @After("execution(* org.wordpress.android.ui.stats.StatsActivity.onResume())")
    public void statsLoaded(JoinPoint joinPoint) {
        if(rowID == R.id.row_stats) {
            long span = System.currentTimeMillis() - start_row;
            rowID = 0;
            if(j < 100) {
                end_stats[j++] = span;
                System.out.println("time for loading the states:" + times);
            }
        }
    }

    //blog loaded
    @After("execution(* org.wordpress.android.ui.posts.PostsListFragment.onResume())")
    public void blogLoaded(JoinPoint joinPoint) {
        if(rowID == R.id.row_blog_posts) {
            long span = System.currentTimeMillis() - start_row;
            rowID = 0;
            if( k < 100) {
                end_blogs[k++] = span;
                System.out.println("time for loading the blogs:" + times);
            }
        }
    }

    @After("execution(* org.wordpress.android.ui.media.MediaBrowserActivity.onResume())")
    public void actvLoaded(JoinPoint joinPoint) {
        printPerformance();
    }

    //loading a specific theme
    @Before(" execution(* *.onMenuItemClick(android.view.MenuItem))  && args(i)")
    public void b4Menu(MenuItem i) {
        times = 0;
        start_menu = System.currentTimeMillis();
        if (i.getItemId() == R.id.menu_support) {
            isSupport = true;
        } else if (i.getItemId() == R.id.menu_view) {
            isView = true;
        }
    }

    @Around("execution(* *.onPageFinished(*, java.lang.String)) && args(*, url)")
    public void onPageFinished(JoinPoint joinPoint, String url) throws Throwable {
        if( isView && !url.endsWith("wordpress.com/wp-login.php") ) {
            long span = System.currentTimeMillis() - start_menu;
            isView = false;
            if(n < 100) {
                end_view[n++] = span;
                System.out.println("time for loading view:" + times);
            }
        }
        else if(isSupport) {
            if(count == 3) {
                isSupport = false;
                trigger = new Result(joinPoint, null);
                monitor.processTrigger(trigger, currContext);
                long span = System.currentTimeMillis() - start_menu;
                count = 1;
                if (m < 100) {
                    end_support[m++] = span;
                    System.out.println("time for loading the support:" + ++times);
                }
            }else
                count++;
        }
    }

    @Around(evtsigs)
    public Object aroundJoinPoint(final ProceedingJoinPoint joinPoint) throws Throwable {
        if (monitor.isLocked4Oblig()) {
            Object obj = joinPoint.proceed();
            monitor.getRtrace().addRes(new Result(joinPoint, obj));
            return obj;
        } else {
            times+=2;
            trigger = new Action(joinPoint);
           // Log.d(TAG, trigger.getEvtTyp() + "----------"+ trigger.getEvtSig() + ": " + joinPoint.getSourceLocation());
            returnRes = null;
            monitor.processTrigger(new Action(joinPoint), currContext);

            if (Policy.outputNotSet() || trigger.equals(Policy.getOutput())) {
                returnRes = joinPoint.proceed();
            }else{
                Event e = Policy.getOutput();
                if(e == null)
                    killProcess();

                if(e.getEventTyp() == EvtTyp.ACTION) {
                    returnRes = ((Action) e).execute();
                }else {
                    Result result = ((Result) e);
                    if (result.getEvtSig() == null && result.getEvtRes() == null)
                        System.exit(-1);
                    returnRes = ((Result) e).getEvtRes();
                }
            }

            trigger = new Result(joinPoint, returnRes);
            monitor.processTrigger(trigger, currContext);

            if (Policy.outputNotSet()) {
                return returnRes;
            } else {
                Event e = Policy.getOutput();
                if (e == null)
                    returnRes = e;
                else
                    returnRes = e.getEventTyp() == EvtTyp.RESULT ? ((Result) e).getEvtRes()
                                                                 : ((Action) e).execute();
            }
            return returnRes;
        }
    }

    public void killProcess() {
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
        System.exit(0);
    }

    public void printPerformance() {
        System.out.println("time for loading the application: " + loading + "\n");

        System.out.print("time for loading the themes: ");
        for(int x = 0; x <= i; x++) System.out.println(end_themes[x]);
        System.out.println("");

        System.out.print("time for loading the stats: ");
        for(int x = 0; x <= j; x++) System.out.println(end_stats[x]);
        System.out.println("");

        System.out.print("time for loading the blogs: ");
        for(int x = 0; x <= k; x++) System.out.println(end_blogs[x]);
        System.out.println("");

        System.out.print("time for loading the support: ");
        for(int x = 0; x <= m; x++) System.out.println(end_support[x]);
        System.out.println("");

        System.out.print("time for loading the view: ");
        for(int x = 0; x <= n; x++) System.out.println(end_view[x]);
        System.out.println("");
    }
}