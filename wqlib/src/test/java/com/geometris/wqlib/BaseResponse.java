package com.geometris.wqlib;

/**
 * Represents a response to a request from a Whereqube device.
 * Extends Request type class to encapsulate request details that
 * generated the response.
 */
public class BaseResponse extends RequestType{

    /**
     * Type of message
     */
    int messageId;

    /**
     * Response contents
     */
    Object obj;


    protected BaseResponse(){

    }

    protected BaseResponse(int requestId){
        super(requestId);
    }

    protected BaseResponse(int requestId, int messageId){
        super(requestId);
        this.messageId = messageId;
        this.obj= null;
    }

    protected BaseResponse(int requestId, int messageId, Object obj)
    {
        super(requestId);
        this.messageId = messageId;
        this.obj= obj;
    }

    /**
     * Gets the respons contents
     * @return response contents
     */
    public Object getObject(){
        return this.obj;
    }


}
