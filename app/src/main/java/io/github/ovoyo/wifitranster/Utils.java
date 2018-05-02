package io.github.ovoyo.wifitranster;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {

    private static final SimpleDateFormat DATE_FORMAT =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private Utils() {
    }

    public static String getWifiIp(Context context) {
        WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifimanager == null) return null;
        WifiInfo wifiInfo = wifimanager.getConnectionInfo();
        if (wifiInfo != null && wifiInfo.getIpAddress() > 0) {
            return intToIp(wifiInfo.getIpAddress());
        }
        return null;
    }

    private static String intToIp(int i) {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
    }

    public static int getWifiState(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return wifiManager.getWifiState();
        }
        return WifiManager.WIFI_STATE_DISABLED;
    }

    public static NetworkInfo.State getWifiConnectState(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return NetworkInfo.State.UNKNOWN;
        NetworkInfo mWiFiNetworkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.getState();
        }
        return NetworkInfo.State.DISCONNECTED;
    }

    public static String getFileSizeString (long fileSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        String size = "";
        if (fileSize < 1024) {
            size = df.format((double) fileSize) + "B";
        } else if (fileSize < 1024 * 1024) {
            size = df.format((double) fileSize / 1024) + "K";
        } else if (fileSize < 1024 * 1024 * 1024) {
            size = df.format((double) fileSize / 1024 / 1024) + "M";
        } else if (fileSize < 1024L * 1024 * 1024 * 1024) {
            size = df.format((double) fileSize / 1024 / 1024 / 1024) + "G";
        }
        return size;
    }

    public static String formatDate(long time){
        return DATE_FORMAT.format(new Date(time));
    }
}
