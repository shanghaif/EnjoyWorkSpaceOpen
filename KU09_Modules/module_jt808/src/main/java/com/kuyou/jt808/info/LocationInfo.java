package com.kuyou.jt808.info;

import android.location.Location;
import android.util.Log;

import com.cuichen.jt808_sdk.BuildConfig;
import com.kuyou.jt808.alarm.ALARM;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import kuyou.common.ku09.bytes.BitOperator;
import kuyou.common.ku09.bytes.ByteUtils;
import kuyou.sdk.jt808.base.jt808coding.JTT808Coding;

/**
 * action :
 * <p>
 * author: wuguoxian <br/>
 * date: 20-10-24 <br/>
 * <p>
 */
public class LocationInfo extends MsgInfo {

    private final int LOCATION_BATCH_SIZE = 3;

    public static interface CONFIG {
        public static final String FAKE_PROVIDER = "fake";
    }

    private final int[] mAlarmFlags = {
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0};

    private Location mLocation;
    private List<byte[]> mBatchLocationByteList = new ArrayList<>();
    private boolean isLocationFake = false;

    private boolean isAutoAddSosFlag = false;
    private int mSosFlag = -1;

    @Override
    public boolean parse(byte[] bytes) {
        return super.parse(bytes);
    }

    public String getTime(String pattern) {
        if (null == pattern)
            pattern = "yyyyMMddHHmmss";
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getTimeZone("GMT+08"));
        Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public double getLatitude() {
        return getLocation().getLatitude();
    }

    public double getLongitude() {
        return getLocation().getLongitude();
    }

    public double getAltitude() {
        return getLocation().getAltitude();
    }

    public double getSpeed() {
        return getLocation().getSpeed();
    }

    public double getBearing() {
        return getLocation().getBearing();
    }

    public double getAccuracy() {
        return getLocation().getAccuracy();
    }

    private Location getLocation() {
        return mLocation;
    }

    public LocationInfo setLocation(Location location) {
        mLocation = location;
        return LocationInfo.this;
    }

    public byte[] reportLocation() {
        return reportLocation(false);
    }

    public byte[] reportLocation(boolean isBatch) {
        if (null == getLocation()) {
            Log.e(TAG, "reportLocation > process fail : Location is null");
            return null;
        }
        Log.d(TAG, "reportLocation > ");

        byte[] alarm = getLocation808ItemAlarmFlag();
        byte[] state = getLocation808ItemState(getLatitude(), getLongitude());

        //DWORD经纬度
        double pow106 = Math.pow(10, 6);
        double lat106 = getLatitude() * pow106;
        double lng106 = getLongitude() * pow106;
        byte[] latb = ByteUtils.longToDword(Math.round(lat106));
        byte[] lngb = ByteUtils.longToDword(Math.round(lng106));

        // WORD 高度 速度 方向
        byte[] gaoChen = BitOperator.numToByteArray((int) getAltitude(), 2);
        byte[] speedb = BitOperator.numToByteArray((int) (getSpeed() * 3.6), 2);
        byte[] orientation = BitOperator.numToByteArray((int) getBearing(), 2);

        //bcd时间
        byte[] bcdTime = ByteUtils.str2Bcd(getTime("yyMMddHHmmss"));

        //位置信息附加项
        byte[] additionLocation = getLocation808ItemAddition(getConfig().getOrderId(), (int) getAltitude(), String.valueOf(getAccuracy()));

        if (BuildConfig.DEBUG) {
            Log.d(TAG, toString());
        } else {
            Log.i(TAG, toStringMini());
        }
        byte[] body = ByteUtils.byteMergerAll(alarm, state, latb, lngb, gaoChen, speedb, orientation, bcdTime, additionLocation);

        resetAlarmFlags();
        //非批量
        if (!isBatch) {
            return JTT808Coding.generate808(0x0200, getConfig(), body);
        }

        if (LOCATION_BATCH_SIZE > mBatchLocationByteList.size()) {
            mBatchLocationByteList.add(body);
            return null;
        }
        byte[] counts = ByteUtils.int2Word(LOCATION_BATCH_SIZE); //数据项个数
        byte[] batchType = {0}; //位置数据类型 0：正常位置批量汇报，1：盲区补报

        List<byte[]> formatLocations = new ArrayList<>();
        for (int i = 0; i < mBatchLocationByteList.size(); i++) {
            byte[] cLocation = mBatchLocationByteList.get(i);
            byte[] clocationLength = ByteUtils.int2Word(cLocation.length); //位置汇报数据体长度
            formatLocations.add(ByteUtils.byteMergerAll(clocationLength, cLocation));
        }

        byte[] batchLocations = ByteUtils.byteMergerAll(formatLocations); //位置汇报数据体
        mBatchLocationByteList.clear();
        return JTT808Coding.generate808(0x0704, getConfig(), ByteUtils.byteMergerAll(counts, batchType, batchLocations));
    }

    /**
     * 位置信息状态项
     *
     * @param
     * @return
     */
    private static byte[] getLocation808ItemState(double lat, double lng) {
        String state = "00";
        state = state + (lat < 0 ? "1" : "0");
        state = state + (lng < 0 ? "1" : "0");
        state = state + "0000000000000000000000000000";
        byte[] stateByte = ByteUtils.int2DWord(Integer.parseInt(state, 2));
        return stateByte;
    }


