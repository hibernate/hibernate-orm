/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Basic;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Person {

	@Id
	private Integer id;

	@Basic
	private String name;

	@Embedded
	@Convert(
			attributeName = "city",
			converter = ToDataBaseColumnThrowRuntimeExceptionConverter.class)
	private Address address;

	Person() {
	}

	public Person(Integer id, String name, Address address) {
		this.id = id;
		this.name = name;
		this.address = address;
	}

	public Integer getId() {
		return id;
	}

	public Address getAddress() {
		return address;
	}

}
