/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.subselect;


import java.io.Serializable;
import javax.persistence.Embeddable;

@Embeddable
public class EmployeeGroupId
		implements Serializable {
	private static final long serialVersionUID = 1L;
	private String groupName;
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
