/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql.joinedSubclass;

import javax.persistence.Entity;

/**
 * @author Steve Ebersole
 */
@Entity
public class Employee extends Person {
	private String employeeNumber;

	public Employee() {
	}

	public Employee(String name) {
		super( name );
	}

	public Employee(String name, String employeeNumber) {
		super( name );
		this.employeeNumber = employeeNumber;
	}

	public String getEmployeeNumber() {
		return employeeNumber;
	}

	public void setEmployeeNumber(String employeeNumber) {
		this.employeeNumber = employeeNumber;
	}
}
