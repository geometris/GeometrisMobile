package com.geometris.wqlib;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 *  Singleton provides a service to send and receive data from Whereqube devices,
 *  wrapping a WherequbeService object during initialization.
 */
public class WQSmartService extends Service {


    private static final String TAG = "Geometris";

    public static final int MESSAGE_SCAN_RESULT = 1;
    public static final int MESSAGE_CONNECTED = 2;
    public static final int MESSAGE_CHARACTERISTIC_VALUE = 3;
    public static final int MESSAGE_DISCONNECTED = 4;
    public static final int MESSAGE_REQUEST_FAILED = 5;
    public static final int MESSAGE_CHARACTERISTIC_READ = 6;
    public static final int MESSAGE_CHARACTERISTIC_WRITE = 7;
    public static final int MESSAGE_DESCRIPTOR_READ = 8;
    public static final int MESSAGE_DESCRIPTOR_WRITE = 9;
    public static final int MESSAGE_DESCRIPTOR_VALUE = 10;
    public static final int MESSAGE_WRITE_COMPLETE = 11;

    // // Keys to use for sending extra data with above messages.
    public static final String EXTRA_SCAN_RECORD = "SCANRECORD";
    public static final String EXTRA_VALUE = "CVALUE";
    public static final String EXTRA_RSSI = "RSSI";
    public static final String EXTRA_APPEARANCE_KEY = "APPEARKEY";
    public static final String EXTRA_APPEARANCE_NAME = "APPEARNAME";
    public static final String EXTRA_APPEARANCE_ICON = "APPEARICON";
    public static final String EXTRA_SERVICE_UUID = "SERVUUID";
    public static final String EXTRA_CHARACTERISTIC_UUID = "CHARUUID";
    public static final String EXTRA_DESCRIPTOR_UUID = "DESCUUID";
    public static final String EXTRA_REQUEST_ID = "REQUESTID";
    public static final String EXTRA_CLIENT_REQUEST_ID = "CLIENTREQUESTID";


    public static final int APPEARANCE_UNKNOWN = 0;

    private WherequbeService mWherequbeService = null;

    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBtManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private String mBluetoothDeviceAddress =null;
    public BluetoothGatt mGattClient = null;
    private int mConnectionState = BluetoothAdapter.STATE_DISCONNECTED;

    /**
     * Broadcast receiver for bluetooth status
     */
    BluetoothStateObserver mBluetoothStateObserver;

    // Characteristic currently waiting to have a notification value written to it.
    private BluetoothGattCharacteristic mPendingCharacteristic = null;

    Queue<WQSmartRequest> requestQueue = new LinkedList<WQSmartRequest>();

    WQSmartRequest currentRequest = null;

    private OBDDataInfo obdDataInfo= null;

    public void initOBDDataInfo() {
        obdDataInfo = new OBDDataInfo();
    }

    public void LogMessage(String tag, String msg)
    {
        //if(GeoPreferences.debug)
        Log.d(tag, msg);
    }

    /**
     * Disconnect from Gatt client
     * @see BluetoothGattCallback
     */
    public void disconnectOnStateChange (int status) {

        if (mConnectionState == BluetoothProfile.STATE_DISCONNECTED) {
            return;
        }

        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        Log.d(TAG, "WQSS: Device disconnectOnStateChange() " + status);

        refreshDeviceCache();
        requestQueue.clear();
        currentRequest = null;
        if(mGattClient != null) {
            mGattClient.close();
            mGattClient = null;
        }

        String intentAction;

        if (status == 62) {
            // Error BLE_HCI_CONN_FAILED_TO_BE_ESTABLISHED thrown by BluetoothGatt
            Log.w(TAG, "WQSS: Status code 62 returned by GATT server");
            Log.w(TAG, "WQSS: Connection failed, should try to reconnect to device");
            intentAction = "com.geometris.WQ.ACTION_GATT_CONNECTION_FAILED";
        } else {
            intentAction = "com.geometris.WQ.ACTION_GATT_DISCONNECTED";
            Log.d(TAG, "WQSS: Disconnected from GATT server.");
        }

        WQSmartService.this.broadcastUpdate(intentAction);
    }

    class BluetoothStateObserver extends AbstractBluetoothStateObserver {

