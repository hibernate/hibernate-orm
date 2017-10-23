/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;


import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
public class EmployeeGroup {
	@Id
	private EmployeeGroupId id;

	@OneToMany(cascade = CascadeType.ALL)
	@Fetch(FetchMode.SUBSELECT)
	private List<Employee> employees = new ArrayList<Employee>();

	public EmployeeGroup(EmployeeGroupId id) {
		this.id = id;
	}

	@SuppressWarnings("unused")
	private EmployeeGroup() {
	}

	public boolean addEmployee(Employee employee) {
		return employees.add(employee);
	}

	public List<Employee> getEmployees() {
		return employees;
	}

	public EmployeeGroupId getId() {
		return id;
	}

	@Override
	public String toString() {
		return id.toString();
	}
}
