package com.geometris.wqlib;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Message handler thread logic for the WherequbeService;
 * to be used only by WherequbeService class
 */
public class MsgHandlerThread extends HandlerThread {

    public static final String TAG = "GeometrisManager";
    Handler mHandler = null;
    WherequbeService mWS;
    final Context mContext;

    AtomicBoolean mBusy = new AtomicBoolean();
    protected HashMap<Integer, RequestHandler> mInboundRequests = new HashMap();
    protected HashMap<Integer, ResponseHandler> mPendingOutboundRequests = new HashMap();
    protected static ArrayList<UUID> sDIS_CHARS = new ArrayList();
    int dis_pos = 0;
    static int sHANDLES;
    ArrayList<MsgHandlerThread.ReqTuple> mXmitQueue = new ArrayList();
    ArrayList<MsgHandlerThread.ReqTuple> mPendingXmitQueue = new ArrayList();


    /**
     * Constructor
     * @param context   The context in which the thread manager runs
     * @param ws        The hosting WherequbeService
     */
    public MsgHandlerThread(Context context, WherequbeService ws) {
        super("MsgHandlerThread");
        sHANDLES = 0;
        this.mWS = ws;
        this.mContext = context;

        this.mBusy.set(false);
        this.start();

        // message handling callback
        this.mHandler = new Handler(this.getLooper()) {
            public void handleMessage(Message msg) {
                if(!MsgHandlerThread.this.mWS.isConnected()){
                    Log.d(TAG, "MH: Ignore msg, Gatt disconnected");
                }
                else {
                    MsgHandlerThread.ReqTuple rxReqTuple;
                    boolean status;
                    Message mt;

                    // process by message type

                    switch (msg.what) {
                        case OBD_NOTIFICATION:
                            BaseRequest request = (BaseRequest) msg.obj;
                            RequestHandler rh = (RequestHandler) MsgHandlerThread.this.mInboundRequests.get(request.requestId);
                            if (rh != null) {
                                rh.onRecv(MsgHandlerThread.this.mContext, request);
                            }
                            break;
                        case APP_IDENTIFIER:
                            request = (BaseRequest) msg.obj;
                            if (request.messageId == WQSmartService.MESSAGE_WRITE_COMPLETE)
                                mWS.setTXNotification(true);
                            break;
                        case WQREQUEST:
                            status = false;
                            rxReqTuple = (MsgHandlerThread.ReqTuple) msg.obj;
                            mBusy.set(true);
                            switch(rxReqTuple.mReq.requestId)
                            {
                                case BaseRequest.REQUEST_DEVICE_ADDRESS:
                                    status = mWS.readDeviceAddress();
                                    break;
                                case BaseRequest.PURGE_UDEVENTS:
                                    status = mWS.purgeUnidentifiedDriverMessages();
                                    break;
                                case BaseRequest.REQUEST_START_UDEVENTS:
                                    status = mWS.startTransmittingUnidentifiedDriverMessages();
                                    break;
                                case BaseRequest.REQUEST_STOP_UDEVENTS:
                                    status  = mWS.stopTransmittingUnidentifiedDriverMessages();
                                    break;

                            }
                            mBusy.set(false);
                            Log.d(TAG, "MH: +++ " + rxReqTuple.mReq.requestId + " xmitted.");
                            rxReqTuple.mTxTS = System.currentTimeMillis();

                            if(status) {
                                if (rxReqTuple.mTimeout > 0) {
                                    mt = MsgHandlerThread.this.mHandler.obtainMessage(WQREQUESTTIMEOUT, rxReqTuple);
                                    MsgHandlerThread.this.mHandler.sendMessageDelayed(mt, (long) rxReqTuple.mTimeout);
                                }
                            }
                            else {
                                if (rxReqTuple.mResponseHandler != null) {
                                    rxReqTuple.mResponseHandler.onError(mContext);
                                }
                            }
                            Log.d("PT", "MH: after sending" + rxReqTuple.mReq.requestId  + ", Q =" + MsgHandlerThread.this.mPendingXmitQueue.size());
                            if (MsgHandlerThread.this.mPendingXmitQueue.size() > 0) {
                                MsgHandlerThread.this._sendNext((MsgHandlerThread.ReqTuple) MsgHandlerThread.this.mPendingXmitQueue.remove(0));
                            }


                            break;
                        case WQRESPONSE:
                            BaseResponse bs = (BaseResponse)msg.obj;
                            MsgHandlerThread.ReqTuple rxReqTuplex = null;
                            Iterator var10 = MsgHandlerThread.this.mXmitQueue.iterator();

                            while(var10.hasNext()) {
                                MsgHandlerThread.ReqTuple rt = (MsgHandlerThread.ReqTuple)var10.next();
                                if(rt.mReq.requestId== bs.requestId) {
                                    rxReqTuplex = rt;
                                    break;
                                }
                            }

                            if(rxReqTuplex == null) {
                                Log.w(TAG, "MH: -?- Rx stale status:" + bs.requestId);
                            } else {
                                Log.v(TAG, "MH: --- Rx status:" + bs.requestId + ", " + (System.currentTimeMillis() - rxReqTuplex.mTxTS) + "ms");
                                MsgHandlerThread.this.mXmitQueue.remove(rxReqTuplex);
                                Log.d(TAG, "MH: Remove RT w/handle " + rxReqTuplex.handle);
                                MsgHandlerThread.this.mHandler.removeMessages(WQREQUESTTIMEOUT, rxReqTuplex);
                                if(rxReqTuplex.mResponseHandler != null) {
                                    rxReqTuplex.mResponseHandler.onRecv(MsgHandlerThread.this.mContext, bs);
                                }
                            }
                            break;
                        case WQREQUESTFAILED:
                            bs = (BaseResponse)msg.obj;
                            rxReqTuplex = null;
                            var10 = MsgHandlerThread.this.mXmitQueue.iterator();

                            while(var10.hasNext()) {
                                MsgHandlerThread.ReqTuple rt = (MsgHandlerThread.ReqTuple)var10.next();
                                if(rt.mReq.requestId== bs.requestId) {
                                    rxReqTuplex = rt;
                                    break;
                                }
                            }
                            if(rxReqTuplex == null) {
                                Log.w(TAG, "MH: -?- Rx stale status:" + bs.requestId);
                            } else {
                                Log.v(TAG, "MH: --- Rx status:" + bs.requestId + ", " + (System.currentTimeMillis() - rxReqTuplex.mTxTS) + "ms");
                                MsgHandlerThread.this.mXmitQueue.remove(rxReqTuplex);
                                Log.d(TAG, "MH: Remove RT w/handle " + rxReqTuplex.handle);
                                MsgHandlerThread.this.mHandler.removeMessages(WQREQUESTTIMEOUT, rxReqTuplex);
                                if(rxReqTuplex.mResponseHandler != null) {
                                    rxReqTuplex.mResponseHandler.onError(mContext);
                                }
                            }
                            break;
                        case WQREQUESTTIMEOUT:
                            rxReqTuple = (MsgHandlerThread.ReqTuple)msg.obj;
                            Log.w(TAG, "MH: ??? Timeout for :" + rxReqTuple.handle + "," + rxReqTuple.mReq.requestId + ", " + (System.currentTimeMillis() - rxReqTuple.mTxTS) + "ms");
                            MsgHandlerThread.this.mXmitQueue.remove(rxReqTuple);
                            if(rxReqTuple.mResponseHandler != null) {
                                rxReqTuple.mResponseHandler.onError(MsgHandlerThread.this.mContext);
                            }
                            break;
                        case SERVICEDISCOVERED:

                            if (mWS.hasSupportVersionTwo())
                                mWS.sendAppIdentification();
                            else
                                mWS.setTXNotification(true);
                            break;
                    }
                }
            }
        };
    }

