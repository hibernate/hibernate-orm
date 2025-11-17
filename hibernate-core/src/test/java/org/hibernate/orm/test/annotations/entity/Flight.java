/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.entity;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

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
