/*
 * SPDX-License-Identifier: Apache-2.0
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