    protected  void cancelAllRequests(){
        this.mXmitQueue.clear();
        this.mPendingXmitQueue.clear();
        this.mHandler.removeMessages(WQREQUESTTIMEOUT);
    }
    protected void serviceDiscovered()
    {
        this.mHandler.sendEmptyMessage(SERVICEDISCOVERED);
    }

    protected void onMessage(BaseRequest bs){

        Message msg = this.mHandler.obtainMessage(MsgHandlerThread.OBD_NOTIFICATION,bs);
        this.mHandler.sendMessage(msg);
    }

    protected void onResponse(BaseResponse bs){
        Message msg;
        if(bs.messageId == WQSmartService.MESSAGE_REQUEST_FAILED)
        {
            msg = this.mHandler.obtainMessage(MsgHandlerThread.WQREQUESTFAILED,bs);
        }
        else {
            msg = this.mHandler.obtainMessage(MsgHandlerThread.WQRESPONSE, bs);

        }
        this.mHandler.sendMessage(msg);
    }

    protected void onWriteAppIdentifier(BaseRequest bs){

        Message msg = this.mHandler.obtainMessage(MsgHandlerThread.APP_IDENTIFIER,bs);
        this.mHandler.sendMessage(msg);
    }


    private void _sendNext(MsgHandlerThread.ReqTuple rt) {
        this._sendRequest(rt);
    }

    private void _sendRequest(MsgHandlerThread.ReqTuple rt) {
        if(this.mBusy.get()) {
            Log.e(TAG, "MH: Busy " + rt.mReq.requestId);
            this.mPendingXmitQueue.add(rt);
        } else {
            this.mXmitQueue.add(rt);
            Log.d("PT", "MH: sendRequest:RT w/handle " + rt.handle + ",from:" + Thread.currentThread().getName());
            Message m = this.mHandler.obtainMessage(WQREQUEST, rt);
            this.mHandler.sendMessage(m);
        }

    }
    protected void sendRequest(BaseRequest request, ResponseHandler sh, int timeout) {
        Log.d(TAG, "MH: Tx Request: " + request.requestId);
        MsgHandlerThread.ReqTuple rt = new MsgHandlerThread.ReqTuple(request, sh, timeout);
        this._sendRequest(rt);

    }
    protected void setReqHandler(int type, RequestHandler rh) {
        this.mInboundRequests.put(type, rh);
    }



    class ReqTuple {
       // public final String mStatus;
        public final BaseRequest mReq;
        public final ResponseHandler mResponseHandler;
        public final int mTimeout;
        public final Integer handle;
        public long mTxTS;
        ReqTuple(BaseRequest req, ResponseHandler responseHandler, int timeout) {
          //  this.mStatus = status;
            this.mReq = req;
            this.mResponseHandler = responseHandler;
            this.mTimeout = timeout;
            this.handle = Integer.valueOf(MsgHandlerThread.sHANDLES++);
        }

        public boolean equals(Object obj) {
            MsgHandlerThread.ReqTuple rt = (MsgHandlerThread.ReqTuple)obj;
            return this.handle == rt.handle;
        }
    }

    public static final int OBD_NOTIFICATION =1;
    public static final int APP_IDENTIFIER =2;
    public static final int WQREQUEST =3;
    public static final int WQREQUESTFAILED =4;
    public static final int WQREQUESTTIMEOUT =5;
    public static final int WQRESPONSE =6;
    public static final int SERVICEDISCOVERED = 7;
}



