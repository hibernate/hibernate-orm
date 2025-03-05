/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;


import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EmployeeGroupId
		implements Serializable {
	private static final long serialVersionUID = 1L;
	@Column(name = "group_name")
	private String groupName;
	@Column(name = "dept_name")
	private String departmentName;

	@SuppressWarnings("unused")
	private EmployeeGroupId() {
	}

	public EmployeeGroupId(String groupName, String departmentName) {
		this.groupName = groupName;
		this.departmentName = departmentName;
	}

	public String getDepartmentName() {
		return departmentName;
	}

	public String getGroupName() {
		return groupName;
	}

	@Override
	public String toString() {
		return "groupName: " + groupName + ", departmentName: " + departmentName;
	}
}
