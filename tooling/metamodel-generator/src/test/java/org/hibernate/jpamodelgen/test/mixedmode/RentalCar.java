/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
