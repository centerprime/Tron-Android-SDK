package com.centerprime.tronclientsdk;

import android.app.Application;
import android.content.Context;

/**
 * Created by CenterPrime on 2020/12/13.
 */
public class App extends Application     {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return App.mContext;
    }

}
