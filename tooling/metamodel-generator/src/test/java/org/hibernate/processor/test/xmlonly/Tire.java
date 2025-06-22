/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlonly;

public class Tire {
	public Long getId() {
		return 1L;
	}

	public void setId(Long id) {
	}

	public Car getCar() {
		return null;
	}

	public void setCar(Car car) {
	}
}
