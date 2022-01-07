/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.converted.converter;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Employee {
	@Id
	private Integer id;

	@Convert(converter = AgeConverter.class)
	private String age;

	public Employee() {
	}

	public Employee(int id, String age) {
		this.id = id;
		this.age = age;
	}

	public int getId() {
		return id;
	}

	public String getAge() {
		return age;
	}
}
