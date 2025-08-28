/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cid;
import java.io.Serializable;
import jakarta.persistence.Column;

/**
 * @author Emmanuel Bernard
 */

public class ParentPk implements Serializable {
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}


	private String firstName;
	@Column(name = "p_lname")
	private String lastName;

	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof ParentPk ) ) return false;

		final ParentPk parentPk = (ParentPk) o;

		if ( !firstName.equals( parentPk.firstName ) ) return false;
		if ( !lastName.equals( parentPk.lastName ) ) return false;

		return true;
	}

	public int hashCode() {
		int result;
		result = firstName.hashCode();
		result = 29 * result + lastName.hashCode();
		return result;
	}
}
