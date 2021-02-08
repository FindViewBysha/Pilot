package com.bx.pilot.controller;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.squareup.otto.Subscribe;
import com.bx.pilot.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends FragmentActivity implements View.OnClickListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private static boolean isAppStarted = false;
    private Handler mHandler;
    private HandlerThread mHandlerThread = new HandlerThread("Activation");
    private BaseProduct mProduct;
    private TextView connectionStatusText;
    private AtomicBoolean hasAppActivationListenerStarted = new AtomicBoolean(false);
    private static final int MSG_UPDATE_BLUETOOTH_CONNECTOR = 0;
    private static final int MSG_INFORM_ACTIVATION = 1;
    private static final int ACTIVATION_DALAY_TIME = 3000;
    private AppActivationState.AppActivationStateListener appActivationStateListener;
    private BaseComponent.ComponentListener mDJIComponentListener = new BaseComponent.ComponentListener() {

        @Override
        public void onConnectivityChange(boolean isConnected) {
            Log.d(TAG, "onComponentConnectivityChanged: " + isConnected);
            notifyStatusChange();
        }
    };
    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
//                Toast.makeText(getApplicationContext(), "SDK registration succeeded!", Toast.LENGTH_LONG).show();
            } else {
//                Toast.makeText(getApplicationContext(),
//                        "SDK registration failed, check network and retry!",
//                        Toast.LENGTH_LONG).show();
            }
        }
        @Override
        public void onProductDisconnect() {
//            Toast.makeText(getApplicationContext(), "product disconnect!", Toast.LENGTH_LONG).show();
            notifyStatusChange();
        }

        @Override
        public void onProductConnect(BaseProduct product) {
//            Toast.makeText(getApplicationContext(), "product connect!", Toast.LENGTH_LONG).show();
            notifyStatusChange();
        }

        @Override
        public void onProductChanged(BaseProduct product) {
            notifyStatusChange();
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
            if (newComponent != null) {
                newComponent.setComponentListener(mDJIComponentListener);
            }

//            Toast.makeText(getApplicationContext(), key.toString() + " changed", Toast.LENGTH_LONG).show();
            notifyStatusChange();
        }

        @Override
        public void onInitProcess(DJISDKInitEvent event, int totalProcess) {

        }

        @Override
        public void onDatabaseDownloadProgress(long current, long total) {

        }
    };

    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE, // Gimbal rotation
            Manifest.permission.INTERNET, // API requests
            Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
            Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
            Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
            Manifest.permission.ACCESS_FINE_LOCATION, // Maps
            Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
            Manifest.permission.BLUETOOTH, // Bluetooth connected products
            Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
            Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
            Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
            Manifest.permission.RECORD_AUDIO // Speaker accessory
    };
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private List<String> missingPermission = new ArrayList<>();

    public static boolean isStarted() {
        return isAppStarted;
    }

    //声明四个Tab的布局文件
    private LinearLayout mTabDevice;
    private LinearLayout mTabAirline;
    private LinearLayout mTabRecord;
    private LinearLayout mTabSetting;

    //声明四个Tab的ImageButton
    private ImageButton mDeviceImg;
    private ImageButton mAirlineImg;
    private ImageButton mRecordImg;
    private ImageButton mSettingImg;

    //声明四个Tab的TextView
    private TextView mDeviceText;
    private TextView mAirlineText;
    private TextView mRecordText;
    private TextView mSettingText;

    //声明四个Tab分别对应的Fragment
    private Fragment mFragDevice;
    private Fragment mFragAirline;
    private Fragment mFragRecord;
    private Fragment mFragSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkAndRequestPermissions();
        BXApplication.getEventBus().register(this);
        setContentView(R.layout.activity_main);
        isAppStarted = true;

