package com.geometris.wqlib;

/**
 * Specifies a request to stop receiving unidentified driver messages
 */
public class UnidentifiedDriverMessageStopReq extends BaseRequest {
    public UnidentifiedDriverMessageStopReq()
    {
        super(RequestType.REQUEST_STOP_UDEVENTS);
    }
}
