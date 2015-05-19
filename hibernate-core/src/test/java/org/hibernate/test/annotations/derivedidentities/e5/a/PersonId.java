/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e5.a;
import java.io.Serializable;

/**
 * @author Emmanuel Bernard
 */
public class PersonId implements Serializable {
	String firstName;
	String lastName;

	public PersonId() {
	}

	public PersonId(String firstName, String lastName) {
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
