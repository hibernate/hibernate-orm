/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.embeddable;

import java.io.Serializable;
import javax.persistence.Embeddable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Embeddable
@Audited
public class Name implements Serializable {
	private String firstName;
	private String lastName;

	public Name() {
	}

	public Name(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Name) ) {
			return false;
		}

		Name name = (Name) o;
		if ( firstName != null ? !firstName.equals( name.firstName ) : name.firstName != null ) {
			return false;
		}
		if ( lastName != null ? !lastName.equals( name.lastName ) : name.lastName != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = firstName != null ? firstName.hashCode() : 0;
		result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Name(firstName = " + firstName + ", lastName = " + lastName + ")";
	}

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
}
