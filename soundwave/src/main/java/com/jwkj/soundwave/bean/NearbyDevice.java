package com.jwkj.soundwave.bean;


import com.jwkj.soundwave.utils.ByteOptionUtils;


public class NearbyDevice {
    private int cmd;
    private int errCode;
    private int msgVersion;
    private int currVersion;
    private int deviceId;
    private int deviceType;
    private int deviceSubType;
    private int pwdFlag;
    private String ip;

    public static NearbyDevice getDeviceInfoByByteArray(byte[] data) {
        NearbyDevice device = new NearbyDevice();
        int msgVersion = ByteOptionUtils.getInt(data,12);
        int curVersion = (msgVersion >> 4) & 0x1;
        device.setDeviceType(ByteOptionUtils.getInt(data,20));
        device.setDeviceSubType(ByteOptionUtils.getInt(data,80));
        device.setDeviceId(ByteOptionUtils.getInt(data,16));
        device.setCmd(ByteOptionUtils.getInt(data,0));
        device.setPwdFlag(ByteOptionUtils.getInt(data,24));
        device.setCurrVersion(curVersion);
        device.setMsgVersion(msgVersion);
        return device;
    }

    public int getMsgVersion() {
        return msgVersion;
    }

    public void setMsgVersion(int msgVersion) {
        this.msgVersion = msgVersion;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public int getCurrVersion() {
        return currVersion;
    }

    public void setCurrVersion(int currVersion) {
        this.currVersion = currVersion;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public int getDeviceSubType() {
        return deviceSubType;
    }

    public void setDeviceSubType(int deviceSubType) {
        this.deviceSubType = deviceSubType;
    }

    public int getPwdFlag() {
        return pwdFlag;
    }

    public void setPwdFlag(int pwdFlag) {
        this.pwdFlag = pwdFlag;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public String toString() {
        return "NearbyDevice{" +
                "cmd=" + cmd +
                ", currVersion=" + currVersion +
                ", deviceId=" + deviceId +
                ", deviceType=" + deviceType +
                ", deviceSubType=" + deviceSubType +
                ", pwdFlag=" + pwdFlag +
                ", ip='" + ip + '\'' +
                '}';
    }
}
