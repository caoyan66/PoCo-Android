package org.wordpress.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ProfilingUtils;
import org.wordpress.android.util.ToastUtils;

public class WPLaunchActivity extends AppCompatActivity {
    /*
     * this the main (default) activity, which does nothing more than launch the
     * previously active activity on startup - note that it's defined in the
     * manifest to have no UI
     */

    private static String[] PERMISSIONS_STORAGE = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 1;
    private void checkPermission(String permission, String[] permissionStr, int request) {
        if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, permissionStr, request);
    }
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    ;
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfilingUtils.split("WPLaunchActivity.onCreate");
        checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSIONS_STORAGE, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        launchWPMainActivity();
    }

    private void launchWPMainActivity() {
        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        Intent intent = new Intent(this, WPMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
