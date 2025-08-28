/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id.sequences.entities;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
@SuppressWarnings("serial")
public class FootballerPk implements Serializable {
	private String firstname;
	private String lastname;

	@Column(name = "fb_fname")
	public String getFirstname() {
		return firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public FootballerPk() {
	}

	public FootballerPk(String firstname, String lastname) {
		this.firstname = firstname;
		this.lastname = lastname;

	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof FootballerPk ) ) return false;

		final FootballerPk footballerPk = (FootballerPk) o;

		if ( !firstname.equals( footballerPk.firstname ) ) return false;
		if ( !lastname.equals( footballerPk.lastname ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = firstname.hashCode();
		result = 29 * result + lastname.hashCode();
		return result;
	}

}
