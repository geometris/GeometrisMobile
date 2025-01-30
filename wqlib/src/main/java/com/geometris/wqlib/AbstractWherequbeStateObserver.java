package com.geometris.wqlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


/**
 * Override this Broadcast receiver to override OnConnected(), onDiscovered(),
 * onError(), onSynced() and onDisconnected()
 * and react to these intents when Geometris WhereQube devices
 * interact via Bluetooth.
 */
public abstract class AbstractWherequbeStateObserver extends BroadcastReceiver {

    private String TAG = "WherequbeStateObserver";

    public AbstractWherequbeStateObserver() {

    }

    /**
     * Receives the intent and routes the data to the appropriate method call,
     * including onConnection(), onDisconnected(), onDiscovered(), onError(),
     * onSynced() -- these must be overridden in a derived class
     *
     * @param context The context in which the receiver is running.
     * @param intent  The Intent being received.
     */
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);

        if (action.equals("com.geometris.WQ.ACTION_GATT_CONNECTED")) {
            this.onConnected();
        } else if (action.equals("com.geometris.WQ.ACTION_GATT_DISCONNECTED")) {
            String statusCode = intent.getStringExtra("reason");
            Log.d("WhereQube", "onReceive ACTION_GATT_DISCONNECTED statusCode: " + statusCode);
            this.onDisconnected(statusCode);
        } else if (action.equals("com.geometris.WQ.ACTION_GATT_SERVICES_DISCOVERED")) {
            this.onDiscovered();
        } else if (action.equals("com.geometris.WQ.ACTION_GATT_CONNECTION_FAILED")) {
            this.onError(new WQError(62, "Unknown"));
        } else {
            int code;
            String cause;
            if (action.equals("com.geometris.WQ.DEVICE_DOES_NOT_SUPPORT_DATA")) {
                code = intent.getIntExtra("errorCode", -8);
                cause = intent.getStringExtra("errorCause");
                this.onError(new WQError(code, cause));
            } else if (action.equals("com.geometris.WQ.DEVICE_SYNC_OK")) {
                this.onSynced();
            } else if (action.equals("com.geometris.WQ.DEVICE_SYNC_FAIL")) {
                this.onError(new WQError());
            }
        }
    }

    /**
     * Will be called when a WhereQube device is discovered on the Bluetooth radio.
     */
    public abstract void onDiscovered();

    /**
     * Will be called when a WhereQube device connects.
     */
    public abstract void onConnected();

    /**
     * Will be called when a WhereQube device synchronizes data.
     */
    public abstract void onSynced();

    /**
     * Will be called when a WhereQube device disconnects.
     */
    public abstract void onDisconnected(String statusCode);

    /**
     * Will be called when retrieval of data from a WhereQube device fails.
     *
     * @param var1 Identifies the specific error.
     * @see WQError
     */
    public abstract void onError(WQError var1);

    /**
     * Registers this broadcast receiver with the intents filter from WherequbeModel.
     *
     * @param context The context in which the receiver is running.
     * @see WherequbeModel
     */
    public void register(Context context) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, WherequbeModel.getWherequbeEventsIntentFilter());
    }

    /**
     * Unregisters this broadcast receiver.
     *
     * @param context The context in which the receiver is running.
     */
    public void unregister(Context context) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
    }
}
