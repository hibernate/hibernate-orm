/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mixedmode;

import jakarta.persistence.Entity;


/**
 * @author Hardy Ferentschik
 */
@Entity
public class RentalCar extends Car {
	private RentalCompany company;

	private CURRENTLY_HIRED hired;

	private Character[] chassisNumber;

	private Insurance insurance;

	public RentalCompany getCompany() {
		return company;
	}

	public void setCompany(RentalCompany company) {
		this.company = company;
	}

	public Character[] getChassisNumber() {
		return chassisNumber;
	}

	public void setChassisNumber(Character[] chassisNumber) {
		this.chassisNumber = chassisNumber;
	}

	public CURRENTLY_HIRED getHired() {
		return hired;
	}

	public void setHired(CURRENTLY_HIRED hired) {
		this.hired = hired;
	}

	public Insurance getInsurance() {
		return insurance;
	}

	public void setInsurance(Insurance insurance) {
		this.insurance = insurance;
	}

	enum CURRENTLY_HIRED {
		YES, NO
	}
}
