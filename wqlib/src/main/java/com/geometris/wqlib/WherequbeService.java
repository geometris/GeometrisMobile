package com.geometris.wqlib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

/**
 * Singleton object used to handle communications with a Whereqube device,
 * To be used with a WQSmartService instance.
 */
public class WherequbeService {
    public static final String TAG = "Geometris";
    public static final int DEFAULT_REQ_TIME_OUT = 250;
    private static WherequbeService instance = new WherequbeService();
    protected WQSmartService mService = null;
    protected WherequbeModel mModel = null;
    private boolean mServiceBound = false;


    protected ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService= ((WQSmartService.LocalBinder)rawBinder).getService();
            Log.d(TAG, "TS: onServiceConnected mService= " + mService);
            mServiceBound = true;
            if(!mService.initialize(instance)) {
                Log.e(TAG, "TS: Unable to initialize Bluetooth");
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            Log.e(TAG, "TS: onServiceDisconnected; Cleanup not performed");
            mServiceBound = false;
        }
    };

    protected MsgHandlerThread mMHT = null;

    public WherequbeService() {
    }

    /**
     *
     * @return provides the singleton instance of the service
     */
    public static WherequbeService getInstance() {
        return instance;
    }

    /**
     * Maps a request handler to the given request type, to be invoked by the Whereqube service
     * as messages arrive.
     * @param type  type of message
     * @param rh    object implementing RequestHandler interface to process messages of this type
     */
    public void setReqHandler(int type, RequestHandler rh) {
        this.mMHT.setReqHandler(type, rh);
    }

    /**
     * Registers the service with the application context,
     * initializes all data and starts the message processing thread.
     * @param context context that the service runs in.
     */
    public void initialize(Context context) {
        if(this.mService == null) {
            if(!Wqa.getInstance().isInitialized()) {
                throw new IllegalStateException("Lib is not initialized.");
            } else {
                Intent bindIntent = new Intent(context.getApplicationContext(), WQSmartService.class);
                context.getApplicationContext().bindService(bindIntent, this.mServiceConnection,Context.BIND_AUTO_CREATE);
                this.mModel = new WherequbeModel();
                this.mMHT = new MsgHandlerThread(context.getApplicationContext(), this);
                context.getApplicationContext().startService(bindIntent);
                Log.d(TAG, "WherequbeService: initialize");
            }
        }
    }


    /**
     * Checks whether service is fully initialize to make connection.
     * @return boolean
     */
    public boolean isServiceInitialized(){
        if(!Wqa.getInstance().isInitialized()) {
            return false;
        } else if(this.mService == null) {
            return false;
        }
        return true;
    }

    /**
     * Connects to the Whereqube at the given address.
     * @param address address of the whereqube to connect to
     * @return true if the connection suceeds
     * @throws IllegalArgumentException if the service was not properly initialized.
     */
    public boolean connect(String address) {
        Log.d(TAG, "WherequbeService: connecting ..." + address);
        if(!Wqa.getInstance().isInitialized()) {
            throw new IllegalStateException("Lib is not initialized");
        } else if(this.mService == null) {
            throw new IllegalStateException("Service is not initialized");
        }
        else if(address.isEmpty() || address.equals("") || address == null){
            throw new IllegalArgumentException("Invalid Address");
        } else {
            return this.mService.connect(address);
        }
    }

    /**
     * Disconnects from any connected Whereqube.
     * @throws IllegalArgumentException if the service was not properly initialized.
     */
    public void disconnect() {
        if(this.mService == null) {
            throw new IllegalStateException("Service is not initialized");
        } else {
            this.mService.disconnect();
            //this.mMHT.shutdown();
            Log.d(TAG, "WherequbeService: service disconnected");
        }
    }

    /**
     *
     * @return returns true if connected to a Whereqube device.
     */
    public boolean isConnected() {
        return this.mService != null?this.mService.isConnected():false;
    }

    /**
     *
     * Destroys / Cleans up the WherequbeService object
     * @param context Context in which the service runs.
     */
    public void destroy(Context context) {
        //  this.setTXNotification(Boolean.valueOf(false));
        this.runInForegroundCancel();
        if(this.mMHT != null) {
            this.mMHT.interrupt();
            this.mMHT.quit();
        }

        if(this.mServiceBound) {
            this.mService.stopSelf();
            context.getApplicationContext().unbindService(this.mServiceConnection);
            this.mServiceBound = false;
            this.mService = null;
        }

        Log.d(TAG, "Whereqube: destroyed.");
    }

    protected void shutdown(Context context) {
        this.setTXNotification(Boolean.valueOf(false));
        this.runInForegroundCancel();
        if(this.mMHT != null) {
            this.mMHT.interrupt();
            this.mMHT.quit();
        }

        this.disconnect();
        this.close();
        Log.d(TAG, "WherequbeService: shutdown.");
    }

    /**
     * Closes the service.
     */
    public void close() {
        Log.d(TAG, "WherequbeService: closed.");
        this.mService.close();
    }

    /**
     * Queries what the connected Whereqube is capable of providing
     * @param id Query Request id from BaseRequest class.
     * @return true if the given request id is supported by the connected device.
     * @see BaseRequest
     */
    public boolean isSupported(int id){
        switch(id){
            case BaseRequest.OBD_MEASUREMENT:
                return true;
            case BaseRequest.REQUEST_DEVICE_ADDRESS:
                if(hasSupportVersionTwo())return true;
                break;
            case BaseRequest.REQUEST_START_UDEVENTS:
                if(hasSupportVersionTwo()) return true;
                break;
            case BaseRequest.REQUEST_STOP_UDEVENTS:
                if(hasSupportVersionTwo()) return true;
                break;
            case BaseRequest.PURGE_UDEVENTS:
                if(hasSupportVersionTwo()) return true;
                break;
            case BaseRequest.WRITE_APP_IDENTIFIER:
                if(hasSupportVersionTwo()) return true;
                break;
            default:
                return false;
        }
        return false;
    }

    /**
     * Sends a request to the connected device, and then calls back the response handler
     * @param request   request data
     * @param sh        response handler
     * @param timeout   timeout in ms
     */
    public void sendRequest(BaseRequest request,  ResponseHandler sh, int timeout)
    {
        this.mMHT.sendRequest(request, sh, timeout);
    }

    /**
     * Does this device support version 2 of the BLE protocol?
     * @return true if the device supports version 2.
     */
    protected boolean hasSupportVersionTwo()
    {
        return mService.isCharacterisiticExists(WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(), WQSmartService.WQSmartUuid.OBD_WQ_DATA_POINT.getUuid());
    }

    /**
     * If the connected device supports version 2 of the BLE protocol, will send a request to the device to begin
     * transmitting events for unidentified drivers.
     * @return returns true if the device suppoorts version 2, false otherwise.
     */
    public boolean startTransmittingUnidentifiedDriverMessages()
    {
        if(hasSupportVersionTwo()) {
            if (mServiceBound) {
                mService.writeCharacteristicValue(BaseRequest.REQUEST_START_UDEVENTS, WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(),
                        WQSmartService.WQSmartUuid.OBD_WQ_DATA_POINT.getUuid(), new byte[]{0x02, 0x01});
                return true;
            }
        }
        return false;
    }

    /**
     * If the connected device supports version 2 of the BLE protocol, will send a request to the device to
     * stop transmitting events for unidentified drivers.
     * @return true if the device supports version 2
     */
    public boolean  stopTransmittingUnidentifiedDriverMessages()
    {
        if(hasSupportVersionTwo()) {
            if (mServiceBound) {
                mService.writeCharacteristicValue(BaseRequest.REQUEST_STOP_UDEVENTS, WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(),
                        WQSmartService.WQSmartUuid.OBD_WQ_DATA_POINT.getUuid(), new byte[]{0x02, 0x00});
                return true;
            }
        }
        return  false;
    }

    /**
     * If the connected device supports version 2 of the BLE protocol, requests that unidentified driver events
     * stored on the device be cleared.
     * @return returns true if the device supports version 2.
     */
    public boolean purgeUnidentifiedDriverMessages()
    {
        if(hasSupportVersionTwo()){
            if (mServiceBound) {
                mService.writeCharacteristicValue(BaseRequest.PURGE_UDEVENTS, WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(),
                        WQSmartService.WQSmartUuid.OBD_WQ_DATA_POINT.getUuid(), new byte[]{0x03, 0x01});
                return true;
            }

        }
        return false;
    }

    /**
     * If the connected device supports version 2 of the BLE protocol, sends a request to read the connected device's address
     * @return returns true if the device supports version 2
     */
    public boolean readDeviceAddress()
    {
        if(hasSupportVersionTwo()) {
            if (mServiceBound) {
                if (mService.isCharacterisiticExists(WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(), WQSmartService.WQSmartUuid.OBD_DEVICE_ADDRESS.getUuid())) {
                    Log.d(TAG, "Device Address Characteristics exists:");
                    mService.requestCharacteristicValue(BaseRequest.REQUEST_DEVICE_ADDRESS, WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(),
                            WQSmartService.WQSmartUuid.OBD_DEVICE_ADDRESS.getUuid());
                    return true;
                }
            }
        }
        return false;
    }


    protected void sendAppIdentification(){
        mService.writeCharacteristicValue(BaseRequest.WRITE_APP_IDENTIFIER, WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(), WQSmartService.WQSmartUuid.OBD_WQ_DATA_POINT.getUuid(), new byte[]{0x01,0x02});
    }

    protected void setTXNotification(Boolean flag) {
        if(this.mService != null && this.mService.isConnected()) {
            if(flag) {
                mService.initOBDDataInfo();
                mService.requestCharacteristicNotification(BaseRequest.OBD_MEASUREMENT,
                        WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(), WQSmartService.WQSmartUuid.OBD_MEASUREMENT.getUuid(),
                        new byte[]{0x01});
            }
            else {
                mService.initOBDDataInfo();
                mService.requestCharacteristicNotification(BaseRequest.OBD_MEASUREMENT,
                        WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid(), WQSmartService.WQSmartUuid.OBD_MEASUREMENT.getUuid(),
                        new byte[]{0x00});
            }
        }

    }

    protected WherequbeModel getModel() {
        return this.mModel;
    }

    public void runInForeground(Context context, PendingIntent pendingIntent) {
        if(this.mService != null && this.mService.isConnected()) {
            Log.d(TAG, "WherequbeService: US in f/g"); // WTF does nothing

        }

    }

    public void runInForegroundCancel() {
        if(this.mService != null) {
            Log.d(TAG, "Whereqube: US f/g cancel");
            this.mService.stopForeground(true);
        }

    }




}
