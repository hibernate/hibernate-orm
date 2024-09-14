/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
