package com.bx.pilot.controller;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.multidex.MultiDex;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;
//import com.bx.pilot.greendao.DaoMaster;
//import com.bx.pilot.greendao.DaoSession;

import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.BluetoothProductConnector;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Main application
 */
public class BXApplication extends Application {

    public static final String TAG = com.bx.pilot.controller.BXApplication.class.getName();

    private static BaseProduct product;
    private static BluetoothProductConnector bluetoothConnector = null;
    private static Bus bus = new Bus(ThreadEnforcer.ANY);
    private static Application app = null;
//    private static DaoSession mDaoSession;

    /**
     * Gets instance of the specific product connected after the
     * API KEY is successfully validated. Please make sure the
     * API_KEY has been added in the Manifest
     */
    public static synchronized BaseProduct getProductInstance() {
        product = DJISDKManager.getInstance().getProduct();
        return product;
    }

    public static synchronized BluetoothProductConnector getBluetoothProductConnector() {
        bluetoothConnector = DJISDKManager.getInstance().getBluetoothProductConnector();
        return bluetoothConnector;
    }

    public static boolean isAircraftConnected() {
        return getProductInstance() != null && getProductInstance() instanceof Aircraft;
    }

    public static boolean isHandHeldConnected() {
        return getProductInstance() != null && getProductInstance() instanceof HandHeld;
    }

    public static synchronized Aircraft getAircraftInstance() {
        if (!isAircraftConnected()) {
            return null;
        }
        return (Aircraft) getProductInstance();
    }

    public static synchronized HandHeld getHandHeldInstance() {
        if (!isHandHeldConnected()) {
            return null;
        }
        return (HandHeld) getProductInstance();
    }

    public static Application getInstance() {
        return com.bx.pilot.controller.BXApplication.app;
    }

    public static Bus getEventBus() {
        return bus;
    }

//    private void initGreenDao() {
//        //创建数据库mydb.db
//        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(app,"mydb.db");
//        //获取可写数据库
//        SQLiteDatabase database = helper.getWritableDatabase();
//        //获取数据库对象
//        DaoMaster daoMaster = new DaoMaster(database);
//        //获取Dao对象管理者
//        mDaoSession = daoMaster.newSession();
//    }

//    public static DaoSession getmDaoSession(){
//        return mDaoSession;
//    }

    @Override
    protected void attachBaseContext(Context paramContext) {
        super.attachBaseContext(paramContext);
        MultiDex.install(this);
        com.secneo.sdk.Helper.install(this);
        app = this;

//        initGreenDao();
    }
}