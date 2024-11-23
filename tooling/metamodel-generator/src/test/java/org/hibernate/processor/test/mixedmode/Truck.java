/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mixedmode;


/**
 * @author Hardy Ferentschik
 */
public class Truck extends Vehicle {
	private String make;

	public int getHorsePower() {
		return 0;
	}

	public void setHorsePower(int horsePower) {
	}

	public String getMake() {
		return make;
	}

	public void setMake(String make) {
		this.make = make;
	}
}
