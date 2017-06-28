package com.rhathe.portforwardingunlocked;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.net.InetSocketAddress;


@Entity(tableName = "rule")
public class Rule {

	// Column Fields

	@PrimaryKey(autoGenerate = true)
	private int uid;

	@ColumnInfo()
	private String name;

	@ColumnInfo(name = "is_tcp")
	private Boolean isTcp;

	@ColumnInfo(name = "is_udp")
	private Boolean isUdp;

	@ColumnInfo(name = "from_port")
	private int fromPort;

	@ColumnInfo(name = "from_interface")
	private String fromInterface;

	@ColumnInfo(name = "port_range")
	private int portRange;

	@ColumnInfo()
	private String target;

	@ColumnInfo(name = "target_port")
	private int targetPort;

	@ColumnInfo(name = "is_enabled")
	private Boolean isEnabled;


	// Getters and Setters

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Boolean getIsTcp() {
		return isTcp;
	}

	public void setIsTcp(Boolean isTcp) {
		this.isTcp = isTcp;
	}

	public Boolean getIsUdp() {
		return isUdp;
	}

	public void setIsUdp(Boolean isUdp) {
		this.isUdp = isUdp;
	}

	public int getFromPort() {
		return fromPort;
	}

	public void setFromPort(int fromPort) {
		this.fromPort = fromPort;
	}

	public String getFromInterface() {
		return fromInterface;
	}

	public void setFromInterface(String fromInterface) {
		this.fromInterface = fromInterface;
	}

	public int getPortRange() {
		return portRange;
	}

	public void setPortRange(int portRange) {
		this.portRange = portRange;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public int getTargetPort() {
		return targetPort;
	}

	public void setTargetPort(int targetPort) {
		this.targetPort = targetPort;
	}

	public Boolean getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}