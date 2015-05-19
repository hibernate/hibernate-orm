/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

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
