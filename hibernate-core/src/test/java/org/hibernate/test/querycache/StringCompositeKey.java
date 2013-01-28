package org.hibernate.test.querycache;

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class StringCompositeKey implements Serializable {
	
    private static final long serialVersionUID = 1L;

	private String substation;
    
    private String deviceType;
    
    private String device;
    
    public String getSubstation() {
		return substation;
	}

	public void setSubstation(String substation) {
		this.substation = substation;
	}

	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	public String getAnalog() {
		return analog;
	}

	public void setAnalog(String analog) {
		this.analog = analog;
	}

	private String analog;
}