//        connectionStatusText = findViewById(R.id.text_connection_status);
//        findViewById(R.id.manual_flight_bg).setOnClickListener(this);
//        findViewById(R.id.mission_flight_bg).setOnClickListener(this);

        refreshSDKRelativeUI();

        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_INFORM_ACTIVATION:
                        loginToActivationIfNeeded();
                        break;
                }
            }
        };

        initViews();// 初始化控件
        initEvents();// 初始化事件
        selectTab(0);// 默认选中第一个Tab
    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DJISDKManager.getInstance().destroy();
        isAppStarted = false;
        removeAppActivationListenerIfNeeded();
        BXApplication.getEventBus().unregister(this);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
            Intent attachedIntent = new Intent();
            attachedIntent.setAction(DJISDKManager.USB_ACCESSORY_ATTACHED);
            sendBroadcast(attachedIntent);
        }
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
//            Toast.makeText(getApplicationContext(), "Missing permissions! Will not register SDK to connect to aircraft.", Toast.LENGTH_LONG).show();
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(MainActivity.this, registrationCallback);
                }
            });
        }
    }

    private void notifyStatusChange() {
        BXApplication.getEventBus().post(new ConnectivityChangeEvent());
    }

    @Subscribe
    public void onConnectivityChange(ConnectivityChangeEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshSDKRelativeUI();
            }
        });
    }

    public static class ConnectivityChangeEvent {

    }

    private void refreshSDKRelativeUI() {
        mProduct = BXApplication.getProductInstance();
        Log.d(TAG, "mProduct: " + (mProduct == null ? "null" : "unnull"));
        if (null != mProduct ) {
            if (mProduct.isConnected()) {
                if (mProduct instanceof Aircraft) {
                    addAppActivationListenerIfNeeded();
                }

                if (null != mProduct.getModel()) {
                    connectionStatusText.setText("" + mProduct.getModel().getDisplayName());
                } else {
//                    connectionStatusText.setText(R.string.connection_loose);
                }
            } else if (mProduct instanceof Aircraft){
                Aircraft aircraft = (Aircraft) mProduct;
                if (aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
//                    connectionStatusText.setText(R.string.connection_only_rc);
                }
            }
        } else {
//            connectionStatusText.setText(R.string.connection_loose);
        }
    }

    private void addAppActivationListenerIfNeeded() {
        if (AppActivationManager.getInstance().getAppActivationState() != AppActivationState.ACTIVATED) {
            sendDelayMsg(MSG_INFORM_ACTIVATION, ACTIVATION_DALAY_TIME);
            if (hasAppActivationListenerStarted.compareAndSet(false, true)) {
                appActivationStateListener = new AppActivationState.AppActivationStateListener() {

                    @Override
                    public void onUpdate(AppActivationState appActivationState) {
                        if (mHandler != null && mHandler.hasMessages(MSG_INFORM_ACTIVATION)) {
                            mHandler.removeMessages(MSG_INFORM_ACTIVATION);
                        }
                        if (appActivationState != AppActivationState.ACTIVATED) {
                            sendDelayMsg(MSG_INFORM_ACTIVATION, ACTIVATION_DALAY_TIME);
                        }
                    }
                };
                AppActivationManager.getInstance().addAppActivationStateListener(appActivationStateListener);
            }
        }
    }

    private void sendDelayMsg(int msg, long delayMillis) {
        if (mHandler == null){
            return;
        }

        if (!mHandler.hasMessages(msg)) {
            mHandler.sendEmptyMessageDelayed(msg, delayMillis);
        }
    }

    private void removeAppActivationListenerIfNeeded() {
        if (hasAppActivationListenerStarted.compareAndSet(true, false)) {
            AppActivationManager.getInstance().removeAppActivationStateListener(appActivationStateListener);
        }
    }

    private void loginToActivationIfNeeded() {
        if (AppActivationManager.getInstance().getAppActivationState() == AppActivationState.LOGIN_REQUIRED) {
            UserAccountManager.getInstance()
                    .logIntoDJIUserAccount(this,
                            new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                                @Override
                                public void onSuccess(UserAccountState userAccountState) {
//                                    ToastUtils.setResultToToast("Login Successed!");
                                }

                                @Override
                                public void onFailure(DJIError djiError) {
//                                    ToastUtils.setResultToToast("Login Failed!");
                                }
                            });
        }
    }

    private void initEvents() {
        // 初始化四个Tab的点击事件
        mTabDevice.setOnClickListener(this);
        mTabAirline.setOnClickListener(this);
        mTabRecord.setOnClickListener(this);
        mTabSetting.setOnClickListener(this);
    }

    private void initViews() {
        // 初始化四个Tab的布局文件
        mTabDevice = findViewById(R.id.id_tab_device);
        mTabAirline = findViewById(R.id.id_tab_airline);
        mTabRecord = findViewById(R.id.id_tab_record);
        mTabSetting = findViewById(R.id.id_tab_setting);

        // 初始化四个ImageButton
        mDeviceImg = findViewById(R.id.id_tab_device_img);
        mAirlineImg =  findViewById(R.id.id_tab_airline_img);
        mRecordImg =  findViewById(R.id.id_tab_record_img);
        mSettingImg = findViewById(R.id.id_tab_setting_img);

        // 初始化四个TextView
        mDeviceText = findViewById(R.id.tv_device);
        mAirlineText = findViewById(R.id.tv_airline);
        mRecordText = findViewById(R.id.tv_record);
        mSettingText = findViewById(R.id.tv_setting);
    }

    // 处理Tab的点击事件
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        resetImgsAndTexts();
        switch (v.getId()) {
            case R.id.id_tab_device:
                selectTab(0);
                break;
            case R.id.id_tab_airline:
                selectTab(1);
                break;
            case R.id.id_tab_record:
                selectTab(2);
                break;
            case R.id.id_tab_setting:
                selectTab(3);
                break;
        }

    }

    // 进行选中Tab的处理
    private void selectTab(int i) {
        // 获取FragmentManager对象
        FragmentManager manager = getSupportFragmentManager();
        // 获取FragmentTransaction对象
        FragmentTransaction transaction = manager.beginTransaction();
        // 先隐藏所有的Fragment
        hideFragments(transaction);
        switch (i) {
            case 0:
                mDeviceImg.setImageResource(R.drawable.tab_device_pressed);
                mDeviceText.setTextColor(0xff3a89ea);
                if (mFragDevice == null) {
                    mFragDevice = new DeviceFragment();
                    transaction.add(R.id.id_content, mFragDevice);
                } else {
                    transaction.show(mFragDevice);
                }
                break;
            case 1:
                mAirlineImg.setImageResource(R.drawable.tab_airline_pressed);
                mAirlineText.setTextColor(0xff3a89ea);
                if (mFragAirline == null) {
                    mFragAirline = new AirlineFragment();
                    transaction.add(R.id.id_content, mFragAirline);
                } else {
                    transaction.show(mFragAirline);
                }
                break;
            case 2:
                mRecordImg.setImageResource(R.drawable.tab_record_pressed);
                mRecordText.setTextColor(0xff3a89ea);
                if (mFragRecord == null) {
                    mFragRecord = new RecordFragment();
                    transaction.add(R.id.id_content, mFragRecord);
                } else {
                    transaction.show(mFragRecord);
                }
                break;
            case 3:
                mSettingImg.setImageResource(R.drawable.tab_setting_pressed);
                mSettingText.setTextColor(0xff3a89ea);
                if (mFragSetting == null) {
                    mFragSetting = new SettingFragment();
                    transaction.add(R.id.id_content, mFragSetting);
                } else {
                    transaction.show(mFragSetting);
                }
                break;
        }
        transaction.commit();
    }

    // 将四个的Fragment隐藏
    private void hideFragments(FragmentTransaction transaction) {
        if (mFragDevice != null) {
            transaction.hide(mFragDevice);
        }
        if (mFragAirline != null) {
            transaction.hide(mFragAirline);
        }
        if (mFragRecord != null) {
            transaction.hide(mFragRecord);
        }
        if (mFragSetting != null) {
            transaction.hide(mFragSetting);
        }
    }

    // 将四个ImageButton置为灰色
    private void resetImgsAndTexts() {
        mDeviceImg.setImageResource(R.drawable.tab_device_normal);
        mAirlineImg.setImageResource(R.drawable.tab_airline_normal);
        mRecordImg.setImageResource(R.drawable.tab_record_normal);
        mSettingImg.setImageResource(R.drawable.tab_setting_normal);

        mDeviceText.setTextColor(0xff9b9b9b);
        mAirlineText.setTextColor(0xff9b9b9b);
        mRecordText.setTextColor(0xff9b9b9b);
        mSettingText.setTextColor(0xff9b9b9b);
    }

}