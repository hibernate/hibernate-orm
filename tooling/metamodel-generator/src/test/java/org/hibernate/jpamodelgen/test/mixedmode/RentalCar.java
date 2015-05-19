/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.mixedmode;

import javax.persistence.Entity;


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