        @Override
        public void onEnabled() {
            Log.d(TAG, "WQSS: Bluetooth onEnabled()");
        }

        @Override
        public void onDisabled() {
            Log.d(TAG, "WQSS: Bluetooth onDisabled() disconnectOnStateChange");
            disconnectOnStateChange(99999);  // random value
        }
    }

    /**
     * This is where most of the interesting stuff happens in response to changes in BLE state for a client.
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LogMessage(TAG, "onConnectionStateChange state: "+newState+ " status: "+status);
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED && mGattClient != null) {
                requestQueue.clear();
                currentRequest = null;
                intentAction = "com.geometris.WQ.ACTION_GATT_CONNECTED";
                mConnectionState =  BluetoothProfile.STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(TAG, "WQSS: Connected to GATT server.");
                Log.d(TAG, "WQSS: Attempting to start service discovery:" + mGattClient.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "WQSS: onConnectionStateChange disconnectOnStateChange()");
                disconnectOnStateChange(status);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "WQSS: onServicesDiscovered mBluetoothGatt = " + mGattClient);
                broadcastUpdate("com.geometris.WQ.ACTION_GATT_SERVICES_DISCOVERED");
                mWherequbeService.mMHT.serviceDiscovered();
            }
            else
            {
                Log.d(TAG, "WQSS: onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            onData("com.geometris.WQ.ACTION_OBD_AVAILABLE", characteristic);
        }

        /**
         * After calling registerForNotification this callback should trigger, and then we can perform the actual
         * enable. It could also be called when a descriptor was requested directly, so that case is handled too.
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (currentRequest.type == WQSmartRequest.RequestType.CHARACTERISTIC_NOTIFICATION) {
                // Descriptor was requested indirectly as part of registration for notifications.
                if(currentRequest.value[0] !=0x00) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        sendMessage(currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                    }
                }

                if (characteristic.getService().getUuid().compareTo(mPendingCharacteristic.getService().getUuid()) == 0
                        && characteristic.getUuid().compareTo(mPendingCharacteristic.getUuid()) == 0) {
                    boolean enablevalue = true;
                    if(currentRequest.value[0] ==0x00) {
                        enablevalue = false;
                    }
                    if (!enableNotification(enablevalue, characteristic)) {
                        sendMessage(currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                    }
                    // Don't call processNextRequest yet as this request isn't
                    // complete until onDescriptorWrite() triggers.
                }
            } else if (currentRequest.type == WQSmartRequest.RequestType.READ_DESCRIPTOR) {
                // Descriptor was requested directly.
                if (status == BluetoothGatt.GATT_SUCCESS) {
                } else {
                    sendMessage( currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                }
                // This request is now complete, so see if there is another.
                processNextRequest();
            }
        }

        /**
         * After writing the CCC for a notification this callback should trigger. It could also be called when a
         * descriptor write was requested directly, so that case is handled too.
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            if (currentRequest.type == WQSmartRequest.RequestType.CHARACTERISTIC_NOTIFICATION) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                }
            } else if (currentRequest.type == WQSmartRequest.RequestType.WRITE_DESCRIPTOR) {
                // TODO: If descriptor writing is implemented, add code here to send message to handler.
            }
            processNextRequest();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // This can only be in response to the current request as there can't be more than one in progress.
            // So check this is what we were expecting.
            if (currentRequest.type == WQSmartRequest.RequestType.READ_CHARACTERISTIC) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onData("com.geometris.WQ.ACTION_OBD_AVAILABLE", characteristic);
                }
                else
                {
                    sendMessage(currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                }
                processNextRequest();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (currentRequest.type == WQSmartRequest.RequestType.WRITE_CHARACTERISTIC) {
                if(status == BluetoothGatt.GATT_SUCCESS)
                    sendMessage(currentRequest.requestId, MESSAGE_WRITE_COMPLETE);
                else
                    sendMessage(currentRequest.requestId, MESSAGE_REQUEST_FAILED);

                processNextRequest();
            }
        }

    };
    /**
     * Class used for the client Binder. Because we know this service always runs in the same process as its clients, we
     * don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public WQSmartService getService() {
            // Return this instance of WQSmartService so clients can call public
            // methods.
            return WQSmartService.this;
        }
    }

    public WQSmartService() {
    }

    private void broadcastUpdate(String action) {
        Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onData(String action, BluetoothGattCharacteristic characteristic) {
        new Intent(action);

        // A notification for a characteristic has been received, so notify
        // the registered Handler.
        UUID serviceUUID = characteristic.getService().getUuid();
        UUID characteristicUUID = characteristic.getUuid();
        if (serviceUUID.compareTo(WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid()) == 0
                && characteristicUUID.compareTo(WQSmartService.WQSmartUuid.OBD_MEASUREMENT.getUuid()) == 0) {
            byte[] values = characteristic.getValue();
            obdDataInfo.insertPacket(values);
            if(obdDataInfo.isFull())
            {
                GeoData geoData = obdDataInfo.getGeoData();
                BaseRequest bs = new BaseRequest(BaseRequest.OBD_MEASUREMENT, WQSmartService.MESSAGE_CHARACTERISTIC_VALUE, (Object) geoData);
                mWherequbeService.mMHT.onMessage(bs);
                initOBDDataInfo();
            }
        }
        else {

            if(currentRequest.requestId == BaseRequest.REQUEST_DEVICE_ADDRESS
                    && serviceUUID.compareTo(WQSmartService.WQSmartUuid.OBD_SERVICE.getUuid()) == 0
                    && characteristicUUID.compareTo(WQSmartService.WQSmartUuid.OBD_DEVICE_ADDRESS.getUuid()) == 0
                    ) {
                byte[] values = characteristic.getValue();
                if(values.length == 6 ) {
                    String address = "";
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (count=values.length; count>0;count--) {
                        if (count == values.length)
                            sb.append(String.format("%02x", values[count-1]));
                        else
                            sb.append(String.format(":%02x", values[count-1]));

                    }
                    address = new String(sb);
                    BaseResponse bs= new BaseResponse(BaseResponse.REQUEST_DEVICE_ADDRESS, WQSmartService.MESSAGE_CHARACTERISTIC_VALUE, (Object) new DeviceAddress(address));
                    mWherequbeService.mMHT.onResponse(bs);
                }
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public boolean onUnbind(Intent intent) {
        this.close();
        return super.onUnbind(intent);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    public boolean initialize(WherequbeService wqService) {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        mBluetoothStateObserver = new BluetoothStateObserver();
        registerReceiver(mBluetoothStateObserver, intentFilter);

        if(this.mBtManager == null) {
            this.mBtManager = (BluetoothManager)this.getSystemService(Context.BLUETOOTH_SERVICE);
            if(this.mBtManager == null) {
                Log.e(TAG, "WQSS: Unable to initialize BluetoothManager.");
                return false;
            }
        }

        this.mBtAdapter = this.mBtManager.getAdapter();
        if(this.mBtAdapter == null) {
            Log.e(TAG, "WQSS: Unable to obtain a BluetoothAdapter.");
            return false;
        } else {
            this.mWherequbeService = wqService;
            return true;
        }
    }

    /**
     * Initialise the service.
     */
    @Override
    public void onCreate() {
        if (mBtAdapter == null) {
            mBtManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBtAdapter = mBtManager.getAdapter();
        }
    }

