# Geometris Mobile Resources

To add the library as a dependency to your project, 

1. Add the jitpack maven to your project root level build script:

```
allprojects {
    repositories {
        google()
        jcenter()

        maven { url 'https://jitpack.io' }
    }
}
```

2. Add a dependency to the library in the modules that need it, along with additional
dependencies you will likely need to use the library:

```
dependencies {
    
    ...
      

    implementation 'com.android.support:support-v4:26.1.0'
    implementation 'joda-time:joda-time:2.9.9'

    compile 'com.github.geometris:GeometrisMobile:1.0.3'
}
```



# Geometris Whereqube Bluetooth Integration Library

This library allows for Android applications to receive data from Geometris Whereqube telematics devices, including data about vehicle engine hours, speed, location, vin, etc.

This supports usage for applications that implement the US Federal Motor Carrier Safety Administration regulation compliance for the ELD mandate (see https://www.fmcsa.dot.gov/hours-service/elds/faqs )

## Using the Library

To use the library, you will need to address these points of integration:

1. Initialization and Cleanup
2. Scanning for WhereQubes
3. Data transfer, sending requests and receiving data from a connected Whereqube

We recommend that scanning and data transfer be handled as separate activities.

## Initialization and Cleanup of the Library


First, the service needs to be declared in the application's AndroidManifestxml file:

```
<application>

<service
    android:name="com.geometris.wqlib.WQSmartService"
    android:enabled="true" />
```

We recommend doing initialization and cleanup in the Application class:

public class App extends Application { AppModel mModel;

```java
@Override
public void onCreate()
{
    super.onCreate();
    Wqa.getInstance().initialize(this);
    
}

@Override
public void onTerminate()
{
    super.onTerminate();
    WherequbeService.getInstance().destroy(this);

}
```

The WherequbeService also needs to be initialized, chronologically after the Wqa object initialization.
Initialization of the WherequbeService requires a context as an argument.
An "onCreate" for an activity, such as an application's main activity,
is therefore a perfect place to do this:

 ```java
@Override protected void onCreate(Bundle savedInstanceState) {
 super.onCreate(savedInstanceState);

 // Init Device
 WherequbeService.getInstance().initialize(this);

 if(WherequbeService.getInstance().isConnected())
 {
     // maybe go off to another activity ...
 }
 ```


## Scanning for Available Wherequbes.

Before scanning, best practice is to check for an existing connection, and go to the 
appropriate activity if so, eg:

```java
if(WherequbeService.getInstance().isConnected())
    {
        Intent intent = new Intent(this, OBDActivity.class);
        startActivity(intent);
        return;
    }
```

Derive your own class from AbstractWherequbeStateObserver to 
receive callbacks for common events such as device connected, disconnected, discovered, etc. 
This can also be used to kick off an activity to transfer data from the Whereqube after connection.

For example:

```java
class MyObserver extends AbstractWherequbeStateObserver
{

    public  void onConnected()
    {
        runOnUiThread(new Runnable() {
            public void run() {
                String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                

                AppModel.getInstance().mConnectTime = System.currentTimeMillis();

                Intent intent = new Intent(MainActivity.this, OBDActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onSynced() {

    }

    @Override
    public void onDiscovered() {

    }

    public  void onDisconnected()
    {
        runOnUiThread(new Runnable() {
            public void run() {
                String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                
                Log.e(TAG, "Unexpected DISCONNECT event");

            }
        });
    }

    public  void onError(WQError ec)
    {
        Log.w(TAG, "Error:"+ec.mCode);
    }

}
```

Then create an instance of this class and register its hosting activity:

```java
mWherequbeObserver.register(this);
```


In order to initiate a scan, you will need an instance of the bluetooth manager (API level 18 and above):

```java
final BluetoothManager bluetoothManager =
            (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = bluetoothManager.getAdapter();
    // Checks if Bluetooth is supported on the device.
    if (mBluetoothAdapter == null) {
        Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        finish();
        return;
    }
```    
    
Set up a scan result listener:

```java
WQScanner.ScanResultListener results = new WQScanner.ScanResultListener()
{
    @Override
    public void onScanCompleted(final List<Whereqube> wqubes) {
        Log.i(TAG, "Whereqube scanned " + wqubes.size());
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                for (Whereqube wqube: wqubes) {
                    addDevice(wqube); // use your own function here; puts the data where you can use it
                }
            }
        });
    }
};
```

Then during your activity creation, initialize the scanner and set up your events to kick off the scan process:

```java
mScanButton = (ImageButton) findViewById(R.id.buttonScan);
    mScanButton.setOnClickListener(mScanButtonListener);
    mScanner = new WQScanner(results);
```
    

### Starting the Scan

Supposing your scan starts in a button click event, the following code illustrates how to begin the scan:

```java
private void scanLeDevice(final boolean enable) {
    if (enable) {
        // Stops scanning after a predefined scan period.

        clearScanResults();
        mScanning = true;
        mScanner.start(SCAN_PERIOD);
        setProgressBarIndeterminateVisibility(true);
        mScanButton.setEnabled(false);
    }
    else {
        // Cancel the scan timeout callback if still active or else it may fire later.
        mScanning = false;
        mScanner.stop();
        setProgressBarIndeterminateVisibility(false);
        mScanButton.setEnabled(true);
    }
}

View.OnClickListener mScanButtonListener = new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        if (!AppModel.getInstance().mBtAdapter.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else {
            if(mScanning == false)
                scanLeDevice(true);
        }
    }
};
```


## Data Transfer

Use of the WherequbeService singleton object provides access to data transfer between client and device.
It allows registration of call backs when receiving different types of data, eg:

```java
WherequbeService.getInstance().setReqHandler(BaseRequest.OBD_MEASUREMENT, myEventHandler);
```

The event handle will receive the data, which may be converted to an easily accessible data object:

```java
static RequestHandler myEventHandler = new RequestHandler() {
    @Override
    public void onRecv(@NonNull Context context, @NonNull BaseRequest request) {
        mData = request;
        if(mData != null && mData.requestId == BaseRequest.OBD_MEASUREMENT) {
            GeoData geoData = (GeoData) mData.getObject();
            if(geoData!= null)
                // do stuff ....
        }
        
        ..
    }
};
```

Request types are defined in the library RequestType class. 


Sending requests to the Whereqube to query for information is done with sendRequest() using 
predefined request classes in the library, some examples:

```java
WherequbeService.getInstance().sendRequest(new GetDeviceAddress(), addressResponseHandler, ADDRESS_TIMEOUT);

WherequbeService.getInstance().sendRequest(new UnidentifiedDriverMessageStartReq(), udrvEventResponseHandler, UDRV_EVENT_TIMEOUT);

WherequbeService.getInstance().sendRequest(new UnidentifiedDriverMessageStopReq(), udrvEventStopResponseHandler, UDRV_EVENT_TIMEO
```
