package com.geometris.wqlib;


import android.bluetooth.BluetoothDevice;

/**
 * Manages a connection to a Whereqube device.
 */
public class Whereqube implements Comparable<Whereqube>{

    /**
     * Bluetooth session with device
     */
    public final BluetoothDevice mDevice;

    /**
     * Signal strength of device
     */
    public final int mRssi;

    /**
     * Constructor
     * @param device bluetooth connection to the device
     * @param rssi device signal strength
     */
    public Whereqube(BluetoothDevice device, int rssi) {
        this.mDevice = device;
        this.mRssi = rssi;
    }

    /**
     * Checks to see if a given bluetooth address matches this device
     * @param device device to check against
     * @return true if this device has the same address
     */
    public boolean matches(BluetoothDevice device) {
        return this.mDevice.getAddress().equals(device.getAddress());
    }

    /**
     * compares / sorts whereqube devices by signal strength
     * @param another other whereqube to compare to
     * @return comparison value
     */
    @Override
    public int compareTo(Whereqube another){
        return Integer.valueOf(another.mRssi).compareTo(mRssi);
    }
}
