package io.github.ovoyo.wifitranster;

import android.app.Application;
import android.os.StrictMode;

import com.tencent.bugly.crashreport.CrashReport;


public class App extends Application {

    private static final String APP_ID = "7bd231e816";

    @Override
    public void onCreate() {
        super.onCreate();

        CrashReport.initCrashReport(getApplicationContext(), APP_ID, BuildConfig.DEBUG);

        if (BuildConfig.DEBUG){
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().build());
        }
    }
}
