/**
 * Android_NetSdk
 * SDK_CONFIG_NET_COMMON.java
 * Administrator
 * TODO
 * 2014-12-11
 */
package com.smartism.znzk.xiongmai.lib.sdk.struct;
import com.lib.SDKCONST;

/**
 * Android_NetSdk
 * SDK_CONFIG_NET_COMMON.java
 * @author huangwanshui
 * TODO
 * 2014-12-11
 */
public class SDK_CONFIG_NET_COMMON_V3 {
	
	//!������
	public byte st_00_HostName[] = new byte[SDKCONST.NET_NAME_PASSWORD_LEN];
	//!����IP
	public CONFIG_IPAddress st_01_HostIP = new CONFIG_IPAddress();
	//!��������
	public CONFIG_IPAddress st_02_Submask = new CONFIG_IPAddress();
	//!����IP
	public CONFIG_IPAddress st_03_Gateway = new CONFIG_IPAddress();
	//!HTTP����˿�
	public int st_04_HttpPort;
	//!TCP�����˿�
	public int st_05_TCPPort;	
	//!SSL�����˿�
	public int st_06_SSLPort;
	//!UDP�����˿�
	public int st_07_UDPPort;
	//!���������
	public int st_08_MaxConn;
	//!����Э�� {"TCP","UDP","MCAST",��}
	public int st_09_MonMode;
	//!�޶�����ֵ
	public int st_10_MaxBps;
	//!�������
	//char TransferPlan[NET_NAME_PASSWORD_LEN];
	public int st_11_TransferPlan;

	//!�Ƿ����ø���¼�����ز���
	public boolean st_12_bUseHSDownLoad;
	//!MAC��ַ
	public byte st_14_sMac[] = new byte[SDKCONST.NET_MAX_MAC_LEN];
	public byte st_15_UserName[] = new byte[SDKCONST.NET_NAME_PASSWORD_LEN];
	public byte st_16_Password[] = new byte[SDKCONST.NET_NAME_PASSWORD_LEN];
	public byte st_17_LocalMac[] = new byte[SDKCONST.NET_MAX_MAC_LEN];
	public byte[] st_17_Zarg0 = new byte[3];
	public int st_18_nPasswordType;
}
