/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.ordering;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class Contact {
	@Basic
	@Column(name = "last_name")
	private String lastName;
	@Basic
	@Column(name = "first_name")
	private String firstName;
	@Enumerated(EnumType.STRING)
	private TypesOfThings stuff;
	public Contact(String lastName, String firstName) {
		this.lastName = lastName;
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public TypesOfThings getStuff() {
		return stuff;
	}

	public void setStuff(TypesOfThings stuff) {
		this.stuff = stuff;
	}
}
