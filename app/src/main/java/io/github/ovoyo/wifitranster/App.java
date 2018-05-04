package io.github.ovoyo.wifitranster;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;


public class App extends Application {

    private static final String APP_ID = "7bd231e816";

    @Override
    public void onCreate() {
        super.onCreate();

        CrashReport.initCrashReport(getApplicationContext(), APP_ID, BuildConfig.DEBUG);
    }
}
