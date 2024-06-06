package com.geometris.wqlib;

import android.content.IntentFilter;

/**
 * Created by bipin_2 on 1/25/2018.
 */

public class WherequbeModel {
    public static final String TAG = "Geometris";
    public static final int BLE_NUS_MAX_DATA_LEN = 20;
    public static final String ACTION_GATT_CONNECTED = "com.geometris.WQ.ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = "com.geometris.WQ.ACTION_GATT_DISCONNECTED";
    public static final String ACTION_GATT_CONNECTION_FAILED = "com.geometris.WQ.ACTION_GATT_CONNECTION_FAILED";
    public static final String ACTION_GATT_SERVICES_DISCOVERED = "com.geometris.WQ.ACTION_GATT_SERVICES_DISCOVERED";
    public static final String ACTION_DATA_AVAILABLE = "com.geometris.WQ.ACTION_OBD_AVAILABLE";
    public static final String EXTRA_DATA = "com.geometris.WQ.EXTRA_DATA";
    public static final String DEVICE_DOES_NOT_SUPPORT_OBD = "com.geometris.WQ.DEVICE_DOES_NOT_SUPPORT_OBD";
    public static final String DEVICE_SYNC_OK = "com.geometris.WQ.DEVICE_SYNC_OK";
    public static final String DEVICE_SYNC_FAILURE = "com.geometris.WQ.DEVICE_SYNC_FAIL";
    // public DeviceInfo mDevInfo = null;
    protected boolean mDevMode = false;

    WherequbeModel() {
    }

    protected static IntentFilter getWherequbeEventsIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.geometris.WQ.ACTION_GATT_CONNECTED");
        intentFilter.addAction("com.geometris.WQ.ACTION_GATT_DISCONNECTED");
        intentFilter.addAction("com.geometris.WQ.ACTION_GATT_CONNECTION_FAILED");
        intentFilter.addAction("com.geometris.WQ.ACTION_GATT_SERVICES_DISCOVERED");
        intentFilter.addAction("com.geometris.WQ.DEVICE_DOES_NOT_SUPPORT_OBD");
        intentFilter.addAction("com.geometris.WQ.DEVICE_SYNC_OK");
        intentFilter.addAction("com.geometris.WQ.DEVICE_SYNC_FAIL");
        return intentFilter;
    }
}
