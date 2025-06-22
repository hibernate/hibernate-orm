/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;
import java.math.BigDecimal;

/**
 * @author Gail Badner
 */
public class PartTimeEmployee extends Employee {
	private String title;
	private BigDecimal salary;
	private Employee manager;
	private int percent;

	public int getPercent() {
		return percent;
	}

	public void setPercent(int percent) {
		this.percent = percent;
	}
}
