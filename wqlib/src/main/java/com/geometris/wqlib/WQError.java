package com.geometris.wqlib;

import android.text.TextUtils;

/**
 * Represents an error received from a Whereqube device.
 */
public class WQError {
    private static final String TAG = "Geometris";
    public static final String KEY_RESULT = "error";
    public static final String KEY_CODE = "errorCode";
    public static final String KEY_CAUSE = "errorCause";
    public int mCode;
    public String mCause;

    public static final int RESULT_OK = 0;
    public static final int ERROR_FAIL = 1;
    public static final int ERROR_INVALID_PARAMS = 2;
    public static final int ERROR_INVALID_STATE = 3;
    public static final int ERROR_UNAUTHORIZED = 5;
    public static final int ERROR_FILE_IO = 6;
    public static final int ERROR_FILE_OOS = 7;
    public static final int ERROR_TIME_OUT = 8;
    public static final int ERROR_UNAVAILABLE = -8;

    public WQError() {
        this.mCode = 1;
        this.mCause = null;
    }

    public WQError(int code, String cause) {
        this.mCode = code;
        this.mCause = cause;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Result: ");
        sb.append(this.mCode);
        if(!TextUtils.isEmpty(this.mCause)) {
            sb.append("Cause: ");
            sb.append(this.mCause);
        }

        return sb.toString();
    }
}
