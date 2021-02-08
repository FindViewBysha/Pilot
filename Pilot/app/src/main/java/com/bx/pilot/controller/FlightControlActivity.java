package com.bx.pilot.controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.MyLocationStyle;
import com.bx.pilot.R;
import com.bx.pilot.utils.CommonUtil;
import com.bx.pilot.utils.DensityUtil;
import com.bx.pilot.utils.WindowUtil;
import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;

import dji.common.airlink.PhysicalSource;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.camera.VideoFeeder;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.controls.CameraControlsWidget;

/**
 * Activity that shows all the UI elements together
 */
public class FlightControlActivity extends Activity implements View.OnClickListener {

    private static final String TAG = FlightControlActivity.class.getName();
    private MapWidget mapWidget;
    private AMap mAMap;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    private ImageButton mImgTopBack;
    private ImageButton mImgTopMore;

    private PopupWindow mPopupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_control);


        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        deviceHeight = outPoint.y;
        deviceWidth = outPoint.x;

        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });

                mAMap = (AMap) map.getMap();
                initMapSetting();

//                map.getUiSettings().setZoomControlsEnabled(false);
            }
        });
        mapWidget.onCreate(savedInstanceState);

        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        if (VideoFeeder.getInstance() != null) {
            //If secondary video feed is already initialized, get video source
            updateSecondaryVideoVisibility(VideoFeeder.getInstance().getSecondaryVideoFeed().getVideoSource() != PhysicalSource.UNKNOWN);
            //If secondary video feed is not yet initialized, wait for active status
            VideoFeeder.getInstance().getSecondaryVideoFeed()
                    .addVideoActiveStatusListener(isActive ->
                            runOnUiThread(() -> updateSecondaryVideoVisibility(isActive)));
        }

        mImgTopBack = findViewById(R.id.img_top_back);
        mImgTopBack.setOnClickListener(this);
        mImgTopMore = findViewById(R.id.img_top_more);
        mImgTopMore.setOnClickListener(this);
    }

    private void initMapSetting() {
//        mAMap.setOnMapTouchListener(this);
//        mAMap.setOnMarkerClickListener(this);
//        mAMap.setOnMapClickListener(this);
//        mAMap.setLocationSource(this);//通过mAMap对象设置定位数据源的监听

        mAMap.setMyLocationEnabled(true);// 可触发定位并显示当前位置
//        mAMap.moveCamera(CameraUpdateFactory.zoomBy(6));

        UiSettings uiSettings = mAMap.getUiSettings();// 实例化UiSettings类对象
        uiSettings.setRotateGesturesEnabled(false);
        uiSettings.setTiltGesturesEnabled(false);
        uiSettings.setZoomGesturesEnabled(true);
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setScrollGesturesEnabled(true);
        uiSettings.setCompassEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false); // 显示默认的定位按钮
        uiSettings.setScaleControlsEnabled(true);// 控制比例尺控件是否显示

        MyLocationStyle myLocationStyle = new MyLocationStyle();// 初始化定位蓝点样式类myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、且将视角移动到地图中心点，定位点依照设备方向旋转，并且会跟随设备移动。（1秒1次定位）如果不设置myLocationType，默认也会执行此种模式。
        myLocationStyle.interval(2000); // 设置连续定位模式下的定位间隔，只在连续定位模式下生效，单次定位模式下不会生效。单位为毫秒。
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));// 设置定位蓝点的icon图标方法，需要用到BitmapDescriptor类对象作为参数。
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
        myLocationStyle.strokeWidth(0);// 设置精度范围的圆形边框宽度
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        mAMap.setMyLocationStyle(myLocationStyle);// 设置定位蓝点的Style

        // 设置希望展示的地图缩放级别
        CameraUpdate cameraUpdate = CameraUpdateFactory.zoomTo(17.5f);
        mAMap.animateCamera(cameraUpdate, 1000, null);
        mAMap.setMaxZoomLevel(17.5f);
        CameraPosition cameraPosition = CameraPosition.builder().tilt(0).build();
        AMapOptions aMapOptions = new AMapOptions();
        aMapOptions.camera(cameraPosition);

        // 初始化定位
        AMapLocationClient locationClient = new AMapLocationClient(getApplicationContext());
        // 设置定位回调监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        // 可在其中解析amapLocation获取相应内容。
                        if (aMapLocation.getLongitude() != 0 && aMapLocation.getLatitude() != 0) {
//                            mLongtitude = aMapLocation.getLongitude();
//                            mLatitude = aMapLocation.getLatitude();
                        }
                    } else {
                        // 定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "location Error, ErrCode:"
                                + aMapLocation.getErrorCode() + ", errInfo:"
                                + aMapLocation.getErrorInfo());
                    }
                }
            }
        });

        // 初始化AMapLocationClientOption对象
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        // 设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        // 设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        locationOption.setInterval(1000);
        // 设置是否返回地址信息（默认返回地址信息）
        locationOption.setNeedAddress(true);
        // 给定位客户端对象设置定位参数
        locationClient.setLocationOption(locationOption);
        // 启动定位
        locationClient.startLocation();
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility(boolean isActive) {
        if (isActive) {
            secondaryVideoView.setVisibility(View.VISIBLE);
        } else {
            secondaryVideoView.setVisibility(View.GONE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        CameraControlsWidget controlsWidget = findViewById(R.id.CameraCapturePanel);
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;

        private int mToHeight;
        private int mFromHeight;
        private int mToWidth;

        private int mFromWidth;
        private int mMargin;
        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.img_top_back:
                super.onBackPressed();
                break;

            case R.id.img_top_more:
                getPopupWindow();
                mPopupWindow.showAtLocation(v, Gravity.RIGHT | Gravity.TOP, 0, 0);;
                break;
            default:
                break;
        }
    }

    protected void initPopuptWindow() {
//        WindowUtil.lucencyBackground(this);
        View popSetting = getLayoutInflater().inflate(R.layout.pop_setting, null);
        mPopupWindow = new PopupWindow(popSetting, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setAnimationStyle(R.style.AnimationFade);
        popSetting.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
//        mPopupWindow.setOutsideTouchable(true);
//        mPopupWindow.setFocusable(false);
        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
//                WindowUtil.cancelBackground(FlightControlActivity.this);
            }
        });

        Button close = popSetting.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopupWindow.dismiss();
            }
        });
    }

    private void getPopupWindow() {
        if (null != mPopupWindow) {
            mPopupWindow.dismiss();
            return;
        } else {
            initPopuptWindow();
        }
    }
}