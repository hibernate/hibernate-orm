/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Lukasz Antoniak
 */
@Entity
public class Visitor implements Serializable {
	@Id
	@GeneratedValue
	private Long id;

	private String firstName;

	private String lastName;

	public Visitor() {
	}

	public Visitor(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( ! ( o instanceof Visitor) ) return false;

		Visitor visitor = (Visitor) o;

		if ( firstName != null ? !firstName.equals( visitor.firstName ) : visitor.firstName != null ) return false;
		if ( id != null ? !id.equals( visitor.id ) : visitor.id != null ) return false;
		if ( lastName != null ? !lastName.equals( visitor.lastName ) : visitor.lastName != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( firstName != null ? firstName.hashCode() : 0 );
		result = 31 * result + ( lastName != null ? lastName.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "Visitor(id = " + id + ", firstName = " + firstName + ", lastName = " + lastName + ")";
	}
}
