package com.kuyou.ft.item;

import android.content.Context;

import com.kuyou.ft.R;
import com.kuyou.ft.basic.thermal.TestItemThermalCameraBasic;

public class TestItemThermalCamera extends TestItemThermalCameraBasic {

    @Override
    public int getTestPolicy() {
        int policy = 0;
        policy |= POLICY_TEST;
        //policy |= POLICY_TEST_AUTO;
        //policy |= POLICY_TEST_AGING;
        return policy;
    }

    @Override
    public int getTestId() {
        return R.id.test_tcamera;
    }

    @Override
    public String getTestTitle(Context context) {
        return context.getString(R.string.tcamera_test);
    }

}