/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id:$

package org.hibernate.test.annotations.derivedidentities;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

/**
 * @author Hardy Ferentschik
 */
@Entity
@IdClass(EmployerId.class)
public class Employer {
	@Id
	String name;

	@Id
	@ManyToOne
	Employee employee;

	public Employer() {
	}

	public Employer(String name) {
		this.name = name;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee emp) {
		this.employee = emp;
	}

	public String getName() {
		return name;
	}
}
