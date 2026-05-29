/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Person {
	@Id
	@GeneratedValue
	private long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Gender gender;

	@Column(nullable = false)
	private HairColor hairColor;

	@Enumerated(EnumType.STRING)
	private HairColor originalHairColor;

	public static Person person(Gender gender, HairColor hairColor) {
		Person person = new Person();
		person.setGender( gender );
		person.setHairColor( hairColor );
		return person;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public HairColor getHairColor() {
		return hairColor;
	}

	public void setHairColor(HairColor hairColor) {
		this.hairColor = hairColor;
	}

	public HairColor getOriginalHairColor() {
		return originalHairColor;
	}

	public void setOriginalHairColor(HairColor originalHairColor) {
		this.originalHairColor = originalHairColor;
	}
}
