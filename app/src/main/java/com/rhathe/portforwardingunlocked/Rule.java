package com.rhathe.portforwardingunlocked;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.util.Log;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;


@Entity(tableName = "rule")
public class Rule implements Observable {

	@Ignore
	private PropertyChangeRegistry registry = new PropertyChangeRegistry();

	@Override
	public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		registry.add(callback);
	}

	@Override
	public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
		registry.remove(callback);
	}

	private enum Protocols {
		TCP, UDP, BOTH
	}

	// Column Fields

	@PrimaryKey(autoGenerate = true)
	private int uid;

	@ColumnInfo()
	private String name;

	@ColumnInfo(name = "is_tcp")
	private Boolean isTcp = true;

	@ColumnInfo(name = "is_udp")
	private Boolean isUdp = false;

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
	private Boolean isEnabled = true;

	@Ignore
	public List<String> fromInterfaces = Collections.emptyList();

	// Getters and Setters

	@Bindable
	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
		registry.notifyChange(this, BR.uid);
	}

	@Bindable
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		registry.notifyChange(this, BR.name);
	}

	public Boolean getIsTcp() { return Boolean.TRUE.equals(isTcp); }

	public void setIsTcp(Boolean isTcp) {
		this.isTcp = isTcp;
	}

	public Boolean getIsUdp() {
		return Boolean.TRUE.equals(isUdp);
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

	@Bindable
	public String getFromInterface() {
		return fromInterface;
	}

	public void setFromInterface(String fromInterface) {
		this.fromInterface = fromInterface;
		registry.notifyChange(this, BR.fromInterface);
	}

	@Bindable
	public int getPortRange() {
		return portRange;
	}

	public void setPortRange(int portRange) {
		this.portRange = portRange;
		registry.notifyChange(this, BR.portRange);
	}

	@Bindable
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
		//registry.notifyChange(this, BR.target);
	}

	@Bindable
	public int getTargetPort() {
		return targetPort;
	}

	public void setTargetPort(int targetPort) {
		this.targetPort = targetPort;
		registry.notifyChange(this, BR.targetPort);
	}

	@Bindable
	public Boolean getIsEnabled() { return Boolean.TRUE.equals(isEnabled); }

	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
		registry.notifyChange(this, BR.isEnabled);
	}

	@Bindable
	public String getProtocol() {
		if (getIsTcp() && getIsUdp()) return "BOTH";
		else if (getIsTcp()) return "TCP";
		else if (getIsUdp()) return "UDP";
		return "BOTH";
	}

	public void setProtocol(String protocol) {
		switch(protocol) {
			case "TCP":
				setIsTcp(true);
				setIsUdp(false);
				break;
			case "UDP":
				setIsTcp(false);
				setIsUdp(true);
				break;
			default:
				setIsTcp(true);
				setIsUdp(true);
				break;
		}
		registry.notifyChange(this, BR.protocol);
	}

	@Bindable
	public int getProtocolIdx() {
		return Protocols.valueOf(getProtocol()).ordinal();
	}

	public void setProtocolIdx(int protocolIdx) {
		String newProtocol = Protocols.values()[protocolIdx].toString();
		setProtocol(newProtocol);
		registry.notifyChange(this, BR.protocolIdx);
	}

	@Bindable
	public int getFromInterfaceIdx() {
		String fi = fromInterface == null ? "" : fromInterface;
		int idx = fromInterfaces.indexOf(fi);
		if (idx >= 0) return idx;
		return 0;
	}

	public void setFromInterfaceIdx(int fromInterfaceIdx) {
		String newFromInterface = fromInterfaces.get(fromInterfaceIdx);
		setFromInterface(newFromInterface);
		registry.notifyChange(this, BR.fromInterfaceIdx);
	}

	@Bindable
	public int getFromEndPort() {
		return getFromPort() + getPortRange();
	}

	public void setFromEndPort(int fromEndPort) {
		setPortRange(fromEndPort - getFromPort());
		registry.notifyChange(this, BR.fromEndPort);
	}

	// Helper Functions

	public void checkIsValid() throws Exception {
		if (name == null || name.isEmpty()) throw new EmptyNameException();
		if (portRange < 0) throw new NegativeRangeException();
	}

	public String displayInfo() {
		return String.format(
			"[%s] | %s: [%s, %s] -> %s: [%s, %s]",
			getProtocol(),
			getFromInterface(),
			getFromPort(),
			getFromEndPort(),
			getTarget(),
			getTargetPort(),
			getTargetPort() + getPortRange()
		);
	}

	public static class EmptyNameException extends Exception {}
	public static class NegativeRangeException extends Exception {}
}