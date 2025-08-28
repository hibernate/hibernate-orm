/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;
import java.io.Serializable;
import jakarta.persistence.Embeddable;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class AddressEntryPk implements Serializable {
	private String firstname;
	private String lastname;

	public AddressEntryPk() {
	}

	public AddressEntryPk(String firstname, String lastname) {
		this.firstname = firstname;
		this.lastname = lastname;
	}

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof AddressEntryPk ) ) return false;

		final AddressEntryPk addressEntryPk = (AddressEntryPk) o;

		if ( !firstname.equals( addressEntryPk.firstname ) ) return false;
		if ( !lastname.equals( addressEntryPk.lastname ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = firstname.hashCode();
		result = 29 * result + lastname.hashCode();
		return result;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}
}
