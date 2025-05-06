/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytomany;
import java.io.Serializable;
import jakarta.persistence.Entity;

/**
 * Employee in an Employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */
@Entity
@SuppressWarnings("serial")
public class Contractor extends Employee implements Serializable {

	private float hourlyRate;

	public float getHourlyRate() {
		return hourlyRate;
	}

	public void setHourlyRate(float hourlyRate) {
		this.hourlyRate = hourlyRate;
	}
}
