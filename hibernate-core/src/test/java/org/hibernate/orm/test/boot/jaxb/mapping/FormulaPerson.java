/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import java.util.Date;

public class FormulaPerson {
	private String name;
	private Date dob;
	private int yob;
	private double heightInches;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public int getYob() {
		return yob;
	}

	public void setYob(int yob) {
		this.yob = yob;
	}

	public double getHeightInches() {
		return heightInches;
	}

	public void setHeightInches(double heightInches) {
		this.heightInches = heightInches;
	}
}