    /**
     * When the service is destroyed, make sure to close the Bluetooth connection.
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "WQSS: onDestroy");
        if (mGattClient != null)
            mGattClient.close();

        if (mBluetoothStateObserver != null) {
            unregisterReceiver(mBluetoothStateObserver);
        }
        super.onDestroy();
    }

    /**
     * Connect to a remote Bluetooth Smart device. The deviceHandler will receive MESSAGE_CONNECTED on
     * connection success.
     */
    public void refreshDeviceCache(){//BluetoothGatt gatt) {
        try {
            if(mGattClient==null) return;
            Method localMethod = mGattClient.getClass().getMethod("refresh");
            if(localMethod != null) {
                localMethod.invoke(mGattClient);
            }
        } catch(Exception localException) {
            LogMessage(TAG, "Exception refreshing BT cache: "+ localException.toString());
        }
    }


    public void refreshDeviceBond(){
        Method method = null;
        try {
            if(mGattClient==null)
                return;
            method = mGattClient.getDevice().getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(mGattClient.getDevice(), (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mGattClient.disconnect();

    }

    public boolean isConnected() {
        return this.mConnectionState == BluetoothAdapter.STATE_CONNECTED;
    }
    public boolean connect(String address) {
        if(this.mBtAdapter != null && address != null) {
            if(this.mBluetoothDeviceAddress != null && address.equals(this.mBluetoothDeviceAddress) && this.mGattClient != null) {
                Log.d(TAG, "WQSS: Trying to use an existing mBluetoothGatt for connection.");
                if(this.mGattClient.connect()) {
                    this.mConnectionState = BluetoothAdapter.STATE_CONNECTING;
                    return true;
                } else {
                    return false;
                }
            } else {
                BluetoothDevice device = this.mBtAdapter.getRemoteDevice(address);
                Log.d(TAG, "WQSS: Creating new mBluetoothGatt for connection.");

                if(device == null) {
                    Log.w(TAG, "WQSS: Device not found.  Unable to connect.");
                    return false;
                } else {

                     if (Build.VERSION.SDK_INT >= 23) {
                        Log.w(TAG, "WQSS: Using TRANSPORT_LE parameter");

                        // Fix added for issue where BluetoothGatt would throw status=62
                        // using TRANSPORT_LE parameter
                        this.mGattClient = device.connectGatt(this, false, this.mGattCallbacks, 2);
                    } else {
                        Log.w(TAG, "WQSS: Without using TRANSPORT_LE parameter");

                        this.mGattClient = device.connectGatt(this, false, this.mGattCallbacks);
                    }

                    this.refreshDeviceCache();
                    Log.d(TAG, "WQSS: Trying to create a new connection.");
                    this.mBluetoothDeviceAddress = address;
                    this.mConnectionState = BluetoothAdapter.STATE_CONNECTING;
                    return true;
                }
            }
        }
        else {
            Log.w(TAG, "WQSS: BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
    }


    public void disconnect() {
        if(this.mBtAdapter != null && this.mGattClient != null) {
            this.mGattClient.disconnect();
        } else {
            Log.w(TAG, "WQSS: BluetoothAdapter not initialized");
        }
    }

    public void close() {
        if(this.mGattClient != null) {
            Log.d(TAG, "WQSS: mBluetoothGatt closed");
            this.mBluetoothDeviceAddress = null;
            this.mGattClient.close();
            this.mGattClient = null;
        }
    }


    public boolean isCharacterisiticExists( UUID serviceUuid, UUID characteristicUuid)
    {
        if(!isConnected() || mGattClient == null) {
            Log.w(TAG, "WQSS: ICE BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService serviceObject = mGattClient.getService(serviceUuid);
        if (serviceObject != null) {
            mPendingCharacteristic = serviceObject.getCharacteristic(characteristicUuid);
            if (mPendingCharacteristic != null) {
                return true;
            }
        }
        return false;
    }
    /**
     * Enable notifications for a particular characteristic and register a handler for those notifications. If a request
     * is currently in progress then queue it.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param serviceUuid
     *            The service that contains the characteristic of interest.
     * @param characteristicUuid
     *            The characteristic to register for.
     * @param value
     *            Characteristic value

     */
    public void requestCharacteristicNotification(int requestId, UUID serviceUuid, UUID characteristicUuid, byte[] value) {
        if (currentRequest == null) {
            performNotificationRequest(requestId, serviceUuid, characteristicUuid, value);
        } else {
            requestQueue.add(new WQSmartRequest(WQSmartRequest.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, serviceUuid,
                    characteristicUuid, null,  value));
        }
    }

    /**
     * Request the current value of a characteristic. This will return the value once only in a
     * MESSAGE_CHARACTERISTIC_VALUE. If a request is currently in progress then queue it.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param service
     *            The UUID of the service that contains the characteristic of interest.
     * @param characteristic
     *            The UUID of the characteristic.

     */
    public void requestCharacteristicValue(int requestId, UUID service, UUID characteristic) {
        if (currentRequest == null) {
            performCharacValueRequest(requestId, service, characteristic);
        } else {
            requestQueue.add(new WQSmartRequest(WQSmartRequest.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                    null));
        }
    }

    /**
     * Request the current value of a descriptor. This will return the value once only in a MESSAGE_DESCRIPTOR_VALUE. If
     * a request is currently in progress then queue it. Use requestCharacteristicNotification() for constant updates
     * when a characteristic value changes.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param service
     *            The UUID of the service that contains the characteristic and descriptor of interest.
     * @param characteristic
     *            The UUID of the characteristic.
     * @param descriptor
     *            The UUID of the descriptor.

     */
    public void requestDescriptorValue(int requestId, UUID service, UUID characteristic, UUID descriptor) {
        if (currentRequest == null) {
            performDescValueRequest(requestId, service, characteristic, descriptor);
        } else {
            requestQueue.add(new WQSmartRequest(WQSmartRequest.RequestType.READ_DESCRIPTOR, requestId, service, characteristic,
                    descriptor));
        }
    }

    /**
     * Write a value to a charactersitic.
     * @param requestId
     *         An id to uniquely identify the request. Included in messages to the handler.
     * @param service
     *         The service that contains the characteristic to write.
     * @param characteristic
     *         The characteristic to write.
     * @param value
     *         The value to write to the characteristic.

     */
    public void writeCharacteristicValue(int requestId, UUID service, UUID characteristic, byte[] value){
        if (currentRequest == null) {
            performCharacWrite(requestId, service, characteristic,  value);
        }
        else {
            requestQueue.add(new WQSmartRequest(WQSmartRequest.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic,
                    null,  value));
        }
    }


    /**
     * Helper function to send a message to a handler with no parameters except the request ID.
     *

     * @param msgId
     *            The message identifier to send. Use one of the defined constants.
     * @param requestId
     *            The request ID provided by the client of this Service.
     */
    private void sendMessage(int requestId, int msgId) {

        if(requestId == BaseRequest.OBD_MEASUREMENT) {
            BaseRequest bs = new BaseRequest(requestId, msgId);
            mWherequbeService.mMHT.onMessage(bs);
        }
        else if(requestId == BaseRequest.WRITE_APP_IDENTIFIER){
            BaseRequest bs = new BaseRequest(requestId, msgId);
            mWherequbeService.mMHT.onWriteAppIdentifier(bs);
        }
        else
        {
            BaseResponse bs = new BaseResponse(requestId, msgId);
            mWherequbeService.mMHT.onResponse(bs);
        }

    }


    /**
     * Process the next request in the queue for some BLE action (such as characteristic read). This is required because
     * the Android 4.3 BLE stack only allows one active request at a time.
     */
    private void processNextRequest() {
        if (requestQueue.isEmpty()) {
            currentRequest = null;
            return;
        }
        WQSmartRequest request = requestQueue.remove();
        switch (request.type) {
            case CHARACTERISTIC_NOTIFICATION:
                performNotificationRequest(request.requestId, request.serviceUuid, request.characteristicUuid, request.value);
                break;
            case READ_CHARACTERISTIC:
                performCharacValueRequest(request.requestId, request.serviceUuid, request.characteristicUuid);
                break;
            case READ_DESCRIPTOR:
                performDescValueRequest(request.requestId, request.serviceUuid, request.characteristicUuid,
                        request.descriptorUuid);
                break;
            case WRITE_CHARACTERISTIC:
                performCharacWrite(request.requestId, request.serviceUuid, request.characteristicUuid, request.value);
            default:
                break;
        }
    }

    /**
     * Perform the notification request now.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param service
     *            The service that contains the characteristic of interest.
     * @param characteristic
     *            The characteristic to register for.

     */
    private void performNotificationRequest(int requestId, UUID service, UUID characteristic,  byte[] value) {
        // This currentRequest object will be used when we get the value back asynchronously in the callback.
        currentRequest = new WQSmartRequest(WQSmartRequest.RequestType.CHARACTERISTIC_NOTIFICATION, requestId, service,
                characteristic, null,  value);
        if(!isConnected() || mGattClient == null){
            Log.w(TAG, "WQSS: PNR BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattService serviceObject = mGattClient.getService(currentRequest.serviceUuid);
        if (serviceObject != null) {
            mPendingCharacteristic = serviceObject.getCharacteristic(characteristic);
            if (mPendingCharacteristic != null) {
                BluetoothGattDescriptor clientCharacteristicConfig = mPendingCharacteristic
                        .getDescriptor(WQSmartUuid.CCC.value);
                // If the CCC exists then attempt to read it.
                if (clientCharacteristicConfig == null || !mGattClient.readDescriptor(clientCharacteristicConfig)) {
                    // CCC didn't exist or the read failed early.
                    // Send the failed message and move onto the next request.
                    sendMessage( currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                    processNextRequest();
                }
            }
        }
    }


    /**
     * Perform the characteristic value request now.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param service
     *            The service that contains the characteristic of interest.
     * @param characteristic
     *            The characteristic to get the value of.

     */
    private void performCharacValueRequest(int requestId, UUID service, UUID characteristic) {
        // This currentRequest object will be used when we get the value back asynchronously in the callback.
        if(!isConnected() || mGattClient == null) {
            // throw new NullPointerException("GATT client not started.");
            Log.w(TAG, "WQSS: PCVR BluetoothAdapter not initialized");
        }
        currentRequest = new WQSmartRequest(WQSmartRequest.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic, null);
        BluetoothGattService serviceObject = mGattClient.getService(service);
        if (serviceObject != null) {
            BluetoothGattCharacteristic characteristicObject = serviceObject.getCharacteristic(characteristic);
            if (characteristicObject != null) {
                if (!mGattClient.readCharacteristic(characteristicObject)) {
                    sendMessage( currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                    processNextRequest();
                }
            }
        }
    }

    /**
     * Perform the descriptor value request now.
     *
     * @param requestId
     *            An id provided by the caller that will be included in messages to the handler.
     * @param service
     *            The service that contains the characteristic of interest.
     * @param characteristic
     *            The characteristic that contains the descriptor of interest.
     * @param descriptor
     *            The descriptor to get the value of.

     */
    private void performDescValueRequest(int requestId, UUID service, UUID characteristic, UUID descriptor) {
        // This currentRequest object will be used when we get the value back asynchronously in the callback.
        if(!isConnected() || mGattClient == null) {
            // throw new NullPointerException("GATT client not started.");
            Log.w(TAG, "WQSS: PDVR BluetoothAdapter not initialized");
        }
        currentRequest = new WQSmartRequest(WQSmartRequest.RequestType.READ_CHARACTERISTIC, requestId, service, characteristic,
                descriptor);
        BluetoothGattService serviceObject = mGattClient.getService(service);
        if (serviceObject != null) {
            BluetoothGattCharacteristic characteristicObject = serviceObject.getCharacteristic(characteristic);
            if (characteristicObject != null) {
                BluetoothGattDescriptor descriptorObject = characteristicObject.getDescriptor(descriptor);
                if (descriptorObject != null) {
                    if (!mGattClient.readDescriptor(descriptorObject)) {
                        sendMessage( currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                        processNextRequest();
                    }
                }
            }
        }
    }

    /**
     * Perform the charactersitic write now.
     *
     * @param requestId
     *         An id to uniquely identify the request. Included in messages to the handler.
     * @param service
     *         The service that contains the characteristic to write.
     * @param characteristic
     *         The characteristic to write.

     * @param value
     *         The value to write to the characteristic.
     */
    private void performCharacWrite(int requestId, UUID service, UUID characteristic,  byte[] value) {
        if(!isConnected() || mGattClient == null) {
            Log.w(TAG, "WQSS: PCW BluetoothAdapter not initialized");
            return;
        }
        currentRequest =
                new WQSmartRequest(WQSmartRequest.RequestType.WRITE_CHARACTERISTIC, requestId, service, characteristic, null,
                        value);
        BluetoothGattService serviceObject = mGattClient.getService(service);
        if (serviceObject != null) {
            BluetoothGattCharacteristic characteristicObject = serviceObject.getCharacteristic(characteristic);
            if (characteristicObject != null) {
                characteristicObject.setValue(value);
                if (!mGattClient.writeCharacteristic(characteristicObject)) {
                    sendMessage(currentRequest.requestId, MESSAGE_REQUEST_FAILED);
                    //send Response Back to thread;
                    //send Response Back to thread;
                    processNextRequest();
                }
            }
        }
    }

    /**
     * Write to the CCC (Client characteristic configuration) to enable or disable notifications.
     *
     * @param enable
     *            Boolean indicating whether the notification should be enabled or disabled.
     * @param characteristic
     *            The CCC to write to.
     * @return Boolean result of operation.
     */
    private boolean enableNotification(boolean enable, BluetoothGattCharacteristic characteristic) {
        if(!isConnected() || mGattClient == null) {
            // throw new NullPointerException("GATT client not started.");
            Log.w(TAG, "WQSS: EN BluetoothAdapter not initialized");
            return false;
        }
        if (!mGattClient.setCharacteristicNotification(characteristic, enable)) {
            return false;
        }
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(WQSmartUuid.CCC.value);
        if (clientConfig == null) {
            return false;
        }
        if (enable) {
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        return mGattClient.writeDescriptor(clientConfig);
    }

    public enum WQSmartUuid {
        CCC("00002902-0000-1000-8000-00805f9b34fb"),
        IMMEDIATE_ALERT("00001802-0000-1000-8000-00805f9b34fb"),
        ALERT_LEVEL("00002a06-0000-1000-8000-00805f9b34fb"),
        ALERT_NOTIFICATION_SERVICE("00001811-0000-1000-8000-00805f9b34fb"),
        ALERT_NOTIFICATION_CONTROL_POINT("00002a44-0000-1000-8000-00805f9b34fb"),
        UNREAD_ALERT_STATUS("00002a45-0000-1000-8000-00805f9b34fb"),
        NEW_ALERT("00002a46-0000-1000-8000-00805f9b34fb"),
        NEW_ALERT_CATEGORY("00002a47-0000-1000-8000-00805f9b34fb"),
        UNREAD_ALERT_CATEGORY("00002a48-0000-1000-8000-00805f9b34fb"),
        DEVICE_INFORMATION_SERVICE("0000180A-0000-1000-8000-00805f9b34fb"),
        MANUFACTURER_NAME("00002A29-0000-1000-8000-00805f9b34fb"),
        MODEL_NUMBER("00002a24-0000-1000-8000-00805f9b34fb"),
        SERIAL_NUMBER("00002a25-0000-1000-8000-00805f9b34fb"),
        HARDWARE_REVISION("00002a27-0000-1000-8000-00805f9b34fb"),
        FIRMWARE_REVISION("00002a26-0000-1000-8000-00805f9b34fb"),
        SOFTWARE_REVISION("00002a28-0000-1000-8000-00805f9b34fb"),
        BATTERY_SERVICE("0000180f-0000-1000-8000-00805f9b34fb"),
        BATTERY_LEVEL("00002a19-0000-1000-8000-00805f9b34fb"),
        OBD_SERVICE("00001816-0000-1000-8000-00805f9b34fb"),
        OBD_MEASUREMENT("0002a5b-0000-1000-8000-00805f9b34fb"),
        OBD_FEATURE("00002a5c-0000-1000-8000-00805f9b34fb"),
        OBD_CONTROL_POINT("00002a55-0000-1000-8000-00805f9b34fb"),
        OBD_WQ_DATA_POINT("00002a57-0000-1000-8000-00805f9b34fb"),
        OBD_DEVICE_ADDRESS("00002a59-0000-1000-8000-00805f9b34fb"),
        RSC_SERVICE("00001814-0000-1000-8000-00805f9b34fb"),
        RSC_MEASUREMENT("00002a53-0000-1000-8000-00805f9b34fb"),
        SC_CONTROL_POINT("00002a55-0000-1000-8000-00805f9b34fb");

        public UUID value;

        private WQSmartUuid(String value) {
            this.value = UUID.fromString(value);
        }

        public UUID getUuid() {
            return value;
        }

        public ParcelUuid getParcelable() {
            return new ParcelUuid(this.value);
        }

        // Lookup table to allow reverse lookup.
        private static final HashMap<UUID, WQSmartUuid> lookup = new HashMap<UUID, WQSmartUuid>();

        // Populate the lookup table at load time
        static {
            for (WQSmartUuid s : EnumSet.allOf(WQSmartUuid.class))
                lookup.put(s.value, s);
        }

        /**
         * Reverse look up UUID to WQSmartUuid
         *
         * @param uuid
         *            The UUID to get a enumerated value for.
         * @return Enumerated value of type WQSmartUuid.
         */
        public static WQSmartUuid get(UUID uuid) {
            return lookup.get(uuid);
        }
    }
}
