package com.geometris.wqlib;

/**
 * Specifies a request to purge unidentified driver messages
 */
public class UnidentifiedDriverMessagePurgeReq extends BaseRequest {
    public UnidentifiedDriverMessagePurgeReq()
    {
        super(RequestType.PURGE_UDEVENTS);
    }
}
