/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
public class Employee {
	@Id
	@GeneratedValue
	private Long id;
	private String name;

	public String getName() {
		return name;
	}

	@SuppressWarnings("unused")
	private Employee() {
	}

	public Employee(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
