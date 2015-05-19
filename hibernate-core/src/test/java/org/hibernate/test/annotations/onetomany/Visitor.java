/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetomany;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
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
