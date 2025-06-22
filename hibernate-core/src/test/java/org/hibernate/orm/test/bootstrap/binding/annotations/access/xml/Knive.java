/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access.xml;
import jakarta.persistence.Embeddable;

/**
 * @author Hardy Ferentschik
 */
@Embeddable
public class Knive {
	private String brand;

	private int bladeLength;

	public int getBladeLength() {
		return bladeLength;
	}

	public void setBladeLength(int bladeLength) {
		this.bladeLength = bladeLength;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}
}
