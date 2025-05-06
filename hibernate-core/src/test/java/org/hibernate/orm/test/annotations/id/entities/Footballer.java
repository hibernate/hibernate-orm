/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.entities;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Emmanuel Bernard
 */
@Entity
@IdClass(FootballerPk.class)
@DiscriminatorColumn(name = "bibi")
public class Footballer {
	private String firstname;
	private String lastname;
	private String club;

	public Footballer() {
	}

	public Footballer(String firstname, String lastname, String club) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.club = club;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Footballer ) ) return false;

		final Footballer footballer = (Footballer) o;

		if ( !firstname.equals( footballer.firstname ) ) return false;
		if ( !lastname.equals( footballer.lastname ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = firstname.hashCode();
		result = 29 * result + lastname.hashCode();
		return result;
	}

	@Id
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	@Id
	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getClub() {
		return club;
	}

	public void setClub(String club) {
		this.club = club;
	}
}
