package com.geometris.wqlib;

import android.content.Context;

/**
 * Created by bipin_2 on 1/24/2018.
 */

public class Wqa {
    public static final int API_LEVEL = 2;
    protected boolean mIsInitialized = false;
    private static Wqa instance = new Wqa();

    private Wqa() {
    }

    public int initialize(Context context) {
        this.mIsInitialized = true;
        return 0;
    }

    public static Wqa getInstance() {
        return instance;
    }

    protected boolean isInitialized() {
        return this.mIsInitialized;
    }
}