    /**
     * 位置信息附加项
     *
     * @param order    订单号open
     * @param altitude 高度
     * @param accuracy 精度
     * @return
     */
    private byte[] getLocation808ItemAddition(String order, int altitude, String accuracy) {
        byte[] orderType = new byte[2]; //订单号
        orderType[0] = (byte) (0xE1);
        orderType[1] = ByteUtils.int2Byte(order.getBytes().length);
        byte[] orderMsg = ByteUtils.byteMergerAll(orderType, order.getBytes());

        byte[] altitudeType = new byte[3]; //高度的正负
        altitudeType[0] = (byte) (0xE2);
        altitudeType[1] = (byte) (0x01);
        altitudeType[2] = (byte) (altitude < 0 ? 0x31 : 0x30);

        byte[] accuracyType = new byte[2]; //精度
        accuracyType[0] = (byte) (0xE3);
        accuracyType[1] = ByteUtils.int2Byte(accuracy.getBytes().length);
        byte[] accuracyMsg = ByteUtils.byteMergerAll(accuracyType, accuracy.getBytes());

        return ByteUtils.byteMergerAll(orderMsg, altitudeType, accuracyMsg);
    }

    private void resetAlarmFlags() {
        for (int i = 0, count = mAlarmFlags.length; i < count; i++) {
            mAlarmFlags[i] = 0;
        }
        if (isAutoAddSosFlag()) {
            setAlarmFlag(mSosFlag);
        }
    }

    /**
     * action:获取设定报警标识位的信息
     */
    public void setAlarmFlag(int index) {
        mAlarmFlags[index] = 1;
    }

    public byte[] getLocation808ItemAlarmFlag() {
        return ByteUtils.bits2DWord(mAlarmFlags);
    }

    private String getAlarmInfo() {
        String def = "报警列表： ";
        StringBuilder info = new StringBuilder(def);
        for(int index = 0,count = mAlarmFlags.length;index<count;index++){
            if (0 == mAlarmFlags[index])
                continue;
            switch (index) {
                case ALARM.FLAG_CAP_OFF:
                    info.append("脱帽 / ");
                    break;
                case ALARM.FLAG_SOS:
                    info.append("SOS / ");
                    break;
                case ALARM.FLAG_NEAR_POWER:
                    info.append("近电 / ");
                    break;
                case ALARM.FLAG_ENTRY_AND_EXIT:
                    info.append("进出 / ");
                    break;
                case ALARM.FLAG_FALL:
                    info.append("跌倒 / ");
                    break;
                case ALARM.FLAG_GAS:
                    info.append("气体 / ");
                    break;
                case ALARM.FLAG_GAS_METHANE:
                    info.append("甲烷气体 / ");
                    break;
                case ALARM.FLAG_GAS_SULFUR_HEXAFLUORIDE:
                    info.append("六氟化硫气体 / ");
                    break;
                case ALARM.FLAG_GAS_CARBON_MONOXIDE:
                    info.append("一氧化碳气体 / ");
                    break;
                default:
                    break;
            }
        }
        String infoResult = info.toString();
        return infoResult.equals(def) ? "" : infoResult;
    }

    public boolean isFakeLocation() {
        return null == getLocation() || getLocation().getProvider().equals(CONFIG.FAKE_PROVIDER);
    }

    public boolean isAutoAddSosFlag() {
        return isAutoAddSosFlag;
    }

    public void setAutoAddSosFlag(boolean val, int flag) {
        isAutoAddSosFlag = val;
        mSosFlag = flag;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(isFakeLocation() ? "模拟位置" : "真实位置 ");
        sb.append("\ndevId = ").append(getConfig().getDevId());
        String alarmInfo = getAlarmInfo();
        if (alarmInfo.length()>0)
            sb.append("\n").append(alarmInfo);
        sb.append("\ngetLongitude = ").append(getLongitude());
        sb.append("\ngetLatitude = ").append(getLatitude());
        //sb.append("\ngetTime = ").append(getTime(null));
        //sb.append("\ngetAltitude = ").append(getAltitude());
        //sb.append("\ngetSpeed = ").append(getSpeed());
        sb.append("\ngetLocation808ItemAlarmFlag = ").append(ByteUtils.bytes2Heb(getLocation808ItemAlarmFlag()));
        //sb.append("\ngetLocation808ItemState = ").append(getLocation808ItemState(getLatitude(), getLongitude()));
        //sb.append("\ngetLocation808ItemAddition = ").append(getLocation808ItemAddition(getConfig().getOrderId(), (int) getAltitude(), String.valueOf(getAccuracy())));
        return sb.toString();
    }

    protected String toStringMini() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(isFakeLocation() ? "模拟位置" : "真实位置 ");
        sb.append("\ndevId = ").append(getConfig().getDevId());
        String alarmInfo = getAlarmInfo();
        if (alarmInfo.length()>0)
            sb.append("\n").append(alarmInfo);
        return sb.toString();
    }
}
