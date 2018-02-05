package com.geometris.wqlib;

/**
 * Represents a request to get a Whereqube device address.
 */
public class GetDeviceAddress extends BaseRequest{
    public GetDeviceAddress(){
        super(BaseRequest.REQUEST_DEVICE_ADDRESS);
    }
}