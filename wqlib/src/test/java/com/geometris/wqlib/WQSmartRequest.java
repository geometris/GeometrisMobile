package com.geometris.wqlib;

import android.os.Handler;

import java.util.UUID;

/**
 * Represents a request to a Whereqube device via BLE
 */
public class WQSmartRequest {
    public enum RequestType {
        CHARACTERISTIC_NOTIFICATION, READ_CHARACTERISTIC, READ_DESCRIPTOR, READ_RSSI, WRITE_CHARACTERISTIC, WRITE_DESCRIPTOR
    };

    public RequestType type;
    public UUID serviceUuid;
    public UUID characteristicUuid;
    public UUID descriptorUuid;
    public int requestId;
    public byte [] value;

    public WQSmartRequest(RequestType type, int requestId, UUID service, UUID characteristic, UUID descriptor) {
        this.type = type;
        this.requestId = requestId;
        this.serviceUuid = service;
        this.characteristicUuid = characteristic;
        this.descriptorUuid = descriptor;
        this.value = null;
    }

    public WQSmartRequest(RequestType type, int requestId, UUID service, UUID characteristic, UUID descriptor,
                          byte [] value) {
        this.type = type;
        this.requestId = requestId;
        this.serviceUuid = service;
        this.characteristicUuid = characteristic;
        this.descriptorUuid = descriptor;
        this.value = value;
    }
}
