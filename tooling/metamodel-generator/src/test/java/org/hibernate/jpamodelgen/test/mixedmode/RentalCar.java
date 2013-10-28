/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
