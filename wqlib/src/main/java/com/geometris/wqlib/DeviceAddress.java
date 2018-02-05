package com.geometris.wqlib;

import java.io.Serializable;

/**
 * Represents the address of a Whereqube via Bluetooth connection.
 */
public class DeviceAddress implements Serializable {

    /**
     * Address detail
     */
    public String address;

    DeviceAddress(String address){
        this.address = address;
    }
}
