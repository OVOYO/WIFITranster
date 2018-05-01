package io.github.ovoyo.wifitranster;

import android.os.Environment;

public class Constants {

    public static final int HTTP_PORT = 12345;
    public static final String DIR_IN_SDCARD = "WifiTransfer";
    public static final int MSG_DIALOG_DISMISS = 0;
    public static final java.io.File DIR = new java.io.File(Environment.getExternalStorageDirectory() + java.io.File.separator + Constants.DIR_IN_SDCARD);

    public static final class RxBusEventType {
        public static final String POPUP_MENU_DIALOG_SHOW_DISMISS = "POPUP MENU DIALOG SHOW DISMISS";
        public static final String WIFI_CONNECT_CHANGE_EVENT = "WIFI CONNECT CHANGE EVENT";
        public static final String LOAD_BOOK_LIST = "LOAD BOOK LIST";
    }

}
