package org.hibernate.test.querycache;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class StringCompositeKey implements Serializable {
	
    private static final long serialVersionUID = 1L;

 private String substation;
    
    private String deviceType;
    
    private String device;

    private String analog;
    
    // For some dialects, the sum of a primary key column lengths cannot
    // be larger than 255 (DB2).  Restrict them to a sufficiently
    // small size.  See HHH-8085.
    
    @Column( length = 50 )
	public String getSubstation() {
		return substation;
	}

	public void setSubstation(String substation) {
		this.substation = substation;
	}

	@Column( length = 50 )
	public String getDeviceType() {
		return deviceType;
	}

	public void setDeviceType(String deviceType) {
		this.deviceType = deviceType;
	}

	@Column( length = 50 )
	public String getDevice() {
		return device;
	}

	public void setDevice(String device) {
		this.device = device;
	}

	@Column( length = 50 )
	public String getAnalog() {
		return analog;
	}

	public void setAnalog(String analog) {
		this.analog = analog;
	}
}
