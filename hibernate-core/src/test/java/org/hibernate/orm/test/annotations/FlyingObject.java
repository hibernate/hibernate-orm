/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
public abstract class FlyingObject extends Thing implements Serializable {
	private String serial;
	private int altitude;
	private int metricAltitude;
	private String color = "white";


	public int getAltitude() {
		return altitude;
	}

	public void setAltitude(int i) {
		altitude = i;
	}

	@Transient
	public int getMetricAltitude() {
		return metricAltitude;
	}

	public void setMetricAltitude(int i) {
		metricAltitude = i;
	}

	@Column(name = "serialnbr")
	public String getSerial() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	@Column(nullable = false)
	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
