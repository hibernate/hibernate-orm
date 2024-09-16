/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
