/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.column.transform.attributeoverride;

public class Measurement {
	private double valueInInches;

	public double getValueInInches() {
		return valueInInches;
	}

	public void setValueInInches(double valueInInches) {
		this.valueInInches = valueInInches;
	}
}
