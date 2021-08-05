package com.kuyou.jt808.location.filter.base;

import android.location.Location;

import com.kuyou.jt808.location.base.HMLocationProvider;

/**
 * action :
 * <p>
 * remarks:  <br/>
 * author: wuguoxian <br/>
 * date: 21-7-5 <br/>
 * </p>
 */
public interface IFilterCallBack  {
    /**
     * action:启用波动滤波器<br/>
     */
    public static final int POLICY_FILTER_FLUCTUATION = (1 << 0);
    /**
     * action:启用卡尔曼滤波器<br/>
     */
    public static final int POLICY_FILTER_KALMAN = (1 << 1);

    public void filter(Location location);

    public void setLocationChangeListener(HMLocationProvider.IOnLocationChangeListener listener);

    /**
     * action:需要启用的滤波器配置
     * <p>
     * author: wuguoxian <br/>
     * date: 20210706 <br/>
     * remark：<br/>
     * int policy = 0;<br/>
     * policy |= IFilterCallBack.POLICY_FILTER_FLUCTUATION;<br/>
     * policy |= IFilterCallBack.POLICY_FILTER_KALMAN;<br/>
     * return policy;
     *
     * @return policy :要素配置
     */
    public int getFilterPolicy();
}
