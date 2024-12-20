/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.*;

public class Person {
	private int id;
	private String name;
	private Date birthDate;
	private double weightInKilograms;
	private double heightInMeters;
	private boolean isMarried;
	private Blob binaryData;
	private Clob comments;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setWeightInKilograms(double weightInKilograms) {
		this.weightInKilograms = weightInKilograms;
	}

	public void setIsMarried(boolean isMarried) {
		this.isMarried = isMarried;
	}

	public double getWeightInKilograms() {
		return weightInKilograms;
	}

	public void setHeightInMeters(double heightInMeters) {
		this.heightInMeters = heightInMeters;
	}

	public double getHeightInMeters() {
		return heightInMeters;
	}

	public boolean getIsMarried() {
		return isMarried;
	}

	public Blob getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(Blob binaryData) {
		this.binaryData = binaryData;
	}

	public Clob getComments() {
		return comments;
	}

	public void setComments(Clob comments) {
		this.comments = comments;
	}
}
