/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

public class Person {
	private long id;

	private Gender gender;
	
	private HairColor hairColor;

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
