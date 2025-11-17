/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import java.sql.Date;
import java.sql.Blob;
import java.sql.Clob;

@Entity
public class Person {
	@Id
	@GeneratedValue
	@SequenceGenerator(sequenceName = "PERSON_SEQ")
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
