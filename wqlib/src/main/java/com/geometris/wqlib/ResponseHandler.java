package com.geometris.wqlib;

import android.content.Context;
import androidx.annotation.NonNull;
/**
 * Generic response handler interface
 */
public interface ResponseHandler {
    void onRecv(@NonNull Context var1, @NonNull BaseResponse var2);

    void onError(@NonNull Context var1);
}
