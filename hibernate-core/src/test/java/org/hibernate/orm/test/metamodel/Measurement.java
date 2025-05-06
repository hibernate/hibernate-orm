/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel;

import jakarta.persistence.MappedSuperclass;

/**
 * @author Marco Belladelli
 */
@MappedSuperclass
public abstract class Measurement {
	private String unit;

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}
}
