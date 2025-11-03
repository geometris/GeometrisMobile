package com.geometris.wqlib;

import androidx.annotation.NonNull;


import org.joda.time.Duration;

/**
 * Generic data request object. Represents message type and contents.
 */
public class BaseRequest extends RequestType{

    /**
     * Identifies the type of the message
     */
    int messageId;

    /**
     * Message content
     */
    Object obj;

    /**
     * Default constructor
     */
    protected BaseRequest(){

    }

    /**
     * Constructor
     * @param requestId Type of request
     * @see RequestType
     */
    protected BaseRequest(int requestId){
        super(requestId);
    }

    /**
     * Constructor
     * @param requestId Type of request
     * @param messageId Type of message
     * @see RequestType
     */
    protected BaseRequest(int requestId, int messageId){
        super(requestId);
        this.messageId = messageId;
        this.obj= null;
    }

    /**
     * Constructor
     * @param requestId Type of request
     * @param messageId Type of message
     * @param obj       Message contents
     * @see RequestType
     */
    protected BaseRequest(int requestId, int messageId, Object obj)
    {
        super(requestId);
        this.messageId = messageId;
        this.obj= obj;
    }

    /**
     * Gets the message contents
     * @return message contents
     */
    public Object getObject(){
        return this.obj;
    }

    //private static final int REQUEST_OBD_MEASUREMENT = 2;

}
