/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.entity;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.hibernate.annotations.Formula;

/**
 * @author Emmanuel Bernard
 */
@Entity()
@Table(name = "Formula_flight")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Flight implements Serializable {
	Long id;
	long maxAltitudeInMilimeter;
	long maxAltitude;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}

	public long getMaxAltitude() {
		return maxAltitude;
	}

	public void setMaxAltitude(long maxAltitude) {
		this.maxAltitude = maxAltitude;
	}

	@Formula("maxAltitude * 1000")
	public long getMaxAltitudeInMilimeter() {
		return maxAltitudeInMilimeter;
	}

	public void setMaxAltitudeInMilimeter(long maxAltitudeInMilimeter) {
		this.maxAltitudeInMilimeter = maxAltitudeInMilimeter;
	}
}
