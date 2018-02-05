package com.geometris.wqlib;

import android.os.ParcelUuid;
import android.support.annotation.RequiresPermission;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanSettings.Builder;

/**
 * Scans bluetooth radio for available connections to Whereqube devices.
 */
public class WQScanner {
    public static final String TAG = "GeometrisManager";
    private WQScanner.ScanResultListener mListener;
    private List<Whereqube> mWherequbes = new ArrayList();

    private ScanCallback scanCallback = new ScanCallback() {
        public void onScanResult(int callbackType, ScanResult result) {
            Log.w(TAG, "onScanResult: Result" + result.getDevice().getAddress());
        }

        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults " + results.size());
            Iterator var2 = results.iterator();

            while(var2.hasNext()) {
                ScanResult result = (ScanResult)var2.next();
                Log.d(TAG, "onBatchScanResults: Result " + result.getDevice().getAddress());
                mWherequbes.add(new Whereqube(result.getDevice(), result.getRssi()));
            }

            if(mListener != null) {
                mListener.onScanCompleted(mWherequbes);
            }
            mWherequbes.clear();
        }

        public void onScanFailed(int errorCode) {
            Log.e(TAG, "onScanFailed: " + errorCode);
        }
    };

    /**
     * Constructor.
     * @param listener object to receive scan results, implementing ScanResultListener
     * */
    public WQScanner(WQScanner.ScanResultListener listener) {
        this.mListener = listener;
    }

    /**
     * Begins scanning for the given duration
     * @param scanDuration scan duration in milliseconds before timing out.
     */
    @RequiresPermission("android.permission.BLUETOOTH")
    public void start(long scanDuration) {
        Log.d(TAG, "Scan Started ...");
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        ScanSettings settings = (new Builder()).setScanMode(2).setReportDelay(scanDuration).setUseHardwareBatchingIfSupported(false).setUseHardwareFilteringIfSupported(false).build();
        List<ScanFilter> filters = new ArrayList();
        ParcelUuid mUuid = WQSmartService.WQSmartUuid.OBD_SERVICE.getParcelable();
        filters.add((new no.nordicsemi.android.support.v18.scanner.ScanFilter.Builder()).setServiceUuid(mUuid).build());
        scanner.startScan(filters, settings, this.scanCallback);
    }

    /**
     * Halts the scan.
     */
    public void stop() {
        BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
        scanner.stopScan(this.scanCallback);
        Log.d(TAG, "Scan stopped.");
    }

    /**
     * Defines an interface for receiving scan results.
     */
    public interface ScanResultListener {
        /**
         * Called back whenever the WQScanner finds devices to connect to.
         * @param devices a list of avaiable devices to connect to.
         */
        void onScanCompleted(List<Whereqube> devices);
    }
}
