/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany.orderby;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

@Entity(name = "Department")
@IdClass(DepartmentId.class)
public class Department {

	@Id
	@ManyToOne
	private ECompany company;


	@Id
	private String departmentCode;

	private String name;

	public String getName() {
		return name;
	}

	public void setCompany(ECompany company) {
		this.company = company;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setDepartmentCode(String departmentId) {
		this.departmentCode = departmentId;
	}
}
