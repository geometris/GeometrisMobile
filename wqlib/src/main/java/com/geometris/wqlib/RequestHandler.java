package com.geometris.wqlib;

import android.content.Context;
import androidx.annotation.NonNull;
/**
 * Defines a generic message handler
 */
public interface RequestHandler {
    void onRecv(@NonNull Context var1, @NonNull BaseRequest var2);
}
