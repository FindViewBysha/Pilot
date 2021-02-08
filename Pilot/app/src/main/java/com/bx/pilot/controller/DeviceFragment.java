package com.bx.pilot.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bx.pilot.R;

public class DeviceFragment extends Fragment implements View.OnClickListener {
    private RelativeLayout mLayoutEnterDevice;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_device, container, false);
        mLayoutEnterDevice = view.findViewById(R.id.rl_enter_device);
        mLayoutEnterDevice.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.rl_enter_device:
                Intent intent = new Intent(getActivity(), FlightControlActivity.class);
                startActivity(intent);
                break;

            default:
                break;
        }
    }
}
