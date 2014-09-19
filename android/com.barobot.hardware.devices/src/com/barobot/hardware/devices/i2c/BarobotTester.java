package com.barobot.hardware.devices.i2c;

import com.barobot.common.IspSettings;
import com.barobot.parser.Queue;

public class BarobotTester extends I2C_Device_Imp {
	private int default_index	= 4;
	private long speed;

	public BarobotTester(){
		this.cpuname	= "m328p";
		this.row	= default_index;
		this.speed		= 16000000L;
		this.lfuse		= "0xFF";
		this.hfuse		= "0xDE";
		this.lock		= "";
		this.efuse		= "0x05";
	}
	public String getReset() {
		return "RESET"+ this.row;
	}
	public String getIsp() {
		return "P"+ this.row;
	}
	@Override
	public void isp(Queue q) {

	}
	public String getHexFile() {
		return IspSettings.mbBootloaderPath;
	}
	public int resetAndReadI2c(Queue q) {
		return 0;
	}
}
