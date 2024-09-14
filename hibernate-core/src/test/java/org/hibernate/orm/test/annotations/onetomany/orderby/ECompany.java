/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.onetomany.orderby;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity(name = "Company")
public class ECompany {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
	@OrderBy("departmentCode DESC")
	private Set<Department> departments;

	public Set<Department> getDepartments() {
		return departments;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDepartments(Set<Department> departments) {
		this.departments = departments;
	}
}
