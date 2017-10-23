/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.test.annotations.derivedidentities;
import java.io.Serializable;

/**
 * @author Hardy Ferentschik
 */
public class EmployerId implements Serializable {
	String name; // matches name of @Id attribute
	long employee; // matches name of @Id attribute and type of Employee PK

	public EmployerId() {
	}

	public EmployerId(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setEmployee(long employee) {
		this.employee = employee;
	}
}
