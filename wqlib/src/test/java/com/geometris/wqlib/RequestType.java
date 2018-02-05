package com.geometris.wqlib;

/**
 * Represents a data request type to be sent to a Whereqube device.
 */
public class RequestType {

    /**
     * Type of the request, as described by the class constants.
     */
    public int requestId;

    /**
     * Default constructor
     */
    public RequestType()
    {

    }

    /**
     * To be used by derived classes to set the id for the request
     * @param requestId What type of request, as described by class
     *                  constants.
     */
    protected RequestType(int requestId){
        this.requestId = requestId;
    }

    /**
     * Get all OBD parameters.
     */
    public static final int OBD_MEASUREMENT = 2;

    /**
     * Provide app id.
     */
    public static final int WRITE_APP_IDENTIFIER = 3;

    /**
     * Start getting unidentified driving events.
     */
    public static final int REQUEST_START_UDEVENTS = 4;

    /**
     * Stop getting unidentified driving events.
     */
    public static final int REQUEST_STOP_UDEVENTS = 5;

    /**
     * Purge saved unidentified driving events.
     */
    public static final int PURGE_UDEVENTS = 6;

    /**
     * Get device address.
     */
    public static final int REQUEST_DEVICE_ADDRESS = 7;

    /**
     * Obsolete
     */
    public static final int MESSAGE_SERVICE_DISCOVERED = 8;
}